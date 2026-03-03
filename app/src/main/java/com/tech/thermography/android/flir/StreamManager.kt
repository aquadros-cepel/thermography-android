package com.tech.thermography.android.flir

import android.opengl.GLSurfaceView
import com.flir.thermalsdk.image.ColorDistributionSettings
import com.flir.thermalsdk.image.Palette
import com.flir.thermalsdk.live.Camera
import com.flir.thermalsdk.live.streaming.Stream
import com.flir.thermalsdk.log.ThermalLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StreamManager(
    private val onError: (String) -> Unit,
    private val onFrame: () -> Unit
) {
    @Volatile private var stream: Stream? = null
    @Volatile private var camera: Camera? = null
    @Volatile private var glSurfaceView: GLSurfaceView? = null
    @Volatile private var palette: Palette? = null
    @Volatile private var colorSettings: ColorDistributionSettings? = null
    @Volatile private var isStreaming = false
    private val mutex = Mutex()

    suspend fun startStream(
        camera: Camera,
        stream: Stream,
        glSurfaceView: GLSurfaceView?,
        palette: Palette,
        colorSettings: ColorDistributionSettings
    ) {
        mutex.withLock {
            stopStream()
            this.camera = camera
            this.stream = stream
            this.glSurfaceView = glSurfaceView
            this.palette = palette
            this.colorSettings = colorSettings
            stream.start({
                glSurfaceView?.requestRender()
                onFrame()
            }, { error ->
                onError("Stream error: $error")
            })
            isStreaming = true
        }
    }

    suspend fun stopStream() {
        mutex.withLock {
            stream?.stop()
            isStreaming = false
        }
    }

    fun setPalette(palette: Palette) {
        this.palette = palette
    }

    fun setAutoRange(enabled: Boolean) {
        // Implement auto range logic if needed
    }

    suspend fun restartStream() {
        mutex.withLock {
            val cam = camera
            val str = stream
            val view = glSurfaceView
            val pal = palette
            val col = colorSettings
            if (cam != null && str != null && view != null && pal != null && col != null) {
                stopStream()
                startStream(cam, str, view, pal, col)
            }
        }
    }
}
