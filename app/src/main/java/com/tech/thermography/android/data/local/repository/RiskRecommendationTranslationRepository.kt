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
) : AbstractSyncRepository<RiskRecommendationTranslationEntity>() {
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
        // 1. Buscar tudo do backend
        val remoteEntities = syncApi.getAllRiskRecommendationTranslations()

        // 2. Fazer o mapeamento para entidades Room e guardar no cache local
        val entities = remoteEntities.map { dto -> RiskRecommendationTranslationMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        // 3. Transação única -> desempenho máximo
        riskRecommendationTranslationDao.insertRiskRecommendationTranslations(cache)
    }
}
