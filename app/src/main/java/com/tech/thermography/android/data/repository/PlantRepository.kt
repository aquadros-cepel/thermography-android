package com.tech.thermography.android.data.repository

import com.tech.thermography.android.data.local.dao.PlantDao
import com.tech.thermography.android.data.local.entity.PlantEntity
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantRepository @Inject constructor(
    private val plantDao: PlantDao
) {

    fun getAllPlants(): Flow<List<PlantEntity>> = plantDao.getAllPlants()

    suspend fun getPlantById(id: UUID): PlantEntity? = plantDao.getPlantById(id)

    suspend fun insertPlant(plant: PlantEntity) = plantDao.insertPlant(plant)

    suspend fun deletePlant(plant: PlantEntity) = plantDao.deletePlant(plant)
}
