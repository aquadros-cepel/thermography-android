package com.tech.thermography.android.data.remote.dto

import com.tech.thermography.android.data.local.entity.enumeration.EquipmentType
import com.tech.thermography.android.data.local.entity.enumeration.PhaseType
import java.time.LocalDate
import java.util.UUID

data class EquipmentDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val description: String?,
    val type: EquipmentType,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val voltageClass: Float?,
    val phaseType: PhaseType,
    val startDate: LocalDate?,
    val latitude: Double?,
    val longitude: Double?,
    val plantId: UUID,
    val groupId: UUID?
)
