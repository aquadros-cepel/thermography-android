package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.BusinessUnitEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface BusinessUnitDao {
    @Query("SELECT * FROM business_unit ORDER BY name")
    fun getAllBusinessUnits(): Flow<List<BusinessUnitEntity>>

    @Query("SELECT * FROM business_unit WHERE id = :id")
    suspend fun getBusinessUnitById(id: UUID): BusinessUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessUnits(businessUnits: List<BusinessUnitEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBusinessUnit(businessUnit: BusinessUnitEntity)
    
    @Update
    suspend fun updateBusinessUnit(businessUnit: BusinessUnitEntity)
    
    @Delete
    suspend fun deleteBusinessUnit(businessUnit: BusinessUnitEntity)
}
