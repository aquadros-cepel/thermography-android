package com.tech.thermography.android

import android.app.Application
import android.util.Log
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid
import com.flir.thermalsdk.log.ThermalLog
import dagger.hilt.android.HiltAndroidApp
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import com.tech.thermography.android.work.ThermographicSyncWorker
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

@HiltAndroidApp
class ThermographyApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        ThermalSdkAndroid.init(
            this,
            ThermalLog.LogLevel.DEBUG
        )

        // Gatilho para sincronização inicial. 
        // Alterado para REPLACE para garantir que o worker seja disparado toda vez que o app abrir durante o desenvolvimento.
        val request = OneTimeWorkRequestBuilder<ThermographicSyncWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "ThermographicSync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
