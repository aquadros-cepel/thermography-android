package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class EquipmentComponentTemperatureLimitsDto(
    val id: UUID,
    val name: String?,
    val normal: String?,
    val lowRisk: String?,
    val mediumRisk: String?,
    val highRisk: String?,
    val imminentHighRisk: String?,
    val componentId: UUID?,
    val equipmentComponent: EquipmentComponentDto?
)
