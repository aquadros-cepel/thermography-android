package com.tech.thermography.android.ui.camera

import com.flir.thermalsdk.image.TemperatureUnit
import com.flir.thermalsdk.image.ThermalValue

data class ThermalParametersConfig(
    val distance: Double = 1.0,
    val emissivity: Double = 0.95,
    val reflectedTemperature: ThermalValue = ThermalValue(20.0, TemperatureUnit.CELSIUS),
    val atmosphericTemperature: ThermalValue = ThermalValue(20.0, TemperatureUnit.CELSIUS),
    val relativeHumidity: Double = 50.0
)