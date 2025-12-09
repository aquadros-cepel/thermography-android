package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class EquipmentGroupDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val description: String?,
    val plantId: UUID?,
    val parentGroupId: UUID?
)
