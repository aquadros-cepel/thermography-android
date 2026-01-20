package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEquipmentEntity
import com.tech.thermography.android.data.remote.dto.InspectionRecordGroupEquipmentDto

object InspectionRecordGroupEquipmentMapper {
    fun dtoToEntity(dto: InspectionRecordGroupEquipmentDto): InspectionRecordGroupEquipmentEntity {
        return InspectionRecordGroupEquipmentEntity(
            id = dto.id,
            orderIndex = dto.orderIndex,
            status = dto.status,
            inspectionRecordGroupId = dto.inspectionRecordGroup?.id,
            equipmentId = dto.equipment?.id
        )
    }
}
