package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(tableName = "equipment_component")
data class EquipmentComponentEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val title: String?,
    val description: String?
)
