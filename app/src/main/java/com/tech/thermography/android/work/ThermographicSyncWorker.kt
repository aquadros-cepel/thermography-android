package com.tech.thermography.android.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tech.thermography.android.data.local.repository.ThermographicInspectionRecordRepository
import com.tech.thermography.android.util.NetworkUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ThermographicSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ThermographicInspectionRecordRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("ThermographicSyncWorker", "Iniciando worker de sincronização")

        if (!NetworkUtils.hasInternetConnection(applicationContext)) {
            Log.d("ThermographicSyncWorker", "Sem conexão com a internet. Tentando novamente mais tarde.")
            return Result.retry()
        }

        return try {
            repository.syncPendingRecords()
            Log.d("ThermographicSyncWorker", "Sincronização concluída com sucesso")
            Result.success()
        } catch (e: Exception) {
            Log.e("ThermographicSyncWorker", "Erro durante a sincronização", e)
            Result.retry()
        }
    }
}
