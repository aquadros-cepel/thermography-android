package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.InspectionRouteEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface InspectionRouteDao {
    @Query("SELECT * FROM inspection_route ORDER BY name")
    fun getAllInspectionRoutes(): Flow<List<InspectionRouteEntity>>

    @Query("SELECT * FROM inspection_route WHERE id = :id")
    suspend fun getInspectionRouteById(id: UUID): InspectionRouteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInspectionRoute(inspectionRoute: InspectionRouteEntity)
    
    @Update
    suspend fun updateInspectionRoute(inspectionRoute: InspectionRouteEntity)
    
    @Delete
    suspend fun deleteInspectionRoute(inspectionRoute: InspectionRouteEntity)
}
