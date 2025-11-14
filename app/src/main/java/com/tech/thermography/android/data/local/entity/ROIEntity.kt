package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "roi",
    foreignKeys = [
        ForeignKey(
            entity = ThermogramEntity::class,
            parentColumns = ["id"],
            childColumns = ["thermogramId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("thermogramId")]
)
data class ROIEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val type: String,
    val label: String,
    val maxTemp: Double,
    val thermogramId: UUID
)
