package com.tech.thermography.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ThermographicInspectionRecordDao {
    @Query("SELECT * FROM thermographic_inspection_record ORDER BY createdAt DESC")
    fun getAllThermographicInspectionRecords(): Flow<List<ThermographicInspectionRecordEntity>>

    @Query("SELECT * FROM thermographic_inspection_record WHERE plantId = :plantId ORDER BY createdAt DESC")
    fun getThermographicInspectionRecordsByPlantId(plantId: UUID): Flow<List<ThermographicInspectionRecordEntity>>

    @Query("SELECT * FROM thermographic_inspection_record WHERE plantId = :plantId AND equipmentId = :equipmentId ORDER BY createdAt DESC")
    fun getThermographicInspectionRecordsByPlantAndEquipment(plantId: UUID, equipmentId: UUID): Flow<List<ThermographicInspectionRecordEntity>>

    @Query("SELECT * FROM thermographic_inspection_record WHERE id = :id")
    suspend fun getThermographicInspectionRecordById(id: UUID): ThermographicInspectionRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThermographicInspectionRecords(thermographicInspectionRecords: List<ThermographicInspectionRecordEntity>)
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertThermographicInspectionRecord(thermographicInspectionRecord: ThermographicInspectionRecordEntity)

    @Update
    suspend fun updateThermographicInspectionRecord(thermographicInspectionRecord: ThermographicInspectionRecordEntity)
    
    @Delete
    suspend fun deleteThermographicInspectionRecord(thermographicInspectionRecord: ThermographicInspectionRecordEntity)
}
