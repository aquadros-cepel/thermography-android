package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.InspectionRouteGroupEquipmentEntity
import com.tech.thermography.android.data.remote.dto.InspectionRouteGroupEquipmentDto

object InspectionRouteGroupEquipmentMapper {
    fun dtoToEntity(dto: InspectionRouteGroupEquipmentDto): InspectionRouteGroupEquipmentEntity {
        return InspectionRouteGroupEquipmentEntity(
            id = dto.id,
            included = dto.included,
            orderIndex = dto.orderIndex,
            inspectionRouteGroupId = dto.inspectionRouteGroupId,
            equipmentId = dto.equipmentId
        )
    }
}
