package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.PlantEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface PlantDao {
    @Query("SELECT * FROM plant ORDER BY name")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plant WHERE id = :id")
    suspend fun getPlantById(id: UUID): PlantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: PlantEntity)

    @Delete
    suspend fun deletePlant(plant: PlantEntity)
}
