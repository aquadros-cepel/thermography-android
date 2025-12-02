package com.tech.thermography.android.data.remote.dto

import java.time.LocalDate
import java.util.UUID

data class PlantDto(
    val id: UUID,
    val code: String?,
    val name: String?,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val startDate: LocalDate?,
    val companyId: UUID?,
    val businessUnitId: UUID?
)
