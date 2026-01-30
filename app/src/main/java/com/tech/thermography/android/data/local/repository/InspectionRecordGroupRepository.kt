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
) : AbstractSyncRepository<InspectionRecordGroupEntity>() {
    private val inspectionRecordGroupDao = db.inspectionRecordGroupDao()

    fun getAllInspectionRecordGroups(): Flow<List<InspectionRecordGroupEntity>> = inspectionRecordGroupDao.getAllInspectionRecordGroups()

    suspend fun getInspectionRecordGroupById(id: UUID): InspectionRecordGroupEntity? = inspectionRecordGroupDao.getInspectionRecordGroupById(id)

    suspend fun getGroupsByInspectionRecordIdOnce(recordId: UUID): List<InspectionRecordGroupEntity> = inspectionRecordGroupDao.getGroupsByRecordIdOnce(recordId)

    suspend fun insertInspectionRecordGroup(inspectionRecordGroup: InspectionRecordGroupEntity) = inspectionRecordGroupDao.insertInspectionRecordGroup(inspectionRecordGroup)

    suspend fun deleteInspectionRecordGroup(inspectionRecordGroup: InspectionRecordGroupEntity) = inspectionRecordGroupDao.deleteInspectionRecordGroup(inspectionRecordGroup)

    override suspend fun syncEntities() {
        try {
            syncEntitiesOrdered()
        } catch (e: Exception) {
            // Loga todos os objetos que seriam inseridos
            cache.forEach { entity ->
                try {
                    // Tenta inserir individualmente para identificar o problemático
                    inspectionRecordGroupDao.insertInspectionRecordGroup(entity)
                } catch (ex: Exception) {
                    android.util.Log.e("InspectionRecordGroupRepository", "Erro ao inserir entity: $entity", ex)
                }
            }
            throw e
        }
    }

    private suspend fun syncEntitiesOrdered() {
        val remoteInspectionRecordGroups = syncApi.getAllInspectionRecordGroups()
        val (roots, children) = remoteInspectionRecordGroups.partition { it.parentGroup?.id == null }
        val ordered = roots + children
        val entities = ordered.map { dto -> InspectionRecordGroupMapper.dtoToEntity(dto) }
        setCache(entities)
        insertCached()
    }

    override suspend fun insertCached() {
        inspectionRecordGroupDao.insertInspectionRecordGroups(cache)
    }
}
