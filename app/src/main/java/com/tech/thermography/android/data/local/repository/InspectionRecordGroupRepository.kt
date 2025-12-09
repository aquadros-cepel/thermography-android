package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEntity
import com.tech.thermography.android.data.remote.mapper.InspectionRecordGroupMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectionRecordGroupRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : SyncableRepository {
    private val inspectionRecordGroupDao = db.inspectionRecordGroupDao()

    fun getAllInspectionRecordGroups(): Flow<List<InspectionRecordGroupEntity>> = inspectionRecordGroupDao.getAllInspectionRecordGroups()

    suspend fun getInspectionRecordGroupById(id: UUID): InspectionRecordGroupEntity? = inspectionRecordGroupDao.getInspectionRecordGroupById(id)

    suspend fun insertInspectionRecordGroup(inspectionRecordGroup: InspectionRecordGroupEntity) = inspectionRecordGroupDao.insertInspectionRecordGroup(inspectionRecordGroup)

    suspend fun deleteInspectionRecordGroup(inspectionRecordGroup: InspectionRecordGroupEntity) = inspectionRecordGroupDao.deleteInspectionRecordGroup(inspectionRecordGroup)

    override suspend fun syncEntities() {
        val remoteInspectionRecordGroups = syncApi.getAllInspectionRecordGroups()
        val entities = remoteInspectionRecordGroups.map { dto -> InspectionRecordGroupMapper.dtoToEntity(dto) }

        db.runInTransaction {
            runBlocking {
                entities.forEach { inspectionRecordGroupDao.insertInspectionRecordGroup(it) }
            }
        }
    }
}
