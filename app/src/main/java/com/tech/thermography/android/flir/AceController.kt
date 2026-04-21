package com.tech.thermography.android.flir

import android.content.Context
import android.os.Environment
import android.opengl.GLSurfaceView
import com.flir.thermalsdk.ErrorCode
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid
import com.flir.thermalsdk.image.TemperatureUnit
import com.flir.thermalsdk.image.ThermalValue
import com.flir.thermalsdk.live.Camera
import com.flir.thermalsdk.live.Identity
import com.flir.thermalsdk.live.CameraType
import com.flir.thermalsdk.live.CommunicationInterface
import com.flir.thermalsdk.live.ConnectParameters
import com.flir.thermalsdk.live.discovery.DiscoveredCamera
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener
import com.flir.thermalsdk.live.discovery.DiscoveryFactory
import com.flir.thermalsdk.log.ThermalLog
import com.flir.thermalsdk.live.streaming.Stream
import com.flir.thermalsdk.image.ColorDistributionSettings
import com.flir.thermalsdk.image.HistogramEqualizationSettings
import com.flir.thermalsdk.image.Palette
import com.flir.thermalsdk.image.PaletteManager
import com.flir.thermalsdk.image.fusion.FusionMode
import com.flir.thermalsdk.utils.Pair
import com.flir.thermalsdk.live.remote.OnCompletion
import com.flir.thermalsdk.live.remote.OnRemoteError
import com.flir.thermalsdk.live.remote.StoredImage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val snapshotManager: SnapshotManager
) {

    data class TemperatureRange(val min: Double, val max: Double)

    companion object {
        private const val TAG = "AceController"
        private val ACE_INTERFACE = CommunicationInterface.ACE
    }

    private enum class State {
        Idle,
        SurfaceReady,
        CameraReady,
        Streaming
    }

    private var state = State.Idle

    private var glView: GLSurfaceView? = null
    private var renderer: AceRenderer? = null

    private var camera: Camera? = null
    private var activeStream: Stream? = null

    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var appliedSurfaceWidth = 0
    private var appliedSurfaceHeight = 0

    // true when onSurfaceChanged fired before the camera was connected
    private var delayedSetSurface = false

    // Palette and color settings — initialized after SDK init
    private var currentPalette: Palette? = null
    private var colorSettings: ColorDistributionSettings = HistogramEqualizationSettings()

    private val flirCameraService = FlirCameraService()

    // Toggle states
    private var isLaserOn = false
    private var isFlashOn = false
    private var snapshotRequested = false
    private var pendingSnapshotPath: String? = null
    private var pendingSnapshotCallback: ((Boolean, String?, StoredImage?) -> Unit)? = null
    
    // Overlay snapshot support
    private var pendingOverlaySnapshot: OverlaySnapshotRequest? = null
    
    private data class OverlaySnapshotRequest(
        val activity: android.app.Activity,
        val file: File,
        val callback: (Boolean, String?, StoredImage?) -> Unit
    )

    data class MeasurementSquareState(
        val label: String = "Bx1",
        val enabled: Boolean = false,
        val centerXFraction: Float = 0.5f,
        val centerYFraction: Float = 0.5f,
        val sizeFraction: Float = 0.18f,
        val initialSizeFraction: Float = 0.18f
    )

    private var measurementSquareStates: List<MeasurementSquareState> = emptyList()

    // ---------- PUBLIC ----------

    fun attachSurface(view: GLSurfaceView) {
        glView = view

        // Se já temos um renderer, usamos ele; caso contrário, criamos um novo
        val r = renderer ?: AceRenderer(this).also { renderer = it }

        view.setEGLContextClientVersion(3)
        view.setRenderer(r)
        view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    fun startCamera() {
        ThermalSdkAndroid.init(context, ThermalLog.LogLevel.DEBUG)
        // Palettes are available only after SDK init
        currentPalette = PaletteManager.getDefaultPalettes()
            .firstOrNull { it.name.equals("Iron", ignoreCase = true) }
            ?: PaletteManager.getDefaultPalettes().firstOrNull()
        ThermalLog.d(TAG, "Using palette: ${currentPalette?.name}")
        discoveryAndConnectCamera()
        // startStream() is called inside doConnect() after the camera is fully connected
    }

    fun discoveryAndConnectCamera(){
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
                }
            },
            ACE_INTERFACE
        )
    }

    private fun doConnect(identity: Identity) {
        // A verificação de permissão deve ser feita na UI (Activity/Compose) antes de chamar startCamera()
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

            val cameraInfo = camera?.remoteControl?.cameraInformation()?.sync
            ThermalLog.d(TAG, "Camera connected: " + cameraInfo)


            state = State.CameraReady
            ThermalLog.d(TAG, "State: $state")
            startStream()
            // Enable temperature bar by default (matches FlirAceCameraService3 behaviour)
            setShowTemperatureBar(true) { success, error ->
                if (success) {
                    println("OK")
                } else {
                    println("Erro: $error")
                }
            }

        } catch (e: Exception) {
            ThermalLog.e(TAG, "Connection failed: ${e.message}")
        }
    }

    private fun validateAndRecoverState(): Boolean {
        if (state == State.CameraReady || state == State.Streaming) return true

        ThermalLog.w(TAG, "State is not CameraReady. Attempting recovery via re-discovery. Current state: $state")

        return try {
            camera = null
            discoveryAndConnectCamera()
            // Discovery is async; stream will be started from doConnect() when camera is found
            false
        } catch (e: Exception) {
            ThermalLog.e(TAG, "State recovery failed: ${e.message}")
            false
        }
    }

    fun startStream() {
        if (state != State.CameraReady && state != State.Streaming) {
            ThermalLog.w(TAG, "startStream() called but state is $state — triggering re-discovery")
            validateAndRecoverState()
            return
        }

        if (renderer == null) {
            ThermalLog.e(TAG, "Cannot start stream: Renderer is null")
            return
        }

        glView?.queueEvent {
            val cam = camera
            if (cam == null) {
                ThermalLog.e(TAG, "Cannot start stream: Camera is null")
                return@queueEvent
            }

            val stream = cam.streams.firstOrNull()
            if (stream == null) {
                ThermalLog.e(TAG, "Cannot start stream: No streams available from camera")
                return@queueEvent
            }

            try {
                ThermalLog.d(TAG, "Setting up OpenGL pipeline")
                cam.glSetupPipeline(stream, false)
                ThermalLog.d(TAG, "glSetupPipeline(scaleToFit=false)")

                // Use camera-specific histogram equalization settings if available
                cam.customHistogramEqualizationSettings?.let {
                    colorSettings = it
                    ThermalLog.d(TAG, "Using custom camera HEQ settings")
                }
                // Do NOT call glOnSurfaceChanged with hardcoded values here.
                // It will be called with the real surface dimensions in onGlSurfaceSizeKnown()
                // or deferred to onGlDrawFrame() if the surface wasn't ready yet.

                ThermalLog.d(TAG, "Starting stream")
                activeStream = stream
                stream.start(
                    {
//                        ThermalLog.d(TAG, "Stream frame received, requesting render")
                        glView?.requestRender()
                    },
                    {
                        ThermalLog.e(TAG, "Stream error: $it")
                    }
                )

                state = State.Streaming
                ThermalLog.d(TAG, "State updated to: $state")
            } catch (e: Exception) {
                ThermalLog.e(TAG, "Exception during stream setup: ${e.message}")
            }
        }
    }

    fun stopStream() {
        ThermalLog.d(TAG, "stopStream()")
        val cam = camera
        if (cam == null) {
            ThermalLog.w(TAG, "stopStream() — camera was null")
            return
        }
        activeStream?.stop()
        activeStream = null
        cam.glTeardownPipeline()
        state = State.CameraReady
        ThermalLog.d(TAG, "Stream stopped. State: $state")
    }

    fun disconnect() {
        Thread {
            ThermalLog.d(TAG, "disconnect()")
            if (camera == null) {
                ThermalLog.w(TAG, "disconnect() — camera was null, resetting state")
                state = State.Idle
                return@Thread
            }
            stopStream()
            camera?.disconnect()
            camera = null
            state = State.Idle
            ThermalLog.d(TAG, "Disconnected. State: $state")
        }.start()
    }

    fun onGlSurfaceCreated() {
        state = State.SurfaceReady
        ThermalLog.d(TAG, "State: $state")
    }

    private fun applySurfaceSize(cam: Camera, reason: String) {
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return
        if (appliedSurfaceWidth == surfaceWidth && appliedSurfaceHeight == surfaceHeight) return
        cam.glOnSurfaceChanged(surfaceWidth, surfaceHeight)
        appliedSurfaceWidth = surfaceWidth
        appliedSurfaceHeight = surfaceHeight
        delayedSetSurface = false
        ThermalLog.d(TAG, "Camera glOnSurfaceChanged applied ($reason): ${surfaceWidth}x${surfaceHeight}")
    }

    fun onGlSurfaceSizeKnown(w: Int, h: Int) {
        surfaceWidth = w
        surfaceHeight = h
        ThermalLog.d(TAG, "Surface size known: ${surfaceWidth}x${surfaceHeight}")

        val cam = camera
        if (cam != null) {
            applySurfaceSize(cam, "renderer")
        } else {
            // Camera not yet connected — defer until onGlDrawFrame
            delayedSetSurface = true
            ThermalLog.d(TAG, "Camera not ready yet — delaying glOnSurfaceChanged")
        }
    }

    fun onSurfaceSizeChanged(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        ThermalLog.d(TAG, "Surface size changed (view): ${surfaceWidth}x${surfaceHeight}")

        val view = glView
        if (view == null) {
            delayedSetSurface = true
            return
        }

        view.queueEvent {
            val cam = camera
            if (cam != null) {
                applySurfaceSize(cam, "view")
            } else {
                delayedSetSurface = true
            }
        }
    }

    fun onGlDrawFrame() {
        val cam = camera ?: return

        if (!cam.glIsGlContextReady()) {
            ThermalLog.w(TAG, "Skip onDrawFrame — GL context not ready")
            return
        }

        // Apply delayed or updated surface size if needed
        if (surfaceWidth > 0 && surfaceHeight > 0) {
            applySurfaceSize(cam, "draw")
        }

        // Configure palette, fusion mode and color settings for each frame
        cam.glWithThermalImage { thermalImage ->

            val stats = thermalImage.statistics

            if (stats != null) {
                val minTv = stats.min
                val maxTv = stats.max

                val scale = thermalImage.scale

                if (scale != null) {
                    scale.setRange(minTv, maxTv)
                }

//                ThermalLog.d(TAG, "AUTO SCALE (stats): $min - $max")
            }
            currentPalette?.let { thermalImage.setPalette(it) }
            thermalImage.fusion?.setFusionMode(FusionMode.THERMAL_ONLY)
            thermalImage.setColorDistributionSettings(colorSettings)
            flirCameraService.updateRangeFromThermalImage(thermalImage)

            // Handle overlay snapshot first (requires full screen capture)
            val overlayRequest = pendingOverlaySnapshot
            if (overlayRequest != null) {
                pendingOverlaySnapshot = null
                
                try {
                    thermalImage.setTemperatureUnit(TemperatureUnit.CELSIUS)
                    
                    val range: Pair<ThermalValue, ThermalValue> = cam.glGetScaleRange()
                    ThermalLog.d(TAG, "glGetScaleRange when storing image: ${range.first} - ${range.second}")
                    
                    thermalImage.scale?.setRange(range.first, range.second)
                    flirCameraService.applyMeasurementSquares(thermalImage, measurementSquareStates)
                    
                    // Capture FULL SCREEN (GL + Compose UI) - blocks until complete
                    val overlay = snapshotManager.captureGLFramebufferAsOverlay(
                        overlayRequest.activity,
                        surfaceWidth,
                        surfaceHeight
                    )
                    
                    if (overlay != null) {
                        // Save with overlay in ONE file
                        thermalImage.saveAs(overlayRequest.file.absolutePath, overlay)
                        ThermalLog.i(TAG, "Snapshot with overlay saved: ${overlayRequest.file.absolutePath}")
                        overlayRequest.callback(true, overlayRequest.file.absolutePath, null)
                    } else {
                        // Fallback: save without overlay
                        thermalImage.saveAs(overlayRequest.file.absolutePath)
                        ThermalLog.w(TAG, "Snapshot saved without overlay (overlay creation failed)")
                        overlayRequest.callback(true, overlayRequest.file.absolutePath, null)
                    }
                    
                } catch (e: Exception) {
                    ThermalLog.e(TAG, "Unable to save snapshot with overlay: ${e.message}")
                    e.printStackTrace()
                    overlayRequest.callback(false, e.message, null)
                }
            }
            // Handle regular snapshot (no overlay)
            else if (snapshotRequested) {
                snapshotRequested = false

                val callback = pendingSnapshotCallback
                val snapshotPath = pendingSnapshotPath
                pendingSnapshotCallback = null
                pendingSnapshotPath = null

                try {
                    thermalImage.setTemperatureUnit(TemperatureUnit.CELSIUS)

                    val range: Pair<ThermalValue, ThermalValue> = cam.glGetScaleRange()
                    ThermalLog.d(TAG, "glGetScaleRange when storing image: ${range.first} - ${range.second}")

                    thermalImage.scale?.setRange(range.first, range.second)
                    flirCameraService.applyMeasurementSquares(thermalImage, measurementSquareStates)

                    val path = snapshotPath ?: throw IllegalStateException("Snapshot path not prepared")
                    thermalImage.saveAs(path)
                    ThermalLog.d(TAG, "Snapshot stored under: $path")
                    callback?.invoke(true, path, null)
                } catch (e: Exception) {
                    ThermalLog.e(TAG, "Unable to take snapshot: ${e.message}")
                    callback?.invoke(false, e.message, null)
                }
            }
        }

        cam.glOnDrawFrame()
    }

    fun setMeasurementSquareStates(states: List<MeasurementSquareState>) {
        measurementSquareStates = states
        ThermalLog.d(TAG, "Measurement square states updated: $states")
    }

    fun takeSnapshot(callback: (Boolean, String?, StoredImage?) -> Unit = { _, _, _ -> }) {
        if (camera == null || activeStream == null) {
            callback(false, "Camera not connected or stream not active", null)
            return
        }

        if (snapshotRequested) {
            callback(false, "Snapshot already requested", null)
            return
        }

        val snapshotFile = buildSnapshotFile() ?: run {
            callback(false, "Unable to prepare snapshot file", null)
            return
        }

        pendingSnapshotPath = snapshotFile.absolutePath
        pendingSnapshotCallback = callback
        snapshotRequested = true
        ThermalLog.d(TAG, "Snapshot requested: ${snapshotFile.absolutePath}")
        glView?.requestRender()
    }
    
    /**
     * Take a snapshot with screen overlay (saved as separate PNG file).
     * Creates 2 files:
     * - snapshot_XXX.jpg (thermal data)
     * - snapshot_XXX_overlay.png (screen capture with UI)
     * 
     * @param activity The activity to capture the screen from
     * @param callback Result callback (success, message, storedImage)
     */
    fun takeSnapshotWithOverlay(
        activity: android.app.Activity,
        callback: (Boolean, String?, StoredImage?) -> Unit = { _, _, _ -> }
    ) {
        if (camera == null || activeStream == null) {
            callback(false, "Camera not connected or stream not active", null)
            return
        }

        if (pendingOverlaySnapshot != null) {
            callback(false, "Overlay snapshot already requested", null)
            return
        }

        val snapshotFile = buildSnapshotFile() ?: run {
            callback(false, "Unable to prepare snapshot file", null)
            return
        }

        pendingOverlaySnapshot = OverlaySnapshotRequest(
            activity = activity,
            file = snapshotFile,
            callback = callback
        )
        
        ThermalLog.d(TAG, "Overlay snapshot requested: ${snapshotFile.absolutePath}")
        glView?.requestRender()
    }

    private fun buildSnapshotFile(): File? {
        return try {
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return null
            val destDir = File(picturesDir, "thermalEnergy")
            if (!destDir.exists()) destDir.mkdirs()
            File(destDir, "snapshot_${System.currentTimeMillis()}.jpg")
        } catch (e: Exception) {
            ThermalLog.e(TAG, "Failed to prepare snapshot file: ${e.message}")
            null
        }
    }

    fun toggleLamp(callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        val next = !isFlashOn

        try {
            camera?.toggleLamp(next)
            isFlashOn = next
            ThermalLog.d(TAG, "Flash toggled → $isFlashOn")
            callback(true, null)

        } catch (e: Exception) {
            ThermalLog.e(TAG, "Exception in toggleLamp: ${e.message}")
            callback(false, e.message)
        }
    }

    fun toggleLaser(callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        val next = !isLaserOn
        val rc = camera?.remoteControl ?: run { callback(false, "Remote control not available"); return }
        try {
            val prop = rc.laserOn() ?: run { callback(false, "Laser not supported"); return }
            prop.setSync(next)
            isLaserOn = next
            ThermalLog.d(TAG, "Laser toggled → $isLaserOn")
            callback(true, null)

        } catch (e: Exception) {
            ThermalLog.e(TAG, "Exception in toggleLaser: ${e.message}")
            callback(false, e.message)
        }
    }

    fun setShowTemperatureBar(enabled: Boolean, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        val tr = camera?.remoteControl?.temperatureRange
        ThermalLog.e(TAG, "Temperature range: $tr")

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

    fun getTemperatureRange(): TemperatureRange? {
        return flirCameraService.getLatestTemperatureRange() ?: getTemperatureRangeFromRemote()
    }

    private fun getTemperatureRangeFromRemote(): TemperatureRange? {
        val tr = camera?.remoteControl?.temperatureRange ?: return null
        val rangeObj = unwrapPropertyValue(tr) ?: tr
        val min = readNumber(rangeObj, listOf("getMin", "getLower", "getLow", "getMinTemperature", "getLowerTemperature", "getMinTemp"))
        val max = readNumber(rangeObj, listOf("getMax", "getUpper", "getHigh", "getMaxTemperature", "getUpperTemperature", "getMaxTemp"))
        return if (min != null && max != null) TemperatureRange(min, max) else null
    }

    private fun unwrapPropertyValue(obj: Any): Any? {
        val getter = obj.javaClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }
            ?: obj.javaClass.methods.firstOrNull { it.name == "get" && it.parameterCount == 0 }
            ?: obj.javaClass.methods.firstOrNull { it.name == "getSync" && it.parameterCount == 0 }
        return try {
            getter?.invoke(obj)
        } catch (_: Exception) {
            null
        }
    }

    private fun readNumber(obj: Any, methodNames: List<String>): Double? {
        for (name in methodNames) {
            val m = obj.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
            if (m != null) {
                val value = try { m.invoke(obj) } catch (_: Exception) { null }
                if (value is Number) return value.toDouble()
            }
        }
        // Fallback: try numeric fields
        for (field in obj.javaClass.declaredFields) {
            try {
                field.isAccessible = true
                val v = field.get(obj)
                if (v is Number) return v.toDouble()
            } catch (_: Exception) {
                // ignore
            }
        }
        return null
    }

}
