package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.RiskRecommendationTranslationEntity
import com.tech.thermography.android.data.remote.dto.RiskRecommendationTranslationDto

object RiskRecommendationTranslationMapper {
    fun dtoToEntity(dto: RiskRecommendationTranslationDto): RiskRecommendationTranslationEntity {
        return RiskRecommendationTranslationEntity(
            id = dto.id,
            language = dto.language,
            name = dto.name,
            riskPeriodicityDeadlineId = dto.riskPeriodicityDeadlineId
        )
    }
}
