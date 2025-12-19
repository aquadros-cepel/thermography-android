package com.tech.thermography.android.ui.sync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.repository.PlantRepository
import com.tech.thermography.android.data.local.repository.SyncableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncState())
    val uiState = _uiState.asStateFlow()

    // Repositório "fake" para encapsular a lógica de download dos tiles do mapa
    private val tileDownloaderRepository = object : SyncableRepository {
        override suspend fun syncEntities() = downloadOfflineTiles()
        override suspend fun insertCached() { /* Não aplicável */ }
    }

    init {
        // Cria as tarefas de dados
        val dataTasks = syncableRepositories.values.map { repository ->
            val taskName = "Sincronizando ${repository.javaClass.simpleName.replace("Repository", "")}"
            SyncTask(name = taskName, repository = repository)
        }
        // Adiciona a tarefa de download de mapa
        val mapTask = SyncTask(name = "Baixando mapas para uso offline", repository = tileDownloaderRepository)
        
        _uiState.value = SyncState(tasks = dataTasks + mapTask)
    }

    fun startSync() {
        viewModelScope.launch {
            val allTasks = _uiState.value.tasks
            if (allTasks.isEmpty()) {
                _uiState.update { it.copy(isSyncFinished = true) }
                return@launch
            }
            
            val dataTasks = allTasks.filter { it.repository != tileDownloaderRepository }
            val mapTask = allTasks.find { it.repository == tileDownloaderRepository }

            // 1. SINCRONIZA DADOS DA API (EM PARALELO)
            runDataSync(dataTasks)

            // 2. SINCRONIZA TILES DO MAPA (APÓS DADOS)
            mapTask?.let { runMapSync(it) }

            _uiState.update { it.copy(isSyncFinished = true) }
        }
    }
    
    private suspend fun runDataSync(tasks: List<SyncTask>) {
        val totalTasks = _uiState.value.tasks.size.toFloat()
        val mutex = Mutex()
        var completedTasks = 0

        tasks.mapIndexed { taskIndex, task ->
            launch(Dispatchers.IO) {
                updateTaskStatus(taskIndex, SyncStatus.IN_PROGRESS)
                try {
                    task.repository.syncEntities()
                    updateTaskStatus(taskIndex, SyncStatus.COMPLETED)
                } catch (e: Exception) {
                    Log.e("SyncViewModel", "Falha na sincronização de ${task.name}", e)
                    updateTaskStatus(taskIndex, SyncStatus.FAILED)
                } finally {
                    mutex.withLock {
                        completedTasks++
                        _uiState.update { it.copy(overallProgress = completedTasks / totalTasks) }
                    }
                }
            }
        }.joinAll()

        // Insere os dados em cache após todas as sincronizações serem concluídas
        syncableRepositories.toSortedMap().values.forEach { it.insertCached() }
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
        val cacheManager = CacheManager(esriSource, tileWriter, 15, 18) // Baixa do zoom 15 ao 18

        val boundingBoxes = plants.mapNotNull { plant ->
            if (plant.latitude != null && plant.longitude != null) {
                // Cria uma caixa de ~1km ao redor da planta
                BoundingBox(plant.latitude + 0.005, plant.longitude + 0.005, plant.latitude - 0.005, plant.longitude - 0.005)
            } else null
        }

        // Envolve a lógica de callback em uma coroutine suspendável
        suspendCancellableCoroutine<Unit> { continuation ->
            val callback = object : CacheManager.CacheManagerCallback {
                override fun onTaskComplete() {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                override fun onGetTilesProgress(progress: Int, total: Int) { /* Opcional: atualizar progresso detalhado */ }
                override fun onTaskFailed(errors: Int) {
                     if (continuation.isActive) continuation.resumeWithException(Exception("Falha ao baixar $errors tiles"))
                }
            }
            cacheManager.downloadAreaAsync(null, boundingBoxes, callback) // Contexto nulo é aceitável aqui
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