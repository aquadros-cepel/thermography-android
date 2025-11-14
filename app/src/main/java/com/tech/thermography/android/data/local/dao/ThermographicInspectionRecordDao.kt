package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface ThermographicInspectionRecordDao {
    @Query("SELECT * FROM thermographic_inspection_record ORDER BY createdAt DESC")
    fun getAllThermographicInspectionRecords(): Flow<List<ThermographicInspectionRecordEntity>>

    @Query("SELECT * FROM thermographic_inspection_record WHERE id = :id")
    suspend fun getThermographicInspectionRecordById(id: UUID): ThermographicInspectionRecordEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertThermographicInspectionRecord(thermographicInspectionRecord: ThermographicInspectionRecordEntity)
    
    @Update
    suspend fun updateThermographicInspectionRecord(thermographicInspectionRecord: ThermographicInspectionRecordEntity)
    
    @Delete
    suspend fun deleteThermographicInspectionRecord(thermographicInspectionRecord: ThermographicInspectionRecordEntity)
}
