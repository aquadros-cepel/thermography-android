package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.remote.dto.ThermogramDto

object ThermogramMapper {
    fun dtoToEntity(dto: ThermogramDto): ThermogramEntity {
        return ThermogramEntity(
            id = dto.id,
            localImagePath = "", // Remote DTO doesn't have local path
            imagePath = dto.imagePath,
            audioPath = dto.audioPath,
            localImageRefPath = "", // Remote DTO doesn't have local path
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
            equipmentId = requireNotNull(dto.equipment.id) { "equipmentId cannot be null in ThermogramDto" },
            createdById = requireNotNull(dto.createdBy.id) { "createdById cannot be null in ThermogramDto" }
        )
    }
}
