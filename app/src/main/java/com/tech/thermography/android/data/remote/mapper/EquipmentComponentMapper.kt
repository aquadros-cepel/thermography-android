package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.EquipmentComponentEntity
import com.tech.thermography.android.data.remote.dto.EquipmentComponentDto

object EquipmentComponentMapper {
    fun dtoToEntity(dto: EquipmentComponentDto): EquipmentComponentEntity {
        return EquipmentComponentEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description
            // If needed, persist temperature limits separately via its own repository using dto.componentTemperatureLimits
        )
    }
}
