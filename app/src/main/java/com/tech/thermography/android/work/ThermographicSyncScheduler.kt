package com.tech.thermography.android.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

object ThermographicSyncScheduler {
    private const val PERIODIC_WORK_NAME = "ThermographicSyncPeriodic"
    private const val CHANGE_WORK_NAME = "ThermographicSyncOnChange"
    private const val CONNECTIVITY_WORK_NAME = "ThermographicSyncOnConnectivity"
    private val oneTimeSchedulingMutex = Mutex()

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ThermographicSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    suspend fun scheduleOnLocalChange(context: Context) {
        enqueueUnique(context, CHANGE_WORK_NAME)
    }

    suspend fun scheduleOnConnectivity(context: Context) {
        enqueueUnique(context, CONNECTIVITY_WORK_NAME)
    }

    private suspend fun enqueueUnique(context: Context, workName: String) {
        oneTimeSchedulingMutex.withLock {
            val workManager = WorkManager.getInstance(context)
            val existingWork = workManager.getWorkInfosForUniqueWorkFlow(workName).first()
            if (existingWork.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }) {
                return
            }

            val policy = if (existingWork.any { it.state == WorkInfo.State.RUNNING }) {
                ExistingWorkPolicy.APPEND_OR_REPLACE
            } else {
                ExistingWorkPolicy.KEEP
            }
            val request = OneTimeWorkRequestBuilder<ThermographicSyncWorker>()
                .setConstraints(networkConstraints())
                .build()

            workManager.enqueueUniqueWork(workName, policy, request)
        }
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}