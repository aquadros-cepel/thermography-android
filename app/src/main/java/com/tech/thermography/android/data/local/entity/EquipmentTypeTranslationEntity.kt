package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(tableName = "equipment_type_translation")
data class EquipmentTypeTranslationEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val code: EquipmentType,
    val language: String,
    val name: String
)
