package com.tech.thermography.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEquipmentEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface InspectionRecordGroupEquipmentDao {
    @Query("SELECT * FROM inspection_record_group_equipment ORDER BY orderIndex")
    fun getAllInspectionRecordGroupEquipments(): Flow<List<InspectionRecordGroupEquipmentEntity>>

    @Query("SELECT * FROM inspection_record_group_equipment WHERE id = :id")
    suspend fun getInspectionRecordGroupEquipmentById(id: UUID): InspectionRecordGroupEquipmentEntity?

    @Query("SELECT * FROM inspection_record_group_equipment WHERE inspectionRecordGroupId = :groupId ORDER BY orderIndex")
    fun getEquipmentsByGroupId(groupId: UUID): Flow<List<InspectionRecordGroupEquipmentEntity>>

    @Query("SELECT * FROM inspection_record_group_equipment WHERE equipmentId = :equipmentId")
    fun getGroupEquipmentsByEquipmentId(equipmentId: UUID): Flow<List<InspectionRecordGroupEquipmentEntity>>

    @Query("SELECT * FROM inspection_record_group_equipment WHERE status = :status ORDER BY orderIndex")
    fun getEquipmentsByStatus(status: String): Flow<List<InspectionRecordGroupEquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspectionRecordGroupEquipments(equipments: List<InspectionRecordGroupEquipmentEntity>)
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInspectionRecordGroupEquipment(equipment: InspectionRecordGroupEquipmentEntity)

    @Update
    suspend fun updateInspectionRecordGroupEquipment(equipment: InspectionRecordGroupEquipmentEntity)
    
    @Delete
    suspend fun deleteInspectionRecordGroupEquipment(equipment: InspectionRecordGroupEquipmentEntity)
}
