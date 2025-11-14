package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.EquipmentComponentEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface EquipmentComponentDao {
    @Query("SELECT * FROM equipment_component ORDER BY name")
    fun getAllEquipmentComponents(): Flow<List<EquipmentComponentEntity>>

    @Query("SELECT * FROM equipment_component WHERE id = :id")
    suspend fun getEquipmentComponentById(id: UUID): EquipmentComponentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipmentComponent(equipmentComponent: EquipmentComponentEntity)
    
    @Update
    suspend fun updateEquipmentComponent(equipmentComponent: EquipmentComponentEntity)
    
    @Delete
    suspend fun deleteEquipmentComponent(equipmentComponent: EquipmentComponentEntity)
}
