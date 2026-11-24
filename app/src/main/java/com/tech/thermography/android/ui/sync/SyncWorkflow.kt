package com.tech.thermography.android.ui.sync

internal suspend fun runSyncWhenConnected(
    isConnected: Boolean,
    synchronize: suspend () -> Boolean
): Boolean = isConnected && synchronize()

internal suspend fun runUploadBeforeDownload(
    uploadPendingRecords: suspend () -> Boolean,
    clearLocalDatabase: suspend () -> Unit,
    downloadCloudData: suspend () -> Unit
): Boolean {
    if (!uploadPendingRecords()) return false

    clearLocalDatabase()
    downloadCloudData()
    return true
}