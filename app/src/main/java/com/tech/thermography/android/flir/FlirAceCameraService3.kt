package com.tech.thermography.android.flir

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.opengl.GLSurfaceView
import com.flir.thermalsdk.ErrorCode
import com.flir.thermalsdk.androidsdk.helpers.PermissionHandler
import com.flir.thermalsdk.image.ColorDistributionSettings
import com.flir.thermalsdk.image.HistogramEqualizationSettings
import com.flir.thermalsdk.image.Palette
import com.flir.thermalsdk.image.PaletteManager
import com.flir.thermalsdk.live.*
import com.flir.thermalsdk.live.discovery.*
import com.flir.thermalsdk.live.remote.OnCompletion
import com.flir.thermalsdk.live.remote.OnRemoteError
import com.flir.thermalsdk.live.remote.Property
import com.flir.thermalsdk.live.remote.StoredImage
import com.flir.thermalsdk.live.streaming.Stream
import com.flir.thermalsdk.log.ThermalLog
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FlirAceCameraService3(
    private val context: Context
) {
    companion object {
        private const val TAG = "FlirAceCameraService3"
        private val ACE_INTERFACE = CommunicationInterface.ACE
    }

    private var glSurfaceView: GLSurfaceView? = null
    private var camera: Camera? = null
    private var activeStream: Stream? = null

    private var permissionHandler: PermissionHandler? =
        if (context is Activity) PermissionHandler(context) else null

    private var currentPalette: Palette =
        PaletteManager.getDefaultPalettes()[0]

    private var defaultColorSettings: ColorDistributionSettings =
        HistogramEqualizationSettings()

    private val streamLock = Any()
    private var isConnecting = false
    private var isStreaming = false
    private var isPipelineSetup = false
    private var isSurfaceSizeKnown = false
    private var pendingStartStream = false

    private var delayedSetSurface = false
    private var delayedSurfaceWidth = 0
    private var delayedSurfaceHeight = 0

    private val cameraManager: CameraManager? = try {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    } catch (e: Exception) {
        null
    }

    fun configureGlSurfaceView(view: GLSurfaceView) {
        this.glSurfaceView = view
        view.apply {
            setEGLContextClientVersion(3)
            setPreserveEGLContextOnPause(false)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

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
                    synchronized(streamLock) { isConnecting = false }
                }
            },
            ACE_INTERFACE
        )
    }

    private fun doConnect(identity: Identity) {
        val granted = permissionHandler?.requestCameraPermission(0x09) ?: true
        if (!granted) {
            synchronized(streamLock) { isConnecting = false }
            return
        }

        Thread {
            try {
                if (camera == null) { camera = Camera() }
                camera?.connect(identity, { ThermalLog.e(TAG, "Connection error: $it") }, ConnectParameters())

                ThermalLog.d(TAG, "Camera connected")

                // Ativa a barra de temperatura por padrão
                setShowTemperatureBar(true)

                startStream()
            } catch (e: Exception) {
                ThermalLog.e(TAG, "Connection failed: ${e.message}")
            } finally {
                synchronized(streamLock) { isConnecting = false }
            }
        }.start()
    }

    private fun startStream() {
        synchronized(streamLock) {
            val cam = camera ?: return
            if (isStreaming) return
            if (!isSurfaceSizeKnown) {
                pendingStartStream = true
                return
            }
            activeStream = cam.streams[0]
            if (!isPipelineSetup) {
                cam.glSetupPipeline(activeStream, false)
                isPipelineSetup = true
            }
            if (delayedSetSurface) {
                cam.glOnSurfaceChanged(delayedSurfaceWidth, delayedSurfaceHeight)
                delayedSetSurface = false
            }
            cam.customHistogramEqualizationSettings?.let { defaultColorSettings = it }
            activeStream?.start(
                { glSurfaceView?.requestRender() },
                { error -> ThermalLog.e(TAG, "Stream error: $error") }
            )
            isStreaming = true
            pendingStartStream = false
        }
    }

    private val renderer = object : GLSurfaceView.Renderer {
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {}
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
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
                if (pendingStartStream && camera != null) shouldStart = true
            }
            if (shouldStart) startStream()
        }
        override fun onDrawFrame(gl: GL10?) {
            val cam = camera ?: return
            if (!cam.glIsGlContextReady()) return
            if (delayedSetSurface) {
                cam.glOnSurfaceChanged(delayedSurfaceWidth, delayedSurfaceHeight)
                delayedSetSurface = false
            }
            cam.glWithThermalImage {
                it.setPalette(currentPalette)
                it.setColorDistributionSettings(defaultColorSettings)
            }
            cam.glOnDrawFrame()
        }
    }

    fun cleanup() {
        disconnect()
        glSurfaceView = null
    }

    fun disconnect() {
        Thread {
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

    fun takeSnapshot(callback: (Boolean, String?, StoredImage?) -> Unit = { _, _, _ -> }) {
        val cam = camera ?: run { callback(false, "Camera not connected", null); return }
        try {
            val storage = cam.remoteControl?.storage ?: run { callback(false, "Storage not available", null); return }
            val cmd = storage.snapshot()

            val onCompletion = object : OnCompletion {
                override fun onCompletion() {
                    try {
                        val lastProp = cam.remoteControl?.storage?.lastStoredImage()
                        val lastValue = try { lastProp?.let { it.javaClass.getMethod("getValue").invoke(it) as? StoredImage } } catch (e: Exception) { null }
                        callback(true, null, lastValue)
                    } catch (e: Exception) {
                        callback(false, e.message, null)
                    }
                }
            }

            cmd.javaClass.getMethod("run", OnCompletion::class.java).invoke(cmd, onCompletion)

        } catch (e: Exception) {
            callback(false, e.message, null)
        }
    }

    fun setLaser(enabled: Boolean, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        val rc = camera?.remoteControl ?: run { callback(false, "Remote control not available"); return }
        try {
            val prop = rc.laserOn() ?: run { callback(false, "Laser not supported"); return }

            val onComp = object : OnCompletion {
                override fun onCompletion() {
                    ThermalLog.d(TAG, "Laser set to $enabled successfully")
                    callback(true, null)
                }
            }

            val onErr = object : OnRemoteError {
                override fun onRemoteError(error: ErrorCode) {
                    ThermalLog.e(TAG, "Laser set to $enabled failed: $error")
                    callback(false, error.toString())
                }
            }

            prop.set(enabled, onComp, onErr)
        } catch (e: Exception) {
            ThermalLog.e(TAG, "Exception in setLaser: ${e.message}")
            callback(false, e.message)
        }
    }

    fun setShowTemperatureBar(enabled: Boolean, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        val fc = camera?.remoteControl?.fireCameraControl ?: run { callback(false, "Remote control not available"); return }
        try {
            val prop = fc.showTemperatureBar() ?: run { callback(false, "Temperature bar not supported"); return }

            val onComp = object : OnCompletion {
                override fun onCompletion() {
                    ThermalLog.d(TAG, "Temperature bar set to $enabled successfully")
                    callback(true, null)
                }
            }

            val onErr = object : OnRemoteError {
                override fun onRemoteError(error: ErrorCode) {
                    ThermalLog.e(TAG, "Temperature bar set to $enabled failed: $error")
                    callback(false, error.toString())
                }
            }

            prop.set(enabled, onComp, onErr)
        } catch (e: Exception) {
            ThermalLog.e(TAG, "Exception in setShowTemperatureBar: ${e.message}")
            callback(false, e.message)
        }
    }

    fun setFlash(enabled: Boolean, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        val rc = camera?.remoteControl
        try {
            val flashProp = if (rc != null) try { rc.javaClass.getMethod("flashLight").invoke(rc) } catch (_: Throwable) { null } else null
            if (flashProp is Property<*>) {
                @Suppress("UNCHECKED_CAST")
                val p = flashProp as Property<Boolean>

                val onComp = object : OnCompletion {
                    override fun onCompletion() { callback(true, null) }
                }

                val onErr = object : OnRemoteError {
                    override fun onRemoteError(error: ErrorCode) { callback(false, error.toString()) }
                }

                p.set(enabled, onComp, onErr)
                return
            }
        } catch (_: Exception) {}

        val cm = cameraManager ?: run { callback(false, "CameraManager not available"); return }
        try {
            val idList = cm.cameraIdList
            var selectedId: String? = null
            for (id in idList) {
                val chars = cm.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    selectedId = id
                    break
                }
            }
            selectedId?.let { cm.setTorchMode(it, enabled); callback(true, null) } ?: callback(false, "No flash id")
        } catch (e: Exception) {
            callback(false, e.message)
        }
    }
}
