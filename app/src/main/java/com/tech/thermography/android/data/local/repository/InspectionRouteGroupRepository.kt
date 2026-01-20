package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.InspectionRouteGroupEntity
import com.tech.thermography.android.data.remote.mapper.InspectionRouteGroupMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectionRouteGroupRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<InspectionRouteGroupEntity>() {
    private val inspectionRouteGroupDao = db.inspectionRouteGroupDao()

    fun getAllInspectionRouteGroups(): Flow<List<InspectionRouteGroupEntity>> = 
        inspectionRouteGroupDao.getAllInspectionRouteGroups()

    suspend fun getInspectionRouteGroupById(id: UUID): InspectionRouteGroupEntity? = 
        inspectionRouteGroupDao.getInspectionRouteGroupById(id)

    suspend fun insertInspectionRouteGroup(inspectionRouteGroup: InspectionRouteGroupEntity) = 
        inspectionRouteGroupDao.insertInspectionRouteGroup(inspectionRouteGroup)

    suspend fun updateInspectionRouteGroup(inspectionRouteGroup: InspectionRouteGroupEntity) = 
        inspectionRouteGroupDao.updateInspectionRouteGroup(inspectionRouteGroup)

    suspend fun deleteInspectionRouteGroup(inspectionRouteGroup: InspectionRouteGroupEntity) = 
        inspectionRouteGroupDao.deleteInspectionRouteGroup(inspectionRouteGroup)

    override suspend fun syncEntities() {
        syncEntitiesOrdered()
    }

    suspend fun syncEntitiesOrdered() {
        val remoteEntities = syncApi.getAllInspectionRouteGroups()
        val (roots, children) = remoteEntities.partition { it.parentGroup?.id == null }
        val ordered = roots + children
        val entities = ordered.map { dto -> InspectionRouteGroupMapper.dtoToEntity(dto) }
        setCache(entities)
        insertCached()
    }

    override suspend fun insertCached() {
        inspectionRouteGroupDao.insertInspectionRouteGroups(cache)
    }
}
