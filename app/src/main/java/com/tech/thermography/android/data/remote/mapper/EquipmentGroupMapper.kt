package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.EquipmentGroupEntity
import com.tech.thermography.android.data.remote.dto.EquipmentGroupDto

object EquipmentGroupMapper {
    fun dtoToEntity(dto: EquipmentGroupDto): EquipmentGroupEntity {
        return EquipmentGroupEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            plantId = dto.plantId,
            parentGroupId = dto.parentGroupId
        )
    }
}
