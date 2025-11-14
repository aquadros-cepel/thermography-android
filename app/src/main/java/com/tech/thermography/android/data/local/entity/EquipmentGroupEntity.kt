package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "equipment_group",
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
            childColumns = ["parentGroupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId"), Index("parentGroupId")]
)
data class EquipmentGroupEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val title: String?,
    val description: String?,
    val plantId: UUID?,
    val parentGroupId: UUID?
)
