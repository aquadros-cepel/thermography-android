package com.tech.thermography.android.ui.camera.components

data class MeasurementSquareState(
    val label: String = "Bx1",
    val enabled: Boolean = false,
    val centerXFraction: Float = 0.5f,
    val centerYFraction: Float = 0.5f,
    val sizeFraction: Float = 0.18f,
    val initialSizeFraction: Float = 0.18f,
    val add: Boolean = false,
    val remove: Boolean = false
)
