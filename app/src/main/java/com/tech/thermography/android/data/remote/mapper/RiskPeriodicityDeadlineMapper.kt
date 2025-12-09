package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.RiskPeriodicityDeadlineEntity
import com.tech.thermography.android.data.remote.dto.RiskPeriodicityDeadlineDto

object RiskPeriodicityDeadlineMapper {
    fun dtoToEntity(dto: RiskPeriodicityDeadlineDto): RiskPeriodicityDeadlineEntity {
        return RiskPeriodicityDeadlineEntity(
            id = dto.id,
            name = dto.name,
            deadline = dto.deadline,
            deadlineUnit = dto.deadlineUnit,
            periodicity = dto.periodicity,
            periodicityUnit = dto.periodicityUnit,
            recommendations = dto.recommendations
        )
    }
}
