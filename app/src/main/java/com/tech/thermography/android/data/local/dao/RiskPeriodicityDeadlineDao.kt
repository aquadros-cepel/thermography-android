package com.tech.thermography.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tech.thermography.android.data.local.entity.RiskPeriodicityDeadlineEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface RiskPeriodicityDeadlineDao {
    @Query("SELECT * FROM risk_periodicity_deadline ORDER BY name")
    fun getAllRiskPeriodicityDeadlines(): Flow<List<RiskPeriodicityDeadlineEntity>>

    @Query("SELECT * FROM risk_periodicity_deadline ORDER BY name")
    suspend fun getAllRiskPeriodicityDeadlinesOnce(): List<RiskPeriodicityDeadlineEntity>

    @Query("SELECT * FROM risk_periodicity_deadline WHERE id = :id")
    suspend fun getRiskPeriodicityDeadlineById(id: UUID): RiskPeriodicityDeadlineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiskPeriodicityDeadlines(riskPeriodicityDeadlines: List<RiskPeriodicityDeadlineEntity>)
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRiskPeriodicityDeadline(riskPeriodicityDeadline: RiskPeriodicityDeadlineEntity)

    @Update
    suspend fun updateRiskPeriodicityDeadline(riskPeriodicityDeadline: RiskPeriodicityDeadlineEntity)
    
    @Delete
    suspend fun deleteRiskPeriodicityDeadline(riskPeriodicityDeadline: RiskPeriodicityDeadlineEntity)

    @Query("DELETE FROM risk_periodicity_deadline")
    suspend fun deleteAllRiskPeriodicityDeadlines()
}
