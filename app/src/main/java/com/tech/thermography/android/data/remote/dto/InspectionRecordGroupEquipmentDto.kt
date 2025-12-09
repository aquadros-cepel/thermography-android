package com.tech.thermography.android.data.remote.dto

import com.tech.thermography.android.data.local.entity.enumeration.EquipmentInspectionStatus
import java.util.UUID

data class InspectionRecordGroupEquipmentDto(
    val id: UUID,
    val orderIndex: Int?,
    val status: EquipmentInspectionStatus?,
    val inspectionRecordGroupId: UUID?,
    val equipmentId: UUID?
)
