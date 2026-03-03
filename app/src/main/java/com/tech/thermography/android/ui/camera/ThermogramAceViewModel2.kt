package com.tech.thermography.android.ui.camera


import android.opengl.GLSurfaceView
import androidx.lifecycle.ViewModel
import com.tech.thermography.android.flir.CameraState
import com.tech.thermography.android.flir.FlirAceCameraService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class ThermogramAceViewModel2 @Inject constructor(
    private val aceService: FlirAceCameraService
) : ViewModel() {

//    val state: StateFlow<CameraState> = aceService.state
//
//    fun attachSurface(view: GLSurfaceView) {
//        aceService.configureGlSurfaceView(view)
//    }
//
//    fun start() {
//        aceService.startDiscoveryAndConnection()
//    }
//
//    fun stop() {
////        aceService.shutdown()
//    }
//
//    override fun onCleared() {
////        aceService.shutdown()
//    }
}
