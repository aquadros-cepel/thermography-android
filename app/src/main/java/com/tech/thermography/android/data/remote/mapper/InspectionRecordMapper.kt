package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.remote.dto.InspectionRecordDto

object InspectionRecordMapper {
    fun dtoToEntity(dto: InspectionRecordDto): InspectionRecordEntity {
        return InspectionRecordEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            maintenanceDocument = dto.maintenanceDocument,
            createdAt = dto.createdAt,
            expectedStartDate = dto.expectedStartDate,
            expectedEndDate = dto.expectedEndDate,
            started = dto.started,
            startedAt = dto.startedAt,
            finished = dto.finished,
            finishedAt = dto.finishedAt,
            plantId = dto.plantId,
            inspectionRouteId = dto.inspectionRouteId,
            createdById = dto.createdById,
            startedById = dto.startedById,
            finishedById = dto.finishedById
        )
    }
}
