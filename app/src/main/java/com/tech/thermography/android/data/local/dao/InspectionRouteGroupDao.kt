package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.InspectionRouteGroupEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface InspectionRouteGroupDao {
    @Query("SELECT * FROM inspection_route_group ORDER BY name")
    fun getAllInspectionRouteGroups(): Flow<List<InspectionRouteGroupEntity>>

    @Query("SELECT * FROM inspection_route_group WHERE id = :id")
    suspend fun getInspectionRouteGroupById(id: UUID): InspectionRouteGroupEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInspectionRouteGroup(inspectionRouteGroup: InspectionRouteGroupEntity)
    
    @Update
    suspend fun updateInspectionRouteGroup(inspectionRouteGroup: InspectionRouteGroupEntity)
    
    @Delete
    suspend fun deleteInspectionRouteGroup(inspectionRouteGroup: InspectionRouteGroupEntity)
}
