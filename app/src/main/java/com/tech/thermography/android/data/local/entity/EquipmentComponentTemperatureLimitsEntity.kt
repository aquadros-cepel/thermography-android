package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "equipment_component_temperature_limits",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentComponentEntity::class,
            parentColumns = ["id"],
            childColumns = ["componentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("componentId")]
)
data class EquipmentComponentTemperatureLimitsEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String?,
    val normal: String?,
    val lowRisk: String?,
    val mediumRisk: String?,
    val highRisk: String?,
    val imminentHighRisk: String?,
    val componentId: UUID?
)
