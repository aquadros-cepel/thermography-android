package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity(
    tableName = "inspection_route",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
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
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["finishedById"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId"), Index("createdById"), Index("startedById"), Index("finishedById")]
)
data class InspectionRouteEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val title: String?,
    val description: String?,
    val planNote: String?,
    val createdAt: Instant,
    val startDate: LocalDate,
    val started: Boolean?,
    val startedAt: Instant?,
    val endDate: LocalDate,
    val finished: Boolean?,
    val finishedAt: Instant?,
    val plantId: UUID,
    val createdById: UUID,
    val startedById: UUID?,
    val finishedById: UUID?
)
