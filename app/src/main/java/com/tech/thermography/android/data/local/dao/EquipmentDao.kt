package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment ORDER BY name")
    fun getAllEquipments(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun getEquipmentById(id: UUID): EquipmentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipments(equipments: List<EquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipment(equipment: EquipmentEntity)
    
    @Update
    suspend fun updateEquipment(equipment: EquipmentEntity)
    
    @Delete
    suspend fun deleteEquipment(equipment: EquipmentEntity)
}
