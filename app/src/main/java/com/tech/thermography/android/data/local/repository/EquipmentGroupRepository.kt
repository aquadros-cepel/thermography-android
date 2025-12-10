package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.EquipmentGroupEntity
import com.tech.thermography.android.data.remote.mapper.EquipmentGroupMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentGroupRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<EquipmentGroupEntity>() {
    private val equipmentGroupDao = db.equipmentGroupDao()

    fun getAllEquipmentGroups(): Flow<List<EquipmentGroupEntity>> = 
        equipmentGroupDao.getAllEquipmentGroups()

    suspend fun getEquipmentGroupById(id: UUID): EquipmentGroupEntity? = 
        equipmentGroupDao.getEquipmentGroupById(id)

    suspend fun insertEquipmentGroup(equipmentGroup: EquipmentGroupEntity) = 
        equipmentGroupDao.insertEquipmentGroup(equipmentGroup)

    suspend fun updateEquipmentGroup(equipmentGroup: EquipmentGroupEntity) = 
        equipmentGroupDao.updateEquipmentGroup(equipmentGroup)

    suspend fun deleteEquipmentGroup(equipmentGroup: EquipmentGroupEntity) = 
        equipmentGroupDao.deleteEquipmentGroup(equipmentGroup)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllEquipmentGroups()
        val entities = remoteEntities.map { dto -> EquipmentGroupMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        equipmentGroupDao.insertEquipmentGroups(cache)
    }
}
