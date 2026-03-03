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
import com.flir.thermalsdk.live.streaming.Stream
import com.flir.thermalsdk.log.ThermalLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AceController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "AceController"
        private val ACE_INTERFACE = CommunicationInterface.ACE
    }

    private enum class State {
        Idle,
        SurfaceReady,
        CameraReady,
        Streaming,
        FirstFrame,
        PipelineReady
    }
    private var state = State.Idle

    private var glView: GLSurfaceView? = null
    private var renderer: AceRenderer? = null

    private var camera: Camera? = null
    private var activeStream: Stream? = null

    private var surfaceWidth = 0
    private var surfaceHeight = 0

    private var firstFrameArrived = false


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
        ThermalSdkAndroid.init(context, ThermalLog.LogLevel.DEBUG);
        discoveryAndConnectCamera()
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


        } catch (e: Exception) {
            ThermalLog.e(TAG, "Connection failed: ${e.message}")
        }
    }

    fun startStream() {
        if (state != State.CameraReady) return
        if (renderer == null) return


        glView?.queueEvent {

            val cam = camera ?: return@queueEvent
            val stream = cam.streams.firstOrNull() ?: return@queueEvent

            cam.glSetupPipeline(stream, true)
            cam.glOnSurfaceChanged(480, 640)

            stream.start(
                { glView?.requestRender() },
                { ThermalLog.e(TAG, "Stream error $it") }
            )

            state = State.Streaming
            ThermalLog.d(TAG, "State: $state")
        }
    }
    // ---------- GL CALLBACKS ----------
    fun onGlSurfaceCreated() {
        state = State.SurfaceReady
        ThermalLog.d(TAG, "State: $state")
    }

    fun onGlSurfaceSizeKnown(w: Int, h: Int) {
        surfaceWidth = w
        surfaceHeight = h

        ThermalLog.d(TAG, "Height: $surfaceHeight x Width: $surfaceWidth")

        if (camera != null) {
            camera?.glOnSurfaceChanged(480, 640)

            ThermalLog.d(TAG, "Camera Surface Changed: $surfaceHeight x Width: $surfaceWidth")
        };
    }

    fun onGlDrawFrame() {
        if (state != State.PipelineReady) return
        // camera?.glDraw()
    }

}
