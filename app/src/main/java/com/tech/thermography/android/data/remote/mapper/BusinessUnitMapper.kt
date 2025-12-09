package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.BusinessUnitEntity
import com.tech.thermography.android.data.remote.dto.BusinessUnitDto

object BusinessUnitMapper {
    fun dtoToEntity(dto: BusinessUnitDto): BusinessUnitEntity {
        return BusinessUnitEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            companyId = dto.companyId
        )
    }
}
