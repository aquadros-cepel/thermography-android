package com.tech.thermography.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tech.thermography.android.data.local.entity.PlantEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PlantDao {
    @Query("SELECT * FROM plant ORDER BY name")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plant WHERE id = :id")
    suspend fun getPlantById(id: UUID): PlantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<PlantEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlant(plant: PlantEntity)
    
    @Update
    suspend fun updatePlant(plant: PlantEntity)
    
    @Delete
    suspend fun deletePlant(plant: PlantEntity)

}
