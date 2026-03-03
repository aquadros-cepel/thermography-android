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
            plantId = requireNotNull(dto.plant.id) { "plantId cannot be null in ThermographicInspectionRecordDto" },
            routeId = dto.route?.id,
            equipmentId = requireNotNull(dto.equipment.id) { "equipmentId cannot be null in ThermographicInspectionRecordDto" },
            componentId = dto.component?.id,
            createdById = requireNotNull(dto.createdBy.id) { "createdById cannot be null in ThermographicInspectionRecordDto" },
            finishedById = requireNotNull(dto.finishedBy.id) { "finishedById cannot be null in ThermographicInspectionRecordDto" },
            thermogramId = requireNotNull(dto.thermogram.id) { "thermogramId cannot be null in ThermographicInspectionRecordDto" },
            thermogramRefId = dto.thermogramRef?.id
        )
    }
}
