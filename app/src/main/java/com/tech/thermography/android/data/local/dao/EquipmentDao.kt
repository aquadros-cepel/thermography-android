package com.tech.thermography.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment ORDER BY name")
    fun getAllEquipments(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun getEquipmentById(id: UUID): EquipmentEntity?

    @Query("SELECT * FROM equipment WHERE plantId = :plantId ORDER BY name")
    fun getEquipmentsByPlantId(plantId: UUID): Flow<List<EquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipments(equipments: List<EquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipment(equipment: EquipmentEntity)
    
    @Update
    suspend fun updateEquipment(equipment: EquipmentEntity)
    
    @Delete
    suspend fun deleteEquipment(equipment: EquipmentEntity)
}
