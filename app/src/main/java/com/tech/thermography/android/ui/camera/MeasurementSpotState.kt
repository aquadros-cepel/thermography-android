package com.tech.thermography.android.ui.camera

data class MeasurementSpotState(
    override val label: String = "Sp1",
    override val enabled: Boolean = false,
    override val centerXFraction: Float = 0.5f,
    override val centerYFraction: Float = 0.5f,
    override val add: Boolean = false,
    override val remove: Boolean = false
) : MeasurementState()