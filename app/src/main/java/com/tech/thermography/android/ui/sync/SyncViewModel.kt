package com.tech.thermography.android.ui.sync

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.repository.PlantRepository
import com.tech.thermography.android.data.local.repository.SyncableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.MapTileIndex
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncableRepositories: Map<Int, @JvmSuppressWildcards SyncableRepository>,
    private val plantRepository: PlantRepository,
    private val application: Application,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncState())
    val uiState = _uiState.asStateFlow()

    // Repositório "fake" para encapsular a lógica de download dos tiles do mapa
    private val tileDownloaderRepository = object : SyncableRepository {
        override suspend fun syncEntities() = downloadOfflineTiles()
        override suspend fun insertCached() { /* Não aplicável */ }
    }

    init {
        // Cria as tarefas de dados em ordem de IntKey
        val dataTasks = syncableRepositories
            .toSortedMap()
            .values.map { repository ->
                val taskName = "Sincronizando ${repository.javaClass.simpleName.replace("Repository", "")}"
                SyncTask(name = taskName, repository = repository)
            }
        // Adiciona a tarefa de download de mapa
        //        val mapTask = SyncTask(name = "Baixando mapas para uso offline", repository = tileDownloaderRepository)
//        _uiState.value = SyncState(tasks = dataTasks + mapTask)
        _uiState.value = SyncState(tasks = dataTasks)
    }

    fun startSync() {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.clearAllTables()
            // 1. SINCRONIZA DADOS DA API (EM ORDEM)
            runDataSync()

            // 2. SINCRONIZA TILES DO MAPA (APÓS DADOS)
//            mapTask?.let { runMapSync(it) }
//            _uiState.update { it.copy(isSyncFinished = true) }
        }
    }
    
    private suspend fun runDataSync() {
        val tasks = _uiState.value.tasks
        val totalTasks = _uiState.value.tasks.size.toFloat()
        var completedTasks = 0

        for ((taskIndex, task) in tasks.withIndex()) {
            updateTaskStatus(taskIndex, SyncStatus.IN_PROGRESS)
            try {
                // Loga a ordem de inserção
                Log.d("Sync", "Inserindo task ${taskIndex}: ${task.name}")
                // Se for InspectionRecordGroup, loga a quantidade de registros InspectionRecord
                if (task.repository.javaClass.simpleName.contains("InspectionRecordGroupRepository")) {
                    val inspectionRecordCount = appDatabase.inspectionRecordDao().getAllInspectionRecordsCount()
                    Log.d("Sync", "Antes de inserir InspectionRecordGroup, há $inspectionRecordCount registros em InspectionRecord")
                }
                task.repository.syncEntities()
                task.repository.insertCached() // Persistência imediata
                updateTaskStatus(taskIndex, SyncStatus.COMPLETED)
            } catch (e: Exception) {
                Log.e("SyncViewModel", "Falha na sincronização de "+task.name, e)
                updateTaskStatus(taskIndex, SyncStatus.FAILED)
            } finally {
                completedTasks++
                _uiState.update { it.copy(overallProgress = completedTasks / totalTasks) }
            }
        }
    }
    
    private suspend fun runMapSync(task: SyncTask) {
        val taskIndex = _uiState.value.tasks.indexOf(task)
        updateTaskStatus(taskIndex, SyncStatus.IN_PROGRESS)

        try {
            task.repository.syncEntities()
            updateTaskStatus(taskIndex, SyncStatus.COMPLETED)
        } catch(e: Exception) {
            Log.e("SyncViewModel", "Falha no download dos tiles do mapa", e)
            updateTaskStatus(taskIndex, SyncStatus.FAILED)
        }
        
        _uiState.update { it.copy(overallProgress = 1f) } // Finaliza a barra de progresso
    }

    private suspend fun downloadOfflineTiles() {
        val plants = plantRepository.getAllPlants().first()
        if (plants.isEmpty()) return

        // Usa a mesma configuração de fonte (Z/Y/X) e nome do InspectionRecordsScreen para garantir Cache Hit
        val esriSource = object : OnlineTileSourceBase(
            "EsriImagery", 0, 19, 256, "",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return (baseUrl
                        + MapTileIndex.getZoom(pMapTileIndex)
                        + "/" + MapTileIndex.getY(pMapTileIndex)
                        + "/" + MapTileIndex.getX(pMapTileIndex))
            }
        }

        val tileWriter = SqlTileWriter()
        val cacheManager = CacheManager(esriSource, tileWriter, 15, 17) // Baixa do zoom 15 ao 17

        val boundingBoxes = plants.mapNotNull { plant ->
            if (plant.latitude != null && plant.longitude != null) {
                // Cria uma caixa de ~1km ao redor da planta
                BoundingBox(plant.latitude + 0.005, plant.longitude + 0.005, plant.latitude - 0.005, plant.longitude - 0.005)
            } else null
        }
        
        if (boundingBoxes.isEmpty()) return

        // Processa cada área sequencialmente para não sobrecarregar
        boundingBoxes.forEach { box ->
            try {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val callback = object : CacheManager.CacheManagerCallback {
                        override fun onTaskComplete() {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                        
                        override fun onTaskFailed(errors: Int) {
                            Log.e("SyncViewModel", "Erro ao baixar tiles: $errors erros")
                             // Não falha o processo todo por causa de um erro parcial, apenas segue
                             if (continuation.isActive) continuation.resume(Unit)
                        }

                        override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomLevelMax: Int, zoomLevelMin: Int) {}
                        override fun setPossibleTilesInArea(total: Int) {}
                        override fun downloadStarted() {} // Implementação necessária
                    }
                    
                    // Chama a função correta que aceita BoundingBox, ZoomMin, ZoomMax e Callback
                    cacheManager.downloadAreaAsync(application, box, 15, 17, callback)
                }
            } catch (e: Exception) {
                Log.e("SyncViewModel", "Exceção ao baixar área: ${e.message}")
            }
        }
    }

    private fun updateTaskStatus(taskIndex: Int, status: SyncStatus) {
        _uiState.update { currentState ->
            val updatedTasks = currentState.tasks.toMutableList()
            if (taskIndex < 0 || taskIndex >= updatedTasks.size) return@update currentState
            updatedTasks[taskIndex] = updatedTasks[taskIndex].copy(status = status)
            currentState.copy(tasks = updatedTasks)
        }
    }
}
