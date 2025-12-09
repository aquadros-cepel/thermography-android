package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.time.Instant
import java.time.LocalDate
import java.util.*
import com.tech.thermography.android.data.local.entity.enumeration.Periodicity

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
        )
    ],
    indices = [Index("plantId"), Index("createdById")]
)
data class InspectionRouteEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val code: String?,
    val name: String,
    val description: String?,
    val maintenancePlan: String?,
    val periodicity: Periodicity?,
    val duration: Int?,
    val expectedStartDate: LocalDate?,
    val createdAt: Instant,
    val plantId: UUID,
    val createdById: UUID
)
