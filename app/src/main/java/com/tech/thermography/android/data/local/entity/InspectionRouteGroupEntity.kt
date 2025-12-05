package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "inspection_route_group",
    foreignKeys = [
        ForeignKey(
            entity = InspectionRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspectionRouteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InspectionRouteGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentGroupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("inspectionRouteId"), Index("parentGroupId")]
)
data class InspectionRouteGroupEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val code: String?,
    val name: String,
    val description: String?,
    val included: Boolean?,
    val orderIndex: Int?,
    val inspectionRouteId: UUID?,
    val parentGroupId: UUID?
)
