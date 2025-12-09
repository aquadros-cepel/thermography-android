package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.EquipmentTypeTranslationEntity
import com.tech.thermography.android.data.remote.dto.EquipmentTypeTranslationDto

object EquipmentTypeTranslationMapper {
    fun dtoToEntity(dto: EquipmentTypeTranslationDto): EquipmentTypeTranslationEntity {
        return EquipmentTypeTranslationEntity(
            id = dto.id,
            code = dto.code,
            language = dto.language,
            name = dto.name
        )
    }
}
