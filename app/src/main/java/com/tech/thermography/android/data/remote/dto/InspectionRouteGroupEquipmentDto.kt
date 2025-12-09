package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class InspectionRouteGroupEquipmentDto(
    val id: UUID,
    val included: Boolean?,
    val orderIndex: Int?,
    val inspectionRouteGroupId: UUID?,
    val equipmentId: UUID?
)
