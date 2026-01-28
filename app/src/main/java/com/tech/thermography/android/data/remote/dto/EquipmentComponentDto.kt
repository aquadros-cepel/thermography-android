package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class EquipmentComponentDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val description: String?,
    val componentTemperatureLimits: EquipmentComponentTemperatureLimitsDto?
)
