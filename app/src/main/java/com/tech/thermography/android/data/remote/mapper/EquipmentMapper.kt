package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.remote.dto.EquipmentDto

object EquipmentMapper {
    fun dtoToEntity(dto: EquipmentDto): EquipmentEntity {
        return EquipmentEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            type = dto.type,
            manufacturer = dto.manufacturer,
            model = dto.model,
            serialNumber = dto.serialNumber,
            voltageClass = dto.voltageClass,
            phaseType = dto.phaseType,
            startDate = dto.startDate,
            latitude = dto.latitude,
            longitude = dto.longitude,
            plantId = dto.plantId,
            groupId = dto.groupId
        )
    }
}
