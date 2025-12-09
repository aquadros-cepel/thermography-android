package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.remote.dto.ThermogramDto

object ThermogramMapper {
    fun dtoToEntity(dto: ThermogramDto): ThermogramEntity {
        return ThermogramEntity(
            id = dto.id,
            imagePath = dto.imagePath,
            audioPath = dto.audioPath,
            imageRefPath = dto.imageRefPath,
            minTemp = dto.minTemp,
            avgTemp = dto.avgTemp,
            maxTemp = dto.maxTemp,
            emissivity = dto.emissivity,
            subjectDistance = dto.subjectDistance,
            atmosphericTemp = dto.atmosphericTemp,
            reflectedTemp = dto.reflectedTemp,
            relativeHumidity = dto.relativeHumidity,
            cameraLens = dto.cameraLens,
            cameraModel = dto.cameraModel,
            imageResolution = dto.imageResolution,
            selectedRoiId = dto.selectedRoiId,
            maxTempRoi = dto.maxTempRoi,
            createdAt = dto.createdAt,
            latitude = dto.latitude,
            longitude = dto.longitude,
            equipmentId = dto.equipmentId,
            createdById = dto.createdById
        )
    }
}
