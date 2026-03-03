package com.tech.thermography.android.flir

import android.content.Context
import com.flir.thermalsdk.live.Camera
import com.flir.thermalsdk.live.CameraType
import com.flir.thermalsdk.live.CommunicationInterface
import com.flir.thermalsdk.live.ConnectParameters
import com.flir.thermalsdk.live.Identity
import com.flir.thermalsdk.live.discovery.DiscoveredCamera
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener
import com.flir.thermalsdk.live.discovery.DiscoveryFactory
import kotlin.coroutines.resume

class CameraConnectionManager(
    private val context: Context,
    private val onConnected: (Camera) -> Unit,
    private val onError: (String) -> Unit
) {
    @Volatile
    var camera: Camera? = null
        private set

    suspend fun discoverAndConnect(interfaceType: CommunicationInterface) {
        // Only ACE supported
        val found = kotlinx.coroutines.suspendCancellableCoroutine<Identity> { cont ->
            DiscoveryFactory.getInstance().scan(object : DiscoveryEventListener {
                override fun onCameraFound(camera: DiscoveredCamera) {
                    val id = camera.identity
                    if (id.cameraType == CameraType.ACE && id.communicationInterface == interfaceType) {
                        DiscoveryFactory.getInstance().stop(interfaceType)
                        if (!cont.isCompleted) cont.resume(id)
                    }
                }
                override fun onDiscoveryError(communicationInterface: CommunicationInterface, error: com.flir.thermalsdk.ErrorCode) {
                    if (!cont.isCompleted) cont.resumeWith(Result.failure(Exception("Discovery error: $error")))
                }
            }, interfaceType)
        }
        connect(found)
    }

    suspend fun connect(identity: Identity) {
        val cam = Camera()
        try {
            cam.connect(identity, { onError("Connection error: $it") }, ConnectParameters())
            camera = cam
            onConnected(cam)
        } catch (e: Exception) {
            onError("Connection failed: ${e.message}")
        }
    }

    suspend fun disconnect() {
        camera?.let {
            try {
                it.glTeardownPipeline()
                it.disconnect()
            } catch (_: Exception) {}
        }
        camera = null
    }
}
