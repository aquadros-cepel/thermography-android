package com.tech.thermography.android.data.remote.dto

import java.time.Instant
import java.util.UUID

data class ThermogramDto(
    val id: UUID,
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
