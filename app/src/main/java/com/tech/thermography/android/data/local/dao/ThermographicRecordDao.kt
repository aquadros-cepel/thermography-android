package com.tech.thermography.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.tech.thermography.android.data.local.entity.ThermographicRecord
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.local.entity.UserInfoEntity
import com.tech.thermography.android.data.local.entity.EquipmentComponentEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ThermographicRecordDao {
    @Query("SELECT * FROM thermographic_record ORDER BY createdAt DESC")
    fun getAllThermographicRecords(): Flow<List<ThermographicRecord>>

    @Query("SELECT * FROM thermographic_record WHERE plantId = :plantId ORDER BY createdAt DESC")
    fun getThermographicRecordsByPlantId(plantId: UUID): Flow<List<ThermographicRecord>>

    @Query("SELECT * FROM thermographic_record WHERE plantId = :plantId AND equipmentId = :equipmentId ORDER BY createdAt DESC")
    fun getThermographicRecordsByPlantAndEquipment(plantId: UUID, equipmentId: UUID): Flow<List<ThermographicRecord>>

    @Query("SELECT * FROM thermographic_record WHERE id = :id")
    suspend fun getThermographicRecordById(id: UUID): ThermographicRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThermographicRecords(thermographicRecords: List<ThermographicRecord>)
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertThermographicRecord(thermographicRecord: ThermographicRecord)

    @Update
    suspend fun updateThermographicRecord(thermographicRecord: ThermographicRecord)
    
    @Delete
    suspend fun deleteThermographicRecord(thermographicRecord: ThermographicRecord)

    @Query("SELECT * FROM thermographic_record WHERE syncStatus != 'SYNCED'")
    suspend fun getRecordsToSync(): List<ThermographicRecord>

    @Query("UPDATE thermographic_record SET syncStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: UUID, status: String)

    @Query("DELETE FROM thermographic_record WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Transaction
    @Query("SELECT * FROM thermographic_record WHERE id = :id")
    suspend fun getThermographicRecordWithRelations(id: UUID): ThermographicRecordWithRelations
}

// Data class para retorno
// Inclua todos os campos relacionados necessários
// Ajuste conforme suas entidades

data class ThermographicRecordWithRelations(
    @Embedded val record: ThermographicRecord,
    @Relation(parentColumn = "plantId", entityColumn = "id") val plant: PlantEntity?,
    @Relation(parentColumn = "routeId", entityColumn = "id") val route: InspectionRecordEntity?,
    @Relation(parentColumn = "equipmentId", entityColumn = "id") val equipment: EquipmentEntity?,
    @Relation(parentColumn = "componentId", entityColumn = "id") val component: EquipmentComponentEntity?,
    @Relation(parentColumn = "createdById", entityColumn = "id") val createdBy: UserInfoEntity?,
    @Relation(parentColumn = "finishedById", entityColumn = "id") val finishedBy: UserInfoEntity?,
    @Relation(parentColumn = "thermogramId", entityColumn = "id") val thermogram: ThermogramEntity?,
    @Relation(parentColumn = "thermogramRefId", entityColumn = "id") val thermogramRef: ThermogramEntity?
)
