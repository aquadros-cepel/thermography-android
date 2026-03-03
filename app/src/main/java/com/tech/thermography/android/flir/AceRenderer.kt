package com.tech.thermography.android.flir

import android.opengl.GLSurfaceView
import com.flir.thermalsdk.image.ColorDistributionSettings
import com.flir.thermalsdk.image.Palette
import com.flir.thermalsdk.log.ThermalLog
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class AceRenderer(
    private var controller: AceController
) : GLSurfaceView.Renderer {

    @Volatile private var palette: Palette? = null
    @Volatile private var colorSettings: ColorDistributionSettings? = null
    @Volatile private var settingsDirty: Boolean = true
    @Volatile private var service: FlirAceCameraService2? = null

    private var surfaceWidth = 0
    private var surfaceHeight = 0


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        controller?.onGlSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        controller?.onGlSurfaceSizeKnown(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        controller?.onGlDrawFrame()
    }

    fun attachService(service: FlirAceCameraService2) {
        this.service = service
    }

    fun setController(controller: AceController) {
        this.controller = controller
    }

    fun setPalette(palette: Palette) {
        this.palette = palette
        settingsDirty = true
    }

    fun setColorSettings(settings: ColorDistributionSettings) {
        this.colorSettings = settings
        settingsDirty = true
    }


}
