package com.tech.thermography.android.ui.camera

sealed class MeasurementState {
    abstract val label: String
    abstract val enabled: Boolean
    abstract val centerXFraction: Float
    abstract val centerYFraction: Float
    abstract val add: Boolean
    abstract val remove: Boolean
}