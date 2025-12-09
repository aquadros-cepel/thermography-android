package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.InspectionRouteEntity
import com.tech.thermography.android.data.remote.dto.InspectionRouteDto

object InspectionRouteMapper {
    fun dtoToEntity(dto: InspectionRouteDto): InspectionRouteEntity {
        return InspectionRouteEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            maintenancePlan = dto.maintenancePlan,
            periodicity = dto.periodicity,
            duration = dto.duration,
            expectedStartDate = dto.expectedStartDate,
            createdAt = dto.createdAt,
            plantId = dto.plantId,
            createdById = dto.createdById
        )
    }
}
