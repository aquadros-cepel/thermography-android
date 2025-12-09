package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.RiskRecommendationTranslationEntity
import com.tech.thermography.android.data.remote.mapper.RiskRecommendationTranslationMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskRecommendationTranslationRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : SyncableRepository {
    private val riskRecommendationTranslationDao = db.riskRecommendationTranslationDao()

    fun getAllRiskRecommendationTranslations(): Flow<List<RiskRecommendationTranslationEntity>> = 
        riskRecommendationTranslationDao.getAllRiskRecommendationTranslations()

    suspend fun getRiskRecommendationTranslationById(id: UUID): RiskRecommendationTranslationEntity? = 
        riskRecommendationTranslationDao.getRiskRecommendationTranslationById(id)

    suspend fun insertRiskRecommendationTranslation(riskRecommendationTranslation: RiskRecommendationTranslationEntity) = 
        riskRecommendationTranslationDao.insertRiskRecommendationTranslation(riskRecommendationTranslation)

    suspend fun updateRiskRecommendationTranslation(riskRecommendationTranslation: RiskRecommendationTranslationEntity) = 
        riskRecommendationTranslationDao.updateRiskRecommendationTranslation(riskRecommendationTranslation)

    suspend fun deleteRiskRecommendationTranslation(riskRecommendationTranslation: RiskRecommendationTranslationEntity) = 
        riskRecommendationTranslationDao.deleteRiskRecommendationTranslation(riskRecommendationTranslation)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllRiskRecommendationTranslations()
        val entities = remoteEntities.map { dto -> RiskRecommendationTranslationMapper.dtoToEntity(dto) }

        db.runInTransaction {
            runBlocking {
                entities.forEach { riskRecommendationTranslationDao.insertRiskRecommendationTranslation(it) }
            }
        }
    }
}
