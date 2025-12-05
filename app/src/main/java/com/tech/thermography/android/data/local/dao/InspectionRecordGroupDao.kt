package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface InspectionRecordGroupDao {
    @Query("SELECT * FROM inspection_record_group ORDER BY orderIndex")
    fun getAllInspectionRecordGroups(): Flow<List<InspectionRecordGroupEntity>>

    @Query("SELECT * FROM inspection_record_group WHERE id = :id")
    suspend fun getInspectionRecordGroupById(id: UUID): InspectionRecordGroupEntity?

    @Query("SELECT * FROM inspection_record_group WHERE inspectionRecordId = :recordId ORDER BY orderIndex")
    fun getGroupsByRecordId(recordId: UUID): Flow<List<InspectionRecordGroupEntity>>

    @Query("SELECT * FROM inspection_record_group WHERE parentGroupId = :parentId ORDER BY orderIndex")
    fun getSubGroupsByParentId(parentId: UUID): Flow<List<InspectionRecordGroupEntity>>

    @Query("SELECT * FROM inspection_record_group WHERE finished = :finished ORDER BY orderIndex")
    fun getGroupsByStatus(finished: Boolean): Flow<List<InspectionRecordGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInspectionRecordGroup(group: InspectionRecordGroupEntity)
    
    @Update
    suspend fun updateInspectionRecordGroup(group: InspectionRecordGroupEntity)
    
    @Delete
    suspend fun deleteInspectionRecordGroup(group: InspectionRecordGroupEntity)
}
