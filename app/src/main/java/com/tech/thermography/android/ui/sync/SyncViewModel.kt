package com.tech.thermography.android.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncState())
    val uiState = _uiState.asStateFlow()

    init {
        // Inicializa as tarefas que serão executadas
        _uiState.value = SyncState(
            tasks = listOf(
                SyncTask(name = "Sincronizando Plantas...")
                // Adicione outras tarefas aqui se necessário
            )
        )
    }

    fun startSync() {
        viewModelScope.launch {
            val tasks = _uiState.value.tasks
            val totalTasks = tasks.size

            tasks.forEachIndexed { index, task ->
                // Atualiza o status da tarefa para "Em andamento"
                updateTaskStatus(index, SyncStatus.IN_PROGRESS)

                try {
                    // Simula a chamada de sincronização do repositório
                    plantRepository.syncPlants()
                    delay(1000) // Simula o tempo de espera da rede

                    // Atualiza o status da tarefa para "Concluída"
                    updateTaskStatus(index, SyncStatus.COMPLETED)
                } catch (e: Exception) {
                    // Em caso de falha, atualiza o status para "Falhou"
                    updateTaskStatus(index, SyncStatus.FAILED)
                }

                // Atualiza o progresso geral
                _uiState.update {
                    it.copy(overallProgress = (index + 1) / totalTasks.toFloat())
                }
            }

            // Marca a sincronização como finalizada
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
