package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.EquipmentComponentTemperatureLimitsEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface EquipmentComponentTemperatureLimitsDao {
    @Query("SELECT * FROM equipment_component_temperature_limits ORDER BY name")
    fun getAllEquipmentComponentTemperatureLimits(): Flow<List<EquipmentComponentTemperatureLimitsEntity>>

    @Query("SELECT * FROM equipment_component_temperature_limits WHERE id = :id")
    suspend fun getEquipmentComponentTemperatureLimitsById(id: UUID): EquipmentComponentTemperatureLimitsEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipmentComponentTemperatureLimitsList(equipmentComponentTemperatureLimits: List<EquipmentComponentTemperatureLimitsEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits: EquipmentComponentTemperatureLimitsEntity)
    
    @Update
    suspend fun updateEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits: EquipmentComponentTemperatureLimitsEntity)
    
    @Delete
    suspend fun deleteEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits: EquipmentComponentTemperatureLimitsEntity)
}
