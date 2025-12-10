package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.EquipmentComponentEntity
import com.tech.thermography.android.data.remote.mapper.EquipmentComponentMapper.dtoToEntity
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentComponentRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<EquipmentComponentEntity>() {
    private val equipmentComponentDao = db.equipmentComponentDao()

    fun getAllEquipmentComponents(): Flow<List<EquipmentComponentEntity>> = equipmentComponentDao.getAllEquipmentComponents()

    suspend fun getEquipmentComponentById(id: UUID): EquipmentComponentEntity? = equipmentComponentDao.getEquipmentComponentById(id)

    suspend fun insertEquipmentComponent(equipmentComponent: EquipmentComponentEntity) = equipmentComponentDao.insertEquipmentComponent(equipmentComponent)

    suspend fun deleteEquipmentComponent(equipmentComponent: EquipmentComponentEntity) = equipmentComponentDao.deleteEquipmentComponent(equipmentComponent)

    override suspend fun syncEntities() {
        // 1. Buscar tudo do backend
        val remoteEquipmentComponents = syncApi.getAllEquipmentComponents()

        // 2. Fazer o mapeamento para entidades Room e guardar no cache local
        val entities = remoteEquipmentComponents.map { dto -> dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        equipmentComponentDao.insertEquipmentComponents(cache)
        clearCache()
    }
}
