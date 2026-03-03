package com.tech.thermography.android.ui.camera

import android.opengl.GLSurfaceView
import androidx.lifecycle.ViewModel
import com.flir.thermalsdk.live.remote.StoredImage
import com.tech.thermography.android.flir.AceController
import com.tech.thermography.android.flir.FlirAceCameraService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThermogramAceViewModel1 @Inject constructor(
    private val controller: AceController
) : ViewModel() {

    fun attachGlSurface(glView: GLSurfaceView) {
        controller.attachSurface(glView)
    }

    fun start() {
        controller.startCamera()
    }

    fun startStream() {
        controller.startStream()
    }

    override fun onCleared() {

    }

    fun stop() {

    }

    /**
     * Requests the camera to store a snapshot. The callback returns (success, message, storedImage?)
     */
    fun takeSnapshot(callback: (Boolean, String?, StoredImage?) -> Unit = { _, _, _ -> }) {
//        aceService.takeSnapshot { success, msg, storedImage ->
//            callback(success, msg, storedImage)
//        }
    }

    fun setLaser(enabled: Boolean, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
//        aceService.setLaser(enabled, callback)
    }

    fun setFlash(enabled: Boolean, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
//        aceService.setFlash(enabled, callback)
    }
}
