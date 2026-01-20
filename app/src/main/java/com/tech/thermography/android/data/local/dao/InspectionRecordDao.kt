package com.tech.thermography.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspectionRecords(records: List<InspectionRecordEntity>)

    // Alterado de ABORT para REPLACE para permitir atualização via insert (upsert)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspectionRecord(record: InspectionRecordEntity)
    
    @Update
    suspend fun updateInspectionRecord(record: InspectionRecordEntity)
    
    @Delete
    suspend fun deleteInspectionRecord(record: InspectionRecordEntity)

    @Query("""
        SELECT ir.* FROM inspection_record ir
        INNER JOIN inspection_record_group irg2 ON irg2.inspectionRecordId = ir.id
        INNER JOIN inspection_record_group irg ON irg.parentGroupId = irg2.id
        INNER JOIN inspection_record_group_equipment irgp ON irgp.inspectionRecordGroupId = irg.id
        INNER JOIN equipment e ON e.id = irgp.equipmentId
        WHERE e.id = :equipmentId
    """)
    suspend fun getInspectionRecordsByEquipmentId(equipmentId: UUID): List<InspectionRecordEntity>

    @Query("""
        SELECT ir.* FROM inspection_record ir
        INNER JOIN inspection_record_group irg2 ON irg2.inspectionRecordId = ir.id
    """)
    suspend fun getInspectionRecords(): List<InspectionRecordEntity>

    @Query("SELECT COUNT(*) FROM inspection_record")
    suspend fun getAllInspectionRecordsCount(): Int

}
