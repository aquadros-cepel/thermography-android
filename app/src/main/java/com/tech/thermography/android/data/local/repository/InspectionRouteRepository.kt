package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.InspectionRouteEntity
import com.tech.thermography.android.data.remote.mapper.InspectionRouteMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectionRouteRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<InspectionRouteEntity>() {
    private val inspectionRouteDao = db.inspectionRouteDao()

    fun getAllInspectionRoutes(): Flow<List<InspectionRouteEntity>> = 
        inspectionRouteDao.getAllInspectionRoutes()

    suspend fun getInspectionRouteById(id: UUID): InspectionRouteEntity? = 
        inspectionRouteDao.getInspectionRouteById(id)

    suspend fun insertInspectionRoute(inspectionRoute: InspectionRouteEntity) = 
        inspectionRouteDao.insertInspectionRoute(inspectionRoute)

    suspend fun updateInspectionRoute(inspectionRoute: InspectionRouteEntity) = 
        inspectionRouteDao.updateInspectionRoute(inspectionRoute)

    suspend fun deleteInspectionRoute(inspectionRoute: InspectionRouteEntity) = 
        inspectionRouteDao.deleteInspectionRoute(inspectionRoute)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllInspectionRoutes()
        val entities = remoteEntities.map { dto -> InspectionRouteMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        inspectionRouteDao.insertInspectionRoutes(cache)
    }
}
