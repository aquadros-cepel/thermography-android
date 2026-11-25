package com.tech.thermography.android.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal fun oneTimeWorkPolicy(work: Collection<Pair<WorkInfo.State, Int>>): ExistingWorkPolicy? = when {
    work.any { it.first == WorkInfo.State.RUNNING } ->
        if (work.any { it.first == WorkInfo.State.ENQUEUED || it.first == WorkInfo.State.BLOCKED }) null
        else ExistingWorkPolicy.APPEND_OR_REPLACE
    work.any { it.first == WorkInfo.State.ENQUEUED && it.second > 0 } -> ExistingWorkPolicy.REPLACE
    work.any { it.first == WorkInfo.State.ENQUEUED || it.first == WorkInfo.State.BLOCKED } -> null
    else -> ExistingWorkPolicy.KEEP
}

object ThermographicSyncScheduler {
    private const val TAG = "ThermographicScheduler"
    private const val PERIODIC_WORK_NAME = "ThermographicSyncPeriodic"
    private const val CHANGE_WORK_NAME = "ThermographicSyncOnChange"
    private const val CONNECTIVITY_WORK_NAME = "ThermographicSyncOnConnectivity"
    private val oneTimeSchedulingMutex = Mutex()

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ThermographicSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .build()

        Log.i(TAG, "Agendando trabalho periódico: name=$PERIODIC_WORK_NAME id=${request.id} policy=KEEP rede=CONNECTED")
        val operation = WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        operation.result.addListener({
            try {
                operation.result.get()
                Log.i(TAG, "Pedido periódico processado: name=$PERIODIC_WORK_NAME id=${request.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao agendar trabalho periódico: name=$PERIODIC_WORK_NAME", e)
            }
        }, { command -> command.run() })
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
            Log.i(TAG, "Solicitação: name=$workName trabalhos=${existingWork.map { "${it.id}:${it.state}:tentativas=${it.runAttemptCount}" }}")
            val policy = oneTimeWorkPolicy(existingWork.map { it.state to it.runAttemptCount })
            if (policy == null) {
                Log.i(TAG, "Solicitação agrupada: name=$workName já possui trabalho pendente")
                return
            }

            val request = OneTimeWorkRequestBuilder<ThermographicSyncWorker>()
                .setConstraints(networkConstraints())
                .build()

            Log.i(TAG, "Enfileirando: name=$workName id=${request.id} policy=$policy rede=CONNECTED")
            val operation = workManager.enqueueUniqueWork(workName, policy, request)
            suspendCancellableCoroutine<Unit> { continuation ->
                operation.result.addListener({
                    try {
                        operation.result.get()
                        Log.i(TAG, "Pedido processado: name=$workName id=${request.id}")
                        if (continuation.isActive) continuation.resume(Unit)
                    } catch (e: Exception) {
                        Log.e(TAG, "Falha ao agendar: name=$workName id=${request.id}", e)
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }, { command -> command.run() })
            }
        }
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}