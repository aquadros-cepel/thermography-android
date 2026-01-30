package com.tech.thermography.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tech.thermography.android.data.local.entity.EquipmentComponentTemperatureLimitsEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface EquipmentComponentTemperatureLimitsDao {
    @Query("SELECT * FROM equipment_component_temperature_limits ORDER BY name")
    fun getAllEquipmentComponentTemperatureLimits(): Flow<List<EquipmentComponentTemperatureLimitsEntity>>

    @Query("SELECT * FROM equipment_component_temperature_limits WHERE id = :id")
    suspend fun getEquipmentComponentTemperatureLimitsById(id: UUID): EquipmentComponentTemperatureLimitsEntity?

    @Query("SELECT * FROM equipment_component_temperature_limits WHERE componentId = :componentId LIMIT 1")
    suspend fun getEquipmentComponentTemperatureLimitsByComponentId(componentId: UUID): EquipmentComponentTemperatureLimitsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipmentComponentTemperatureLimitsList(equipmentComponentTemperatureLimits: List<EquipmentComponentTemperatureLimitsEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits: EquipmentComponentTemperatureLimitsEntity)

    @Update
    suspend fun updateEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits: EquipmentComponentTemperatureLimitsEntity)

    @Delete
    suspend fun deleteEquipmentComponentTemperatureLimits(equipmentComponentTemperatureLimits: EquipmentComponentTemperatureLimitsEntity)
}
