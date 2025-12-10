package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.EquipmentTypeTranslationEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface EquipmentTypeTranslationDao {
    @Query("SELECT * FROM equipment_type_translation ORDER BY language, name")
    fun getAllEquipmentTypeTranslations(): Flow<List<EquipmentTypeTranslationEntity>>

    @Query("SELECT * FROM equipment_type_translation WHERE id = :id")
    suspend fun getEquipmentTypeTranslationById(id: UUID): EquipmentTypeTranslationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipmentTypeTranslations(equipmentTypeTranslations: List<EquipmentTypeTranslationEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipmentTypeTranslation(equipmentTypeTranslation: EquipmentTypeTranslationEntity)
    
    @Update
    suspend fun updateEquipmentTypeTranslation(equipmentTypeTranslation: EquipmentTypeTranslationEntity)
    
    @Delete
    suspend fun deleteEquipmentTypeTranslation(equipmentTypeTranslation: EquipmentTypeTranslationEntity)
}
