package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEquipmentEntity
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class InspectionRecordGroupEquipmentWithEquipment(
    @Embedded val link: InspectionRecordGroupEquipmentEntity,
    @Relation(parentColumn = "equipmentId", entityColumn = "id")
    val equipment: EquipmentEntity?
)

@Dao
interface InspectionRecordGroupEquipmentDao {
    @Query("SELECT * FROM inspection_record_group_equipment ORDER BY orderIndex")
    fun getAllInspectionRecordGroupEquipments(): Flow<List<InspectionRecordGroupEquipmentEntity>>

    @Query("SELECT * FROM inspection_record_group_equipment WHERE id = :id")
    suspend fun getInspectionRecordGroupEquipmentById(id: UUID): InspectionRecordGroupEquipmentEntity?

    @Query("SELECT * FROM inspection_record_group_equipment WHERE inspectionRecordGroupId = :groupId ORDER BY orderIndex")
    fun getEquipmentsByGroupId(groupId: UUID): Flow<List<InspectionRecordGroupEquipmentEntity>>

    @Query("SELECT * FROM inspection_record_group_equipment WHERE inspectionRecordGroupId = :groupId ORDER BY orderIndex")
    suspend fun getEquipmentsByGroupIdOnce(groupId: UUID): List<InspectionRecordGroupEquipmentEntity>

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

    @Transaction
    @Query("SELECT * FROM inspection_record_group_equipment WHERE inspectionRecordGroupId IN (:groupIds) ORDER BY orderIndex")
    suspend fun getInspectionRecordGroupEquipmentsWithEquipmentByGroupIdsOnce(groupIds: List<UUID>): List<InspectionRecordGroupEquipmentWithEquipment>
}
