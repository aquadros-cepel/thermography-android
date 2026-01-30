package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.RiskPeriodicityDeadlineEntity
import com.tech.thermography.android.data.remote.mapper.RiskPeriodicityDeadlineMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
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

    suspend fun getAllOrMock(): List<RiskPeriodicityDeadlineEntity> {
        val list = riskPeriodicityDeadlineDao.getAllRiskPeriodicityDeadlinesOnce()
        if (list.isNotEmpty()) return list

        val mockList = buildMockList()

        // Persist mock data so subsequent calls/readers will see them
        riskPeriodicityDeadlineDao.insertRiskPeriodicityDeadlines(mockList)

        return mockList
    }

    private fun buildMockList(): List<RiskPeriodicityDeadlineEntity> {
        return listOf(
            RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(),
                name = "NORMAL",
                deadline = -1,
                deadlineUnit = null,
                periodicity = -1,
                periodicityUnit = null,
                recommendations = "Encerrar Monitoramento"
            ),
            RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(),
                name = "LOW_RISK",
                deadline = 3,
                deadlineUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.YEAR,
                periodicity = 6,
                periodicityUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.MONTH,
                recommendations = "Anomalia de Baixo Risco com critérios pré definidos a partir ISO 18434-1.Recomenda-se monitorar a anomalia avaliando seu agravamento, executar manutenção preventiva por aproveitamento em até 3 (três) anos."
            ),
            RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(),
                name = "MEDIUM_RISK",
                deadline = 1,
                deadlineUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.YEAR,
                periodicity = 3,
                periodicityUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.MONTH,
                recommendations = "Anomalia de Médio Risco para o componente selecionado, conforme equipamento, com critérios pré definidos a partir  ISO 18434-1. Recomenda-se monitorar a anomalia avaliando seu agravamento, executar manutenção preventiva por aproveitamento em até 1 (um) ano."
            ),
            RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(),
                name = "HIGH_RISK",
                deadline = 45,
                deadlineUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.DAY,
                periodicity = 15,
                periodicityUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.DAY,
                recommendations = "Anomalia de Alto Risco para o componente selecionado, conforme equipamento, com critérios pré definidos a partir  ISO 18434-1.Recomenda-se monitorar a anomalia avaliando seu agravamento,  programar intervenção em até  45 (dias), levar em consideração histórico de falha da família do equipamento na análise, complementando o diagnóstico com outros metódos de ensaios/análises."
            ),
            RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(),
                name = "IMMINENT_HIGH_RISK",
                deadline = 48,
                deadlineUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.HOUR,
                periodicity = 0,
                periodicityUnit = null,
                recommendations = "Anomalia com Risco Iminente para o componente selecionado, conforme equipamento, com critérios pré definidos a partir  ISO 18434-1. Recomenda-se programar intervenção de urgência, complementando o diagnóstico com outros metódos de ensaios/análises."
            )
        )
    }

    suspend fun resetMockData() {
        // delete all and re-insert mock
        riskPeriodicityDeadlineDao.deleteAllRiskPeriodicityDeadlines()
        val mockList = buildMockList()
        riskPeriodicityDeadlineDao.insertRiskPeriodicityDeadlines(mockList)
    }

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
