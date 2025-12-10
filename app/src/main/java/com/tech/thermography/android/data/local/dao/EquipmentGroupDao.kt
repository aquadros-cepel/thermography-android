package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.EquipmentGroupEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface EquipmentGroupDao {
    @Query("SELECT * FROM equipment_group ORDER BY name")
    fun getAllEquipmentGroups(): Flow<List<EquipmentGroupEntity>>

    @Query("SELECT * FROM equipment_group WHERE id = :id")
    suspend fun getEquipmentGroupById(id: UUID): EquipmentGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipmentGroups(equipmentGroups: List<EquipmentGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipmentGroup(equipmentGroup: EquipmentGroupEntity)
    
    @Update
    suspend fun updateEquipmentGroup(equipmentGroup: EquipmentGroupEntity)
    
    @Delete
    suspend fun deleteEquipmentGroup(equipmentGroup: EquipmentGroupEntity)
}
