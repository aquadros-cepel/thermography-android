package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.InspectionRouteGroupEntity
import com.tech.thermography.android.data.remote.dto.InspectionRouteGroupDto

object InspectionRouteGroupMapper {
    fun dtoToEntity(dto: InspectionRouteGroupDto): InspectionRouteGroupEntity {
        return InspectionRouteGroupEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            included = dto.included,
            orderIndex = dto.orderIndex,
            inspectionRouteId = dto.inspectionRoute.id,
            parentGroupId = dto.parentGroup?.id
        )
    }
}
