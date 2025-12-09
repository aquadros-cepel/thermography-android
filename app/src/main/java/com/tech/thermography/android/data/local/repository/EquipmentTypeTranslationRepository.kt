package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.EquipmentTypeTranslationEntity
import com.tech.thermography.android.data.remote.mapper.EquipmentTypeTranslationMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentTypeTranslationRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : SyncableRepository {
    private val equipmentTypeTranslationDao = db.equipmentTypeTranslationDao()

    fun getAllEquipmentTypeTranslations(): Flow<List<EquipmentTypeTranslationEntity>> = equipmentTypeTranslationDao.getAllEquipmentTypeTranslations()

    suspend fun getEquipmentTypeTranslationById(id: UUID): EquipmentTypeTranslationEntity? = equipmentTypeTranslationDao.getEquipmentTypeTranslationById(id)

    suspend fun insertEquipmentTypeTranslation(equipmentTypeTranslation: EquipmentTypeTranslationEntity) = equipmentTypeTranslationDao.insertEquipmentTypeTranslation(equipmentTypeTranslation)

    suspend fun deleteEquipmentTypeTranslation(equipmentTypeTranslation: EquipmentTypeTranslationEntity) = equipmentTypeTranslationDao.deleteEquipmentTypeTranslation(equipmentTypeTranslation)

    override suspend fun syncEntities() {
        val remoteEquipmentTypeTranslations = syncApi.getAllEquipmentTypeTranslations()
        val entities = remoteEquipmentTypeTranslations.map { dto -> EquipmentTypeTranslationMapper.dtoToEntity(dto) }

        db.runInTransaction {
            runBlocking {
                entities.forEach { equipmentTypeTranslationDao.insertEquipmentTypeTranslation(it) }
            }
        }
    }
}
