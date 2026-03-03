package com.tech.thermography.android.flir

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import com.flir.thermalsdk.androidsdk.helpers.PermissionHandler
import com.flir.thermalsdk.image.ColorDistributionSettings
import com.flir.thermalsdk.image.HistogramEqualizationSettings
import com.flir.thermalsdk.image.Palette
import com.flir.thermalsdk.image.PaletteManager
import com.flir.thermalsdk.live.*

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class FlirAceCameraService2(
    private val context: Context
) {
//
//    companion object {
//        private const val TAG = "FlirAceCameraService"
//        private val ACE_INTERFACE = CommunicationInterface.ACE
//        private const val WATCHDOG_TIMEOUT_MS = 4000L
//    }
//
//    // --- State Machine ---
//    private val _state = MutableStateFlow<CameraState>(CameraState.Idle)
//    val state: StateFlow<CameraState> = _state.asStateFlow()
//
//    // --- Coroutine Scope ---
//    private val job = SupervisorJob()
//    private val scope = CoroutineScope(Dispatchers.IO + job)
//
//    // --- Internal Managers ---
//    private val connectionManager = CameraConnectionManager(context, ::onCameraConnected, ::onCameraError)
//    private val streamManager = StreamManager(::onStreamError, ::onFrameReceived)
//    private val renderManager = AceRenderer()
//
//    // --- Permission ---
//    private val permissionHandler: PermissionHandler? = if (context is Activity) PermissionHandler(context) else null
//
//    // --- Shared State (Thread Safe) ---
//    @Volatile private var glSurfaceView: GLSurfaceView? = null
//    @Volatile private var lastFrameTimestamp: Long = 0L
//
//    // --- Palette/Settings ---
//    @Volatile private var currentPalette: Palette = PaletteManager.getDefaultPalettes()[0]
//    @Volatile private var defaultColorSettings: ColorDistributionSettings = HistogramEqualizationSettings()
//
//    // --- Camera accessor for renderer ---
//    fun getCamera(): Camera? = connectionManager.camera
//
//
//    // -------------------------
//    // INIT GL
//    // -------------------------
//
//    fun configureGlSurfaceView(view: GLSurfaceView) {
//        this.glSurfaceView = view
//        view.apply {
//            setEGLContextClientVersion(3)
//            setPreserveEGLContextOnPause(false)
//            setRenderer(renderManager)
//            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
//        }
//        renderManager.attachService(this)
//    }
//
//    // -------------------------
//    // DISCOVERY
//    // -------------------------
//
//
//    fun startDiscoveryAndConnection() {
//        if (state.value is CameraState.Connecting || state.value is CameraState.Streaming) return
//        _state.value = CameraState.Discovering
//        scope.launch {
//            try {
//                val identity = connectionManager.discoverAndConnect(ACE_INTERFACE)
//                _state.value = CameraState.Connecting
//            } catch (e: Exception) {
//                _state.value = CameraState.Error("Discovery failed: ${e.message}")
//            }
//        }
//    }
//
//    // -------------------------
//    // CONNECT
//    // -------------------------
//
//
//    private fun doConnect(identity: Identity) {
//        val granted = permissionHandler?.requestCameraPermission(0x09) ?: true
//        if (!granted) {
//            _state.value = CameraState.Error("Camera permission denied")
//            return
//        }
//        scope.launch {
//            try {
//                connectionManager.connect(identity)
//                _state.value = CameraState.Connected
//                startStream()
//            } catch (e: Exception) {
//                _state.value = CameraState.Error("Connection failed: ${e.message}")
//            }
//        }
//    }
//
//    // -------------------------
//    // STREAM (COPIADO DO SAMPLE)
//    // -------------------------
//
//
//    private fun startStream() {
//        scope.launch {
//            val cam = connectionManager.camera ?: run {
//                _state.value = CameraState.Error("Camera not connected")
//                return@launch
//            }
//            val stream = cam.streams.firstOrNull()
//            if (stream == null) {
//                _state.value = CameraState.Error("No stream available")
//                return@launch
//            }
//            streamManager.startStream(cam, stream, glSurfaceView, currentPalette, defaultColorSettings)
//            _state.value = CameraState.Streaming
//            startWatchdog()
//        }
//    }
//
//    // -------------------------
//    // RENDERER (1:1)
//    // -------------------------
//
//
//    // Renderer is now handled by RenderManager
//
//    // -------------------------
//    // CLEANUP
//    // -------------------------
//
//
//    fun shutdown() {
//        runBlocking {
//            streamManager.stopStream()
//            connectionManager.disconnect()
//            job.cancelAndJoin()
//            glSurfaceView = null
//            _state.value = CameraState.Idle
//        }
//    }
//
//    // --- Palette/Settings API ---
//    fun setPalette(palette: Palette) {
//        currentPalette = palette
//        renderManager.setPalette(palette)
//        streamManager.setPalette(palette)
//    }
//
//    fun setAutoRange(enabled: Boolean) {
//        streamManager.setAutoRange(enabled)
//    }
//
//    fun reconnect() {
//        shutdown()
//        startDiscoveryAndConnection()
//    }
//
//    // --- Internal Callbacks ---
//    private fun onCameraConnected(camera: Camera) {
//        _state.value = CameraState.Connected
//        startStream()
//    }
//
//    private fun onCameraError(message: String) {
//        _state.value = CameraState.Error(message)
//    }
//
//    private fun onStreamError(message: String) {
//        _state.value = CameraState.Error(message)
//    }
//
//    private fun onFrameReceived() {
//        lastFrameTimestamp = System.currentTimeMillis()
//    }
//
//    // --- Watchdog ---
//    private fun startWatchdog() {
//        scope.launch {
//            while (state.value is CameraState.Streaming) {
//                delay(WATCHDOG_TIMEOUT_MS)
//                val now = System.currentTimeMillis()
//                if (now - lastFrameTimestamp > WATCHDOG_TIMEOUT_MS) {
//                    streamManager.restartStream()
//                }
//            }
//        }
//    }
}
