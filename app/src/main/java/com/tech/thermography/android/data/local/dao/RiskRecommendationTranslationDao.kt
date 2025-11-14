package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.RiskRecommendationTranslationEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface RiskRecommendationTranslationDao {
    @Query("SELECT * FROM risk_recommendation_translation ORDER BY language, name")
    fun getAllRiskRecommendationTranslations(): Flow<List<RiskRecommendationTranslationEntity>>

    @Query("SELECT * FROM risk_recommendation_translation WHERE id = :id")
    suspend fun getRiskRecommendationTranslationById(id: UUID): RiskRecommendationTranslationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRiskRecommendationTranslation(riskRecommendationTranslation: RiskRecommendationTranslationEntity)
    
    @Update
    suspend fun updateRiskRecommendationTranslation(riskRecommendationTranslation: RiskRecommendationTranslationEntity)
    
    @Delete
    suspend fun deleteRiskRecommendationTranslation(riskRecommendationTranslation: RiskRecommendationTranslationEntity)
}
