package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.time.Instant
import java.util.*

@Entity(
    tableName = "thermogram",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["createdById"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("equipmentId"), Index("createdById")]
)
data class ThermogramEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val imagePath: String,
    val audioPath: String?,
    val imageRefPath: String,
    val minTemp: Double?,
    val avgTemp: Double?,
    val maxTemp: Double?,
    val emissivity: Double?,
    val subjectDistance: Double?,
    val atmosphericTemp: Double?,
    val reflectedTemp: Double?,
    val relativeHumidity: Double?,
    val cameraLens: String?,
    val cameraModel: String?,
    val imageResolution: String?,
    val selectedRoiId: UUID?,
    val maxTempRoi: Double?,
    val createdAt: Instant?,
    val latitude: Double?,
    val longitude: Double?,
    val equipmentId: UUID,
    val createdById: UUID
)
