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
) : SyncableRepository {
    private val equipmentComponentDao = db.equipmentComponentDao()

    fun getAllEquipmentComponents(): Flow<List<EquipmentComponentEntity>> = equipmentComponentDao.getAllEquipmentComponents()

    suspend fun getEquipmentComponentById(id: UUID): EquipmentComponentEntity? = equipmentComponentDao.getEquipmentComponentById(id)

    suspend fun insertEquipmentComponent(equipmentComponent: EquipmentComponentEntity) = equipmentComponentDao.insertEquipmentComponent(equipmentComponent)

    suspend fun deleteEquipmentComponent(equipmentComponent: EquipmentComponentEntity) = equipmentComponentDao.deleteEquipmentComponent(equipmentComponent)

    override suspend fun syncEntities() {
        val remoteEquipmentComponents = syncApi.getAllEquipmentComponents()
        val entities = remoteEquipmentComponents.map { dto -> dtoToEntity(dto) }

        db.runInTransaction {
            runBlocking {
                entities.forEach { equipmentComponentDao.insertEquipmentComponent(it) }
            }
        }
    }
}
