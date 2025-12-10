package com.tech.thermography.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tech.thermography.android.data.local.entity.ROIEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ROIDao {
    @Query("SELECT * FROM roi ORDER BY label")
    fun getAllROIs(): Flow<List<ROIEntity>>

    @Query("SELECT * FROM roi WHERE id = :id")
    suspend fun getROIById(id: UUID): ROIEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertROIs(rois: List<ROIEntity>)
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertROI(roi: ROIEntity)

    @Update
    suspend fun updateROI(roi: ROIEntity)
    
    @Delete
    suspend fun deleteROI(roi: ROIEntity)
}
