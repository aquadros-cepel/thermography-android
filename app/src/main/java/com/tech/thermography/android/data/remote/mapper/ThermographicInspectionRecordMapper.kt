package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity
import com.tech.thermography.android.data.remote.dto.ThermographicInspectionRecordDto

object ThermographicInspectionRecordMapper {
    fun dtoToEntity(dto: ThermographicInspectionRecordDto): ThermographicInspectionRecordEntity {
        return ThermographicInspectionRecordEntity(
            id = dto.id,
            name = dto.name,
            type = dto.type,
            serviceOrder = dto.serviceOrder,
            createdAt = dto.createdAt,
            analysisDescription = dto.analysisDescription,
            condition = dto.condition,
            deltaT = dto.deltaT,
            periodicity = dto.periodicity,
            deadlineExecution = dto.deadlineExecution,
            nextMonitoring = dto.nextMonitoring,
            recommendations = dto.recommendations,
            finished = dto.finished,
            finishedAt = dto.finishedAt,
            plantId = dto.plantId,
            routeId = dto.routeId,
            equipmentId = dto.equipmentId,
            componentId = dto.componentId,
            createdById = dto.createdById,
            finishedById = dto.finishedById,
            thermogramId = dto.thermogramId,
            thermogramRefId = dto.thermogramRefId
        )
    }
}
