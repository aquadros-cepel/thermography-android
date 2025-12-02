package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.remote.mapper.PlantMapper.dtoToEntity
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) {
    private val plantDao = db.plantDao()
    
    fun getAllPlants(): Flow<List<PlantEntity>> = plantDao.getAllPlants()

    suspend fun getPlantById(id: UUID): PlantEntity? = plantDao.getPlantById(id)

    suspend fun insertPlant(plant: PlantEntity) = plantDao.insertPlant(plant)

    suspend fun deletePlant(plant: PlantEntity) = plantDao.deletePlant(plant)

    suspend fun syncPlants() {

        val remotePlants = syncApi.getAllPlants()
        val entities = remotePlants.map {plant -> dtoToEntity(plant) }

        db.runInTransaction {
            runBlocking {
                plantDao.insertPlants(entities)
            }
        }
    }
}
