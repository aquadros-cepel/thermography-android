package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "inspection_route_group_equipment",
    foreignKeys = [
        ForeignKey(
            entity = InspectionRouteGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspectionRouteGroupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("inspectionRouteGroupId"), Index("equipmentId")]
)
data class InspectionRouteGroupEquipmentEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val included: Boolean?,
    val orderIndex: Int?,
    val inspectionRouteGroupId: UUID?,
    val equipmentId: UUID?
)
