package com.tech.thermography.android.ui.camera

import android.opengl.GLSurfaceView
import androidx.lifecycle.ViewModel
import com.flir.thermalsdk.live.remote.StoredImage
import com.tech.thermography.android.flir.AceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThermogramCameraViewModel @Inject constructor(
    private val controller: AceController
) : ViewModel() {

    data class MeasurementSquareState(
        val label: String = "Bx1",
        val enabled: Boolean = false,
        val centerXFraction: Float = 0.5f,
        val centerYFraction: Float = 0.5f,
        val sizeFraction: Float = 0.3f,
        val initialSizeFraction: Float = 0.3f
    )

    fun attachGlSurface(glView: GLSurfaceView) {
        controller.attachSurface(glView)
    }

    fun onSurfaceSizeChanged(width: Int, height: Int) {
        controller.onSurfaceSizeChanged(width, height)
    }

    fun start() {
        controller.startCamera()
    }

    fun stop() {
        controller.disconnect()
    }

    override fun onCleared() {
        controller.disconnect()
    }

    /**
     * Requests the camera to store a snapshot. The callback returns (success, message, storedImage?)
     */
    fun takeSnapshot(callback: (Boolean, String?, StoredImage?) -> Unit = { _, _, _ -> }) {
        controller.takeSnapshot { success, msg, storedImage ->
            callback(success, msg, storedImage)
        }
    }

    fun toggleLaser(callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        controller.toggleLaser(callback)
    }

    fun toggleFlash(callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        controller.toggleLamp(callback)
    }

    fun getTemperatureRange(): AceController.TemperatureRange? {
        return controller.getTemperatureRange()
    }

    fun setMeasurementSquareState(state: MeasurementSquareState) {
        setMeasurementSquareStates(listOf(state))
    }

    fun setMeasurementSquareStates(states: List<MeasurementSquareState>) {
        controller.setMeasurementSquareStates(
            states.map {
                AceController.MeasurementSquareState(
                    label = it.label,
                    enabled = it.enabled,
                    centerXFraction = it.centerXFraction,
                    centerYFraction = it.centerYFraction,
                    sizeFraction = it.sizeFraction,
                    initialSizeFraction = it.initialSizeFraction
                )
            }
        )
    }
}
