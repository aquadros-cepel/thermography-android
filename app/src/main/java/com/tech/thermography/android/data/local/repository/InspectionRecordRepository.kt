package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.remote.mapper.InspectionRecordMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectionRecordRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<InspectionRecordEntity>() {
    private val inspectionRecordDao = db.inspectionRecordDao()

    fun getAllInspectionRecords(): Flow<List<InspectionRecordEntity>> = inspectionRecordDao.getAllInspectionRecords()

    suspend fun getInspectionRecordById(id: UUID): InspectionRecordEntity? = inspectionRecordDao.getInspectionRecordById(id)

    suspend fun insertInspectionRecord(inspectionRecord: InspectionRecordEntity) = inspectionRecordDao.insertInspectionRecord(inspectionRecord)

    suspend fun deleteInspectionRecord(inspectionRecord: InspectionRecordEntity) = inspectionRecordDao.deleteInspectionRecord(inspectionRecord)

    override suspend fun syncEntities() {
        // 1. Buscar tudo do backend
        val remoteInspectionRecords = syncApi.getAllInspectionRecords()

        // 2. Fazer o mapeamento para entidades Room e guardar no cache local
        val entities = remoteInspectionRecords.map { dto -> InspectionRecordMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        inspectionRecordDao.insertInspectionRecords(cache)
        clearCache()
    }
}
