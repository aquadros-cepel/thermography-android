package com.tech.thermography.android.data.remote.dto

import com.tech.thermography.android.data.local.entity.enumeration.Periodicity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class InspectionRouteDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val description: String?,
    val maintenancePlan: String?,
    val periodicity: Periodicity?,
    val duration: Int?,
    val expectedStartDate: LocalDate?,
    val createdAt: Instant,
    val plant: PlantDto,
    val createdBy: UserInfoDto
)
