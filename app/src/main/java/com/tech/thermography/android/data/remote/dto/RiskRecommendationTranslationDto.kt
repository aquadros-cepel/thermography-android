package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class RiskRecommendationTranslationDto(
    val id: UUID,
    val language: String,
    val name: String,
    val riskPeriodicityDeadlineId: UUID
)
