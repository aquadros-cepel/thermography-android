package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "risk_recommendation_translation",
    foreignKeys = [
        ForeignKey(
            entity = RiskPeriodicityDeadlineEntity::class,
            parentColumns = ["id"],
            childColumns = ["riskPeriodicityDeadlineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("riskPeriodicityDeadlineId")]
)
data class RiskRecommendationTranslationEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val language: String,
    val name: String,
    val riskPeriodicityDeadlineId: UUID
)
