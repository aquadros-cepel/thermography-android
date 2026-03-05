package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.remote.dto.ROIDto
import com.tech.thermography.android.data.remote.dto.UUID_DTO

object ROIMapper {
    fun dtoToEntity(dto: ROIDto): ROIEntity {
        return ROIEntity(
            id = dto.id,
            type = dto.type,
            label = dto.label,
            maxTemp = dto.maxTemp,
            thermogramId = requireNotNull(dto.thermogram.id) { "thermogramId cannot be null in ROIDto" }
        )
    }

    fun entityToDto(entity: ROIEntity): ROIDto {
        return ROIDto(
            id = entity.id,
            type = entity.type,
            label = entity.label,
            maxTemp = entity.maxTemp,
            thermogram = UUID_DTO(entity.thermogramId)
        )
    }
}
