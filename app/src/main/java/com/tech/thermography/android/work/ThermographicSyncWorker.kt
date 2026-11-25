package com.tech.thermography.android.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tech.thermography.android.data.local.repository.ThermographicRecordRepository
import com.tech.thermography.android.util.NetworkUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class ThermographicSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ThermographicRecordRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.i("ThermographicSyncWorker", "Iniciando worker: id=$id tentativa=${runAttemptCount + 1}")

        if (!NetworkUtils.hasInternetConnection(applicationContext)) {
            Log.d("ThermographicSyncWorker", "Sem conexão com a internet. Tentando novamente mais tarde.")
            return Result.retry()
        }

        return try {
            Log.i("ThermographicSyncWorker", "Aguardando acesso exclusivo: id=$id")
            val synced = repository.withSyncLock {
                Log.i("ThermographicSyncWorker", "Consultando e enviando pendências: id=$id")
                repository.syncPendingRecords()
            }
            if (!synced) {
                Log.w("ThermographicSyncWorker", "Há falhas de upload; solicitando retry: id=$id tentativa=${runAttemptCount + 1}")
                return Result.retry()
            }
            Log.i("ThermographicSyncWorker", "Sincronização concluída com sucesso: id=$id")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ThermographicSyncWorker", "Erro durante a sincronização", e)
            Result.retry()
        }
    }
}
