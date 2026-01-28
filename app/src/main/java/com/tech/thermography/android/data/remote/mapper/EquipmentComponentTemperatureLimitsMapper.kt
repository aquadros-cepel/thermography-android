package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.EquipmentComponentTemperatureLimitsEntity
import com.tech.thermography.android.data.remote.dto.EquipmentComponentTemperatureLimitsDto

object EquipmentComponentTemperatureLimitsMapper {
    fun dtoToEntity(dto: EquipmentComponentTemperatureLimitsDto): EquipmentComponentTemperatureLimitsEntity {
        val componentId = dto.componentId ?: dto.equipmentComponent?.id
        return EquipmentComponentTemperatureLimitsEntity(
            id = dto.id,
            name = dto.name,
            normal = dto.normal,
            lowRisk = dto.lowRisk,
            mediumRisk = dto.mediumRisk,
            highRisk = dto.highRisk,
            imminentHighRisk = dto.imminentHighRisk,
            componentId = componentId
        )
    }
}
