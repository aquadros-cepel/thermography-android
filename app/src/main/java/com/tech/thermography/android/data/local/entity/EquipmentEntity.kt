package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.time.LocalDate
import java.util.*
import com.tech.thermography.android.data.local.entity.enum.EquipmentType
import com.tech.thermography.android.data.local.entity.enum.PhaseType

@Entity(
    tableName = "equipment",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EquipmentGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId"), Index("groupId")]
)
data class EquipmentEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val code: String?,
    val name: String,
    val description: String?,
    val type: EquipmentType,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val voltageClass: Float?,
    val phaseType: PhaseType?,
    val startDate: LocalDate?,
    val latitude: Double?,
    val longitude: Double?,
    val plantId: UUID,
    val groupId: UUID?
)
