package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.remote.mapper.EquipmentMapper.dtoToEntity
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<EquipmentEntity>() {
    private val equipmentDao = db.equipmentDao()

    fun getAllEquipments(): Flow<List<EquipmentEntity>> = equipmentDao.getAllEquipments()

    fun getEquipmentsByPlantId(plantId: UUID): Flow<List<EquipmentEntity>> = equipmentDao.getEquipmentsByPlantId(plantId)

    suspend fun getEquipmentById(id: UUID): EquipmentEntity? = equipmentDao.getEquipmentById(id)

    suspend fun insertEquipment(equipment: EquipmentEntity) = equipmentDao.insertEquipment(equipment)

    suspend fun deleteEquipment(equipment: EquipmentEntity) = equipmentDao.deleteEquipment(equipment)

    override suspend fun syncEntities() {
        val remoteEquipments = syncApi.getAllEquipments()
        val entities = remoteEquipments.map { dto -> dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        equipmentDao.insertEquipments(cache)
    }
}
