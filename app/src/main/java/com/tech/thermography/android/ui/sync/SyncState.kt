package com.tech.thermography.android.ui.sync

import com.tech.thermography.android.data.local.repository.SyncableRepository

/**
 * Representa o estado da tela de sincronização.
 */
data class SyncState(
    val tasks: List<SyncTask> = emptyList(),
    val overallProgress: Float = 0f,
    val isSyncFinished: Boolean = false
)

/**
 * Representa uma única tarefa de sincronização, associada a um repositório.
 */
data class SyncTask(
    val name: String,
    val repository: SyncableRepository, // Repositório responsável pela sincronização
    val status: SyncStatus = SyncStatus.PENDING
)

/**
 * Enum para os possíveis status de uma tarefa de sincronização.
 */
enum class SyncStatus {
    PENDING,    // Aguardando para iniciar
    IN_PROGRESS,// Em andamento
    COMPLETED,  // Concluída com sucesso
    FAILED      // Falhou
}
