package com.tech.thermography.android

import android.app.Application
import android.util.Log
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid
import com.flir.thermalsdk.log.ThermalLog
import dagger.hilt.android.HiltAndroidApp
import androidx.work.Configuration
import androidx.work.WorkManager
import com.tech.thermography.android.util.NetworkTransport
import com.tech.thermography.android.util.NetworkUtils
import com.tech.thermography.android.work.ThermographicSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

@HiltAndroidApp
class ThermographyApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

        ThermographicSyncScheduler.schedulePeriodic(this)
        applicationScope.launch {
            var previousTransport = NetworkTransport.DISCONNECTED
            NetworkUtils.observeNetworkTransport(this@ThermographyApp).collect { transport ->
                if (transport != NetworkTransport.DISCONNECTED && transport != previousTransport) {
                    runCatching {
                        ThermographicSyncScheduler.scheduleOnConnectivity(this@ThermographyApp)
                    }.onFailure { error ->
                        Log.e("ThermographyApp", "Não foi possível agendar sincronização por conectividade", error)
                    }
                }
                previousTransport = transport
            }
        }
    }
}
