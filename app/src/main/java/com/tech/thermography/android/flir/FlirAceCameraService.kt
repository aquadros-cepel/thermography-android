package com.tech.thermography.android.flir

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import com.flir.thermalsdk.ErrorCode
import com.flir.thermalsdk.androidsdk.helpers.PermissionHandler
import com.flir.thermalsdk.image.ColorDistributionSettings
import com.flir.thermalsdk.image.HistogramEqualizationSettings
import com.flir.thermalsdk.image.Palette
import com.flir.thermalsdk.image.PaletteManager
import com.flir.thermalsdk.live.*
import com.flir.thermalsdk.live.discovery.*
import com.flir.thermalsdk.live.streaming.Stream
import com.flir.thermalsdk.log.ThermalLog
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FlirAceCameraService(
    context: Context
) {

    companion object {
        private const val TAG = "FlirAceCameraService"
        private val ACE_INTERFACE = CommunicationInterface.ACE
    }

    // -------------------------
    // STATE (igual ao sample)
    // -------------------------

    private var glSurfaceView: GLSurfaceView? = null
    private var camera: Camera? = null
    private var activeStream: Stream? = null

    private var permissionHandler: PermissionHandler? =
        if (context is Activity) PermissionHandler(context) else null

    private var currentPalette: Palette =
        PaletteManager.getDefaultPalettes()[0]

    private var defaultColorSettings: ColorDistributionSettings =
        HistogramEqualizationSettings()

    // Surface / stream sync (EXATAMENTE como no Java)
    private val streamLock = Any()
    private var isConnecting = false
    private var isStreaming = false
    private var isPipelineSetup = false
    private var isSurfaceSizeKnown = false
    private var pendingStartStream = false

    private var delayedSetSurface = false
    private var delayedSurfaceWidth = 0
    private var delayedSurfaceHeight = 0

    // -------------------------
    // INIT GL
    // -------------------------

    fun configureGlSurfaceView(view: GLSurfaceView) {
        this.glSurfaceView = view
        view.apply {
            setEGLContextClientVersion(3)
            setPreserveEGLContextOnPause(false)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    // -------------------------
    // DISCOVERY
    // -------------------------

    fun startDiscoveryAndConnection() {
        synchronized(streamLock) {
            if (isConnecting || isStreaming) return
            isConnecting = true
        }

        DiscoveryFactory.getInstance().scan(
            object : DiscoveryEventListener {

                override fun onCameraFound(camera: DiscoveredCamera) {
                    val id = camera.identity
                    if (id.cameraType == CameraType.ACE &&
                        id.communicationInterface == ACE_INTERFACE
                    ) {
                        DiscoveryFactory.getInstance().stop(ACE_INTERFACE)
                        doConnect(id)
                    }
                }

                override fun onDiscoveryError(
                    communicationInterface: CommunicationInterface,
                    error: ErrorCode
                ) {
                    ThermalLog.e(TAG, "Discovery error: $error")
                    synchronized(streamLock) {
                        isConnecting = false
                    }
                }
            },
            ACE_INTERFACE
        )
    }

    // -------------------------
    // CONNECT
    // -------------------------

    private fun doConnect(identity: Identity) {
        val granted =
            permissionHandler?.requestCameraPermission(0x09) ?: true

        if (!granted) {
            synchronized(streamLock) {
                isConnecting = false
            }
            return
        }

        Thread {
            try {
                ThermalLog.d(TAG, "Connecting to $identity")

                if (camera == null) {
                    camera = Camera()
                }

                camera?.connect(
                    identity,
                    { ThermalLog.e(TAG, "Connection error: $it") },
                    ConnectParameters()
                )

                ThermalLog.d(TAG, "Camera connected")
                startStream()

            } catch (e: Exception) {
                ThermalLog.e(TAG, "Connection failed: ${e.message}")
            } finally {
                synchronized(streamLock) {
                    isConnecting = false
                }
            }
        }.start()
    }

    // -------------------------
    // STREAM (COPIADO DO SAMPLE)
    // -------------------------

    private fun startStream() {
        synchronized(streamLock) {

            val cam = camera ?: return

            if (isStreaming) return

            if (!isSurfaceSizeKnown) {
                pendingStartStream = true
                ThermalLog.w(TAG, "startStream deferred, surface unknown")
                return
            }

            activeStream = cam.streams[0]

            if (!isPipelineSetup) {
                ThermalLog.d(TAG, "glSetupPipeline")
                cam.glSetupPipeline(activeStream, false)
                isPipelineSetup = true
            }

            if (delayedSetSurface) {
                cam.glOnSurfaceChanged(
                    delayedSurfaceWidth,
                    delayedSurfaceHeight
                )
                delayedSetSurface = false
            }

            cam.customHistogramEqualizationSettings?.let {
                defaultColorSettings = it
            }

        activeStream?.start(
            { glSurfaceView?.requestRender() },
            { error -> ThermalLog.e(TAG, "Stream error: $error") }
        )

            isStreaming = true
            pendingStartStream = false
        }
    }

    // -------------------------
    // RENDERER (1:1)
    // -------------------------

    private val renderer = object : GLSurfaceView.Renderer {

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            ThermalLog.d(TAG, "onSurfaceCreated")
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            ThermalLog.d(TAG, "onSurfaceChanged $width x $height")

            var shouldStart = false

            if (camera != null) {
                camera?.glOnSurfaceChanged(width, height)
                delayedSetSurface = false
            } else {
                delayedSetSurface = true
                delayedSurfaceWidth = width
                delayedSurfaceHeight = height
            }

            synchronized(streamLock) {
                isSurfaceSizeKnown = true
                if (pendingStartStream && camera != null) {
                    shouldStart = true
                }
            }

            if (shouldStart) {
                startStream()
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            val cam = camera ?: return

            if (!cam.glIsGlContextReady()) return

            if (delayedSetSurface) {
                cam.glOnSurfaceChanged(
                    delayedSurfaceWidth,
                    delayedSurfaceHeight
                )
                delayedSetSurface = false
            }

            cam.glWithThermalImage {
                it.setPalette(currentPalette)
                it.setColorDistributionSettings(defaultColorSettings)
            }

            cam.glOnDrawFrame()
        }
    }

    // -------------------------
    // CLEANUP
    // -------------------------

    fun cleanup() {
        disconnect()
        glSurfaceView = null
    }

    fun disconnect() {
        Thread {
            if (camera == null) return@Thread

            activeStream?.stop()
            camera?.glTeardownPipeline()
            camera?.disconnect()

            camera = null
            activeStream = null

            synchronized(streamLock) {
                isStreaming = false
                isPipelineSetup = false
                pendingStartStream = false
                isConnecting = false
            }
        }.start()
    }
}
