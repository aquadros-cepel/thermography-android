package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity(
    tableName = "inspection_record",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InspectionRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspectionRouteId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["createdById"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["startedById"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["finishedById"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("plantId"), Index("inspectionRouteId"), Index("createdById"), Index("startedById"), Index("finishedById")]
)
data class InspectionRecordEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val code: String?,
    val name: String,
    val description: String?,
    val maintenanceDocument: String?,
    val createdAt: Instant,
    val expectedStartDate: LocalDate,
    val expectedEndDate: LocalDate,
    val started: Boolean?,
    val startedAt: Instant?,
    val finished: Boolean?,
    val finishedAt: Instant?,
    val plantId: UUID,
    val inspectionRouteId: UUID?,
    val createdById: UUID,
    val startedById: UUID?,
    val finishedById: UUID?
)
