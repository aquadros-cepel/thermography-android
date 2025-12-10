package com.tech.thermography.android.ui.sync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.repository.SyncableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncableRepositories: Map<Int, @JvmSuppressWildcards SyncableRepository>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncState())
    val uiState = _uiState.asStateFlow()

    init {
        // Cria as tarefas dinamicamente a partir do conjunto de repositórios injetados
        val tasks = syncableRepositories.values.map { repository ->
            val taskName = "Sincronizando ${repository.javaClass.simpleName.replace("Repository", "")}..."
            SyncTask(name = taskName, repository = repository)
        }
     
        _uiState.value = SyncState(tasks = tasks)
    }

    fun startSync() {
        viewModelScope.launch {
            val tasks = _uiState.value.tasks
            val totalTasks = tasks.size.toFloat()

            if (tasks.isEmpty()) {
                _uiState.update { it.copy(isSyncFinished = true) }
                return@launch
            }

            // Mutex para proteger o acesso à variável compartilhada e ao estado da UI
            val mutex = Mutex()
            var completedTasksCount = 0

            // Lança todas as syncs em paralelo, cada uma em Dispatchers.IO
            val jobs = tasks.mapIndexed {taskIndex,  task ->
                launch(Dispatchers.IO) {
                    updateTaskStatus(taskIndex, SyncStatus.IN_PROGRESS)

                    try {
                        task.repository.syncEntities()
                        updateTaskStatus(taskIndex, SyncStatus.COMPLETED)

                    } catch (e: Exception) {
                        Log.e("SyncViewModel", "Falha na sincronização de ${task.name}", e)
                        updateTaskStatus(taskIndex, SyncStatus.FAILED)

                    } finally {
                        // Seção crítica protegida pelo Mutex
                        mutex.withLock {
                            completedTasksCount++
                            _uiState.update {
                                it.copy(overallProgress = completedTasksCount / totalTasks)
                            }
                        }
                    }
                }
            }

            // Espera todas as tarefas terminarem de sincronizar dados da red
            jobs.joinAll()
            
            // Insere os dados em cache após todas as sincronizações serem concluídas
            syncableRepositories
                .toSortedMap()
                .values
                .forEach {
                     it.insertCached() 
                }
            
            _uiState.update { it.copy(isSyncFinished = true) }
        }
    }

    private fun updateTaskStatus(taskIndex: Int, status: SyncStatus) {
        _uiState.update { currentState ->
            val updatedTasks = currentState.tasks.toMutableList()
            updatedTasks[taskIndex] = updatedTasks[taskIndex].copy(status = status)
            currentState.copy(tasks = updatedTasks)
        }
    }
}
