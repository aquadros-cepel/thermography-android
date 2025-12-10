package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.RiskPeriodicityDeadlineEntity
import com.tech.thermography.android.data.remote.mapper.RiskPeriodicityDeadlineMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskPeriodicityDeadlineRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<RiskPeriodicityDeadlineEntity>() {
    private val riskPeriodicityDeadlineDao = db.riskPeriodicityDeadlineDao()

    fun getAllRiskPeriodicityDeadlines(): Flow<List<RiskPeriodicityDeadlineEntity>> = 
        riskPeriodicityDeadlineDao.getAllRiskPeriodicityDeadlines()

    suspend fun getRiskPeriodicityDeadlineById(id: UUID): RiskPeriodicityDeadlineEntity? = 
        riskPeriodicityDeadlineDao.getRiskPeriodicityDeadlineById(id)

    suspend fun insertRiskPeriodicityDeadline(riskPeriodicityDeadline: RiskPeriodicityDeadlineEntity) = 
        riskPeriodicityDeadlineDao.insertRiskPeriodicityDeadline(riskPeriodicityDeadline)

    suspend fun updateRiskPeriodicityDeadline(riskPeriodicityDeadline: RiskPeriodicityDeadlineEntity) = 
        riskPeriodicityDeadlineDao.updateRiskPeriodicityDeadline(riskPeriodicityDeadline)

    suspend fun deleteRiskPeriodicityDeadline(riskPeriodicityDeadline: RiskPeriodicityDeadlineEntity) = 
        riskPeriodicityDeadlineDao.deleteRiskPeriodicityDeadline(riskPeriodicityDeadline)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllRiskPeriodicityDeadlines()
        val entities = remoteEntities.map { dto -> RiskPeriodicityDeadlineMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        riskPeriodicityDeadlineDao.insertRiskPeriodicityDeadlines(cache)
    }
}
