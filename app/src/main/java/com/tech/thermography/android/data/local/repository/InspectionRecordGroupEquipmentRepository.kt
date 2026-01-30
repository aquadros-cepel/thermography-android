package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.dao.InspectionRecordGroupEquipmentWithEquipment
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEquipmentEntity
import com.tech.thermography.android.data.remote.mapper.InspectionRecordGroupEquipmentMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectionRecordGroupEquipmentRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<InspectionRecordGroupEquipmentEntity>() {
    private val inspectionRecordGroupEquipmentDao = db.inspectionRecordGroupEquipmentDao()

    fun getAllInspectionRecordGroupEquipments(): Flow<List<InspectionRecordGroupEquipmentEntity>> =
        inspectionRecordGroupEquipmentDao.getAllInspectionRecordGroupEquipments()

    suspend fun getInspectionRecordGroupEquipmentById(id: UUID): InspectionRecordGroupEquipmentEntity? =
        inspectionRecordGroupEquipmentDao.getInspectionRecordGroupEquipmentById(id)

    suspend fun getByGroupIdOnce(groupId: UUID): List<InspectionRecordGroupEquipmentEntity> =
        inspectionRecordGroupEquipmentDao.getEquipmentsByGroupIdOnce(groupId)

    fun getEquipmentsByGroupId(groupId: UUID): Flow<List<InspectionRecordGroupEquipmentEntity>> =
        inspectionRecordGroupEquipmentDao.getEquipmentsByGroupId(groupId)

    fun getGroupEquipmentsByEquipmentId(equipmentId: UUID): Flow<List<InspectionRecordGroupEquipmentEntity>> =
        inspectionRecordGroupEquipmentDao.getGroupEquipmentsByEquipmentId(equipmentId)

    fun getEquipmentsByStatus(status: String): Flow<List<InspectionRecordGroupEquipmentEntity>> =
        inspectionRecordGroupEquipmentDao.getEquipmentsByStatus(status)

    suspend fun insertInspectionRecordGroupEquipment(equipment: InspectionRecordGroupEquipmentEntity) =
        inspectionRecordGroupEquipmentDao.insertInspectionRecordGroupEquipment(equipment)

    suspend fun updateInspectionRecordGroupEquipment(equipment: InspectionRecordGroupEquipmentEntity) =
        inspectionRecordGroupEquipmentDao.updateInspectionRecordGroupEquipment(equipment)

    suspend fun deleteInspectionRecordGroupEquipment(equipment: InspectionRecordGroupEquipmentEntity) =
        inspectionRecordGroupEquipmentDao.deleteInspectionRecordGroupEquipment(equipment)

    // New: fetch links with equipment using a transaction for a list of group ids
    suspend fun getInspectionRecordGroupEquipmentsWithEquipmentByGroupIdsOnce(groupIds: List<UUID>): List<InspectionRecordGroupEquipmentWithEquipment> {
        return inspectionRecordGroupEquipmentDao.getInspectionRecordGroupEquipmentsWithEquipmentByGroupIdsOnce(groupIds)
    }

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllInspectionRecordGroupEquipments()
        val entities = remoteEntities.map { dto -> InspectionRecordGroupEquipmentMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        inspectionRecordGroupEquipmentDao.insertInspectionRecordGroupEquipments(cache)
    }
}
