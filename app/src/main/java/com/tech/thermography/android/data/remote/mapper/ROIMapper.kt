package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.remote.dto.ROIDto

object ROIMapper {
    fun dtoToEntity(dto: ROIDto): ROIEntity {
        return ROIEntity(
            id = dto.id,
            type = dto.type,
            label = dto.label,
            maxTemp = dto.maxTemp,
            thermogramId = dto.thermogramId
        )
    }
}
