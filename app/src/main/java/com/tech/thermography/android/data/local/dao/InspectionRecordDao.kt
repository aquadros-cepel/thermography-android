package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface InspectionRecordDao {
    @Query("SELECT * FROM inspection_record ORDER BY createdAt DESC")
    fun getAllInspectionRecords(): Flow<List<InspectionRecordEntity>>

    @Query("SELECT * FROM inspection_record WHERE id = :id")
    suspend fun getInspectionRecordById(id: UUID): InspectionRecordEntity?

    @Query("SELECT * FROM inspection_record WHERE plantId = :plantId ORDER BY createdAt DESC")
    fun getInspectionRecordsByPlantId(plantId: UUID): Flow<List<InspectionRecordEntity>>

    @Query("SELECT * FROM inspection_record WHERE inspectionRouteId = :routeId ORDER BY createdAt DESC")
    fun getInspectionRecordsByRouteId(routeId: UUID): Flow<List<InspectionRecordEntity>>

    @Query("SELECT * FROM inspection_record WHERE finished = :finished ORDER BY createdAt DESC")
    fun getInspectionRecordsByStatus(finished: Boolean): Flow<List<InspectionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInspectionRecord(record: InspectionRecordEntity)
    
    @Update
    suspend fun updateInspectionRecord(record: InspectionRecordEntity)
    
    @Delete
    suspend fun deleteInspectionRecord(record: InspectionRecordEntity)
}
