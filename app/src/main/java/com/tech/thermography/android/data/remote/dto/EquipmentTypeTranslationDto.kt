package com.tech.thermography.android.data.remote.dto

import com.tech.thermography.android.data.local.entity.enumeration.EquipmentType
import java.util.UUID

data class EquipmentTypeTranslationDto(
    val id: UUID,
    val code: EquipmentType,
    val language: String,
    val name: String
)
