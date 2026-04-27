package com.tech.thermography.android.ui.camera

data class MeasurementSquareState(
    override val label: String = "Bx1",
    override val enabled: Boolean = false,
    override val centerXFraction: Float = 0.5f,
    override val centerYFraction: Float = 0.5f,
    val sizeFraction: Float = 0.25f,
    val initialSizeFraction: Float = 0.25f,
    override val add: Boolean = false,
    override val remove: Boolean = false
) : MeasurementState()