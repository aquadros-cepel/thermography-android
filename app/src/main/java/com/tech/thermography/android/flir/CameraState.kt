package com.tech.thermography.android.flir

sealed interface CameraState {
    object Idle : CameraState
    object Discovering : CameraState
    object Connecting : CameraState
    object Connected : CameraState
    object Streaming : CameraState
    data class Error(val message: String) : CameraState
}
