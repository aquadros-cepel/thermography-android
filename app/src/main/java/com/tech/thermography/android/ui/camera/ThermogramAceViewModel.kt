package com.tech.thermography.android.ui.camera

import android.opengl.GLSurfaceView
import androidx.lifecycle.ViewModel
import com.tech.thermography.android.flir.FlirAceCameraService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThermogramAceViewModel @Inject constructor(
    private val aceService: FlirAceCameraService
) : ViewModel() {

    fun attachGlSurface(glView: GLSurfaceView) {
        aceService.configureGlSurfaceView(glView)
    }

    fun start() {
        aceService.startDiscoveryAndConnection()
    }

    override fun onCleared() {
        aceService.disconnect()
    }
}
