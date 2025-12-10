package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity
import com.tech.thermography.android.data.remote.mapper.ThermographicInspectionRecordMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermographicInspectionRecordRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<ThermographicInspectionRecordEntity>() {
    private val thermographicInspectionRecordDao = db.thermographicInspectionRecordDao()

    fun getAllThermographicInspectionRecords(): Flow<List<ThermographicInspectionRecordEntity>> = 
        thermographicInspectionRecordDao.getAllThermographicInspectionRecords()

    suspend fun getThermographicInspectionRecordById(id: UUID): ThermographicInspectionRecordEntity? = 
        thermographicInspectionRecordDao.getThermographicInspectionRecordById(id)

    suspend fun insertThermographicInspectionRecord(thermographicInspectionRecord: ThermographicInspectionRecordEntity) = 
        thermographicInspectionRecordDao.insertThermographicInspectionRecord(thermographicInspectionRecord)

    suspend fun updateThermographicInspectionRecord(thermographicInspectionRecord: ThermographicInspectionRecordEntity) = 
        thermographicInspectionRecordDao.updateThermographicInspectionRecord(thermographicInspectionRecord)

    suspend fun deleteThermographicInspectionRecord(thermographicInspectionRecord: ThermographicInspectionRecordEntity) = 
        thermographicInspectionRecordDao.deleteThermographicInspectionRecord(thermographicInspectionRecord)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllThermographicInspectionRecords()
        val entities = remoteEntities.map { dto -> ThermographicInspectionRecordMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        thermographicInspectionRecordDao.insertThermographicInspectionRecords(cache)
    }
}
