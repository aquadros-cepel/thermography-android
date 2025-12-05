package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.InspectionRouteGroupEquipmentEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface InspectionRouteGroupEquipmentDao {
    @Query("SELECT * FROM inspection_route_group_equipment ORDER BY orderIndex")
    fun getAllInspectionRouteGroupEquipments(): Flow<List<InspectionRouteGroupEquipmentEntity>>

    @Query("SELECT * FROM inspection_route_group_equipment WHERE id = :id")
    suspend fun getInspectionRouteGroupEquipmentById(id: UUID): InspectionRouteGroupEquipmentEntity?

    @Query("SELECT * FROM inspection_route_group_equipment WHERE inspectionRouteGroupId = :groupId ORDER BY orderIndex")
    fun getEquipmentsByGroupId(groupId: UUID): Flow<List<InspectionRouteGroupEquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInspectionRouteGroupEquipment(equipment: InspectionRouteGroupEquipmentEntity)
    
    @Update
    suspend fun updateInspectionRouteGroupEquipment(equipment: InspectionRouteGroupEquipmentEntity)
    
    @Delete
    suspend fun deleteInspectionRouteGroupEquipment(equipment: InspectionRouteGroupEquipmentEntity)
}
