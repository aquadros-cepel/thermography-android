package com.tech.thermography.android.flir

import android.content.Context
import android.opengl.GLSurfaceView
import com.flir.thermalsdk.ErrorCode
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid
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
import com.flir.thermalsdk.live.remote.OnCompletion
import com.flir.thermalsdk.live.remote.OnRemoteError
import com.flir.thermalsdk.live.remote.StoredImage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class AceController @Inject constructor(
    @ApplicationContext private val context: Context
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
            applyMeasurementSquares(thermalImage)
        }

        cam.glOnDrawFrame()
    }

    fun setMeasurementSquareState(state: MeasurementSquareState) {
        setMeasurementSquareStates(listOf(state))
    }

    fun setMeasurementSquareStates(states: List<MeasurementSquareState>) {
        measurementSquareStates = states
        ThermalLog.d(TAG, "Measurement square states updated: $states")
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
                        val lastValue = try { lastProp?.let { it.javaClass.getMethod("getValue").invoke(it) as? StoredImage } } catch (_: Exception) { null }
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

    private fun applyMeasurementSquares(thermalImage: Any) {
        val measurements = invokeAnyGetter(thermalImage, listOf("getMeasurements", "measurements")) ?: return

        clearRectangles(measurements)

        measurementSquareStates
            .filter { it.enabled }
            .forEach { state ->
                applyMeasurementSquare(measurements, thermalImage, state)
            }
    }

    private fun applyMeasurementSquare(measurements: Any, thermalImage: Any, state: MeasurementSquareState) {

        val imageWidth = readInt(thermalImage, listOf("getWidth", "width")) ?: return
        val imageHeight = readInt(thermalImage, listOf("getHeight", "height")) ?: return

        val baseSize = minOf(imageWidth, imageHeight)
        val squareSize = (baseSize * state.sizeFraction).roundToInt().coerceAtLeast(24)
        val halfSize = squareSize / 2

        val centerX = (imageWidth * state.centerXFraction).roundToInt()
        val centerY = (imageHeight * state.centerYFraction).roundToInt()
        val left = (centerX - halfSize).coerceIn(0, (imageWidth - squareSize).coerceAtLeast(0))
        val top = (centerY - halfSize).coerceIn(0, (imageHeight - squareSize).coerceAtLeast(0))

        val added = invokeAnyMethod(
            measurements,
            listOf("addRectangle", "addRect"),
            arrayOf(left, top, squareSize, squareSize)
        )

        if (added == null) {
            ThermalLog.w(TAG, "Unable to add rectangle measurement to current frame (${state.label})")
        } else {
            ThermalLog.d(TAG, "Measurement rectangle applied (${state.label}) at [$left,$top] size=$squareSize")
        }
    }

    private fun clearRectangles(measurements: Any) {
        val directClear = invokeAnyMethod(
            measurements,
            listOf("clearRectangles", "removeAllRectangles", "resetRectangles", "clear"),
            emptyArray()
        )
        if (directClear != null) return

        val rectangles = invokeAnyGetter(measurements, listOf("getRectangles", "rectangles"))
        if (rectangles is MutableCollection<*>) {
            try {
                @Suppress("UNCHECKED_CAST")
                (rectangles as MutableCollection<Any?>).clear()
            } catch (_: Exception) {
                // ignore and let the next frame try again
            }
        }
    }

    private fun readInt(obj: Any, methodNames: List<String>): Int? {
        for (name in methodNames) {
            val m = obj.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
            if (m != null) {
                val value = try { m.invoke(obj) } catch (_: Exception) { null }
                when (value) {
                    is Int -> return value
                    is Number -> return value.toInt()
                }
            }
        }

        for (field in obj.javaClass.declaredFields) {
            try {
                field.isAccessible = true
                val v = field.get(obj)
                when (v) {
                    is Int -> return v
                    is Number -> return v.toInt()
                }
            } catch (_: Exception) {
                // ignore
            }
        }

        return null
    }

    private fun invokeAnyGetter(obj: Any, methodNames: List<String>): Any? {
        for (name in methodNames) {
            val m = obj.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
            if (m != null) {
                return try { m.invoke(obj) } catch (_: Exception) { null }
            }
        }
        return null
    }

    private fun invokeAnyMethod(target: Any, methodNames: List<String>, args: Array<Any?>): Any? {
        for (name in methodNames) {
            val method = target.javaClass.methods.firstOrNull {
                it.name == name && it.parameterCount == args.size
            }
            if (method != null) {
                return try { method.invoke(target, *args) } catch (_: Exception) { null }
            }
        }
        return null
    }

}
