package com.tech.thermography.android.ui.sync

data class SyncState(
    val tasks: List<SyncTask> = emptyList(),
    val overallProgress: Float = 0f,
    val isSyncFinished: Boolean = false
)

data class SyncTask(
    val name: String,
    val status: SyncStatus = SyncStatus.PENDING
)

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
