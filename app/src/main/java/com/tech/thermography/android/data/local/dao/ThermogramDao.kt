package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface ThermogramDao {
    @Query("SELECT * FROM thermogram ORDER BY createdAt DESC")
    fun getAllThermograms(): Flow<List<ThermogramEntity>>

    @Query("SELECT * FROM thermogram WHERE id = :id")
    suspend fun getThermogramById(id: UUID): ThermogramEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertThermogram(thermogram: ThermogramEntity)
    
    @Update
    suspend fun updateThermogram(thermogram: ThermogramEntity)
    
    @Delete
    suspend fun deleteThermogram(thermogram: ThermogramEntity)
}
