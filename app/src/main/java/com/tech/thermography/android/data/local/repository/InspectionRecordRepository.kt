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
) : SyncableRepository {
    private val inspectionRecordDao = db.inspectionRecordDao()

    fun getAllInspectionRecords(): Flow<List<InspectionRecordEntity>> = inspectionRecordDao.getAllInspectionRecords()

    suspend fun getInspectionRecordById(id: UUID): InspectionRecordEntity? = inspectionRecordDao.getInspectionRecordById(id)

    suspend fun insertInspectionRecord(inspectionRecord: InspectionRecordEntity) = inspectionRecordDao.insertInspectionRecord(inspectionRecord)

    suspend fun deleteInspectionRecord(inspectionRecord: InspectionRecordEntity) = inspectionRecordDao.deleteInspectionRecord(inspectionRecord)

    override suspend fun syncEntities() {
        val remoteInspectionRecords = syncApi.getAllInspectionRecords()
        val entities = remoteInspectionRecords.map { dto -> InspectionRecordMapper.dtoToEntity(dto) }

        db.runInTransaction {
            runBlocking {
                entities.forEach { inspectionRecordDao.insertInspectionRecord(it) }
            }
        }
    }
}
