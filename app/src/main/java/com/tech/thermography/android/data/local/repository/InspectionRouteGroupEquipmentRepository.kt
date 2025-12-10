package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.InspectionRouteGroupEquipmentEntity
import com.tech.thermography.android.data.remote.mapper.InspectionRouteGroupEquipmentMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectionRouteGroupEquipmentRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<InspectionRouteGroupEquipmentEntity>() {
    private val inspectionRouteGroupEquipmentDao = db.inspectionRouteGroupEquipmentDao()

    fun getAllInspectionRouteGroupEquipments(): Flow<List<InspectionRouteGroupEquipmentEntity>> = 
        inspectionRouteGroupEquipmentDao.getAllInspectionRouteGroupEquipments()

    suspend fun getInspectionRouteGroupEquipmentById(id: UUID): InspectionRouteGroupEquipmentEntity? = 
        inspectionRouteGroupEquipmentDao.getInspectionRouteGroupEquipmentById(id)

    fun getEquipmentsByGroupId(groupId: UUID): Flow<List<InspectionRouteGroupEquipmentEntity>> = 
        inspectionRouteGroupEquipmentDao.getEquipmentsByGroupId(groupId)

    suspend fun insertInspectionRouteGroupEquipment(equipment: InspectionRouteGroupEquipmentEntity) = 
        inspectionRouteGroupEquipmentDao.insertInspectionRouteGroupEquipment(equipment)

    suspend fun updateInspectionRouteGroupEquipment(equipment: InspectionRouteGroupEquipmentEntity) = 
        inspectionRouteGroupEquipmentDao.updateInspectionRouteGroupEquipment(equipment)

    suspend fun deleteInspectionRouteGroupEquipment(equipment: InspectionRouteGroupEquipmentEntity) = 
        inspectionRouteGroupEquipmentDao.deleteInspectionRouteGroupEquipment(equipment)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllInspectionRouteGroupEquipments()
        val entities = remoteEntities.map { dto -> InspectionRouteGroupEquipmentMapper.dtoToEntity(dto) }
        setCache(entities)
    }
    
    override suspend fun insertCached() {
        inspectionRouteGroupEquipmentDao.insertInspectionRouteGroupEquipments(cache)
    }
}
