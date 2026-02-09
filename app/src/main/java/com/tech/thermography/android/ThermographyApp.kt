package com.tech.thermography.android

import android.app.Application
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid
import com.flir.thermalsdk.log.ThermalLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ThermographyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ISTO é o gatilho correto
        ThermalSdkAndroid.init(
            this,
            ThermalLog.LogLevel.DEBUG
        )
    }
}