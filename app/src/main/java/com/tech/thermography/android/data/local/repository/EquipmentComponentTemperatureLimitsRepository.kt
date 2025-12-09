package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.EquipmentComponentTemperatureLimitsEntity
import com.tech.thermography.android.data.remote.mapper.EquipmentComponentTemperatureLimitsMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentComponentTemperatureLimitsRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : SyncableRepository {
    private val equipmentComponentTemperatureLimitsDao = db.equipmentComponentTemperatureLimitsDao()

    fun getAllEquipmentComponentTemperatureLimits(): Flow<List<EquipmentComponentTemperatureLimitsEntity>> = 
        equipmentComponentTemperatureLimitsDao.getAllEquipmentComponentTemperatureLimits()

    suspend fun getEquipmentComponentTemperatureLimitsById(id: UUID): EquipmentComponentTemperatureLimitsEntity? = 
        equipmentComponentTemperatureLimitsDao.getEquipmentComponentTemperatureLimitsById(id)

    suspend fun insertEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits: EquipmentComponentTemperatureLimitsEntity) = 
        equipmentComponentTemperatureLimitsDao.insertEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits)

    suspend fun updateEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits: EquipmentComponentTemperatureLimitsEntity) = 
        equipmentComponentTemperatureLimitsDao.updateEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits)

    suspend fun deleteEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits: EquipmentComponentTemperatureLimitsEntity) = 
        equipmentComponentTemperatureLimitsDao.deleteEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllEquipmentComponentTemperatureLimits()
        val entities = remoteEntities.map { dto -> EquipmentComponentTemperatureLimitsMapper.dtoToEntity(dto) }

        db.runInTransaction {
            runBlocking {
                entities.forEach { equipmentComponentTemperatureLimitsDao.insertEquipmentComponentTemperatureLimits(it) }
            }
        }
    }
}
