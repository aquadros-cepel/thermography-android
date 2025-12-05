package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.time.Instant
import java.util.*

@Entity(
    tableName = "inspection_record_group",
    foreignKeys = [
        ForeignKey(
            entity = InspectionRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspectionRecordId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InspectionRecordGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentGroupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("inspectionRecordId"), Index("parentGroupId")]
)
data class InspectionRecordGroupEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val code: String?,
    val name: String,
    val description: String?,
    val orderIndex: Int?,
    val finished: Boolean?,
    val finishedAt: Instant?,
    val inspectionRecordId: UUID?,
    val parentGroupId: UUID?
)
