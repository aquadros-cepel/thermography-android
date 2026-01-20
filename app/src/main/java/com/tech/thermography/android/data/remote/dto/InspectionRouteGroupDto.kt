package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class InspectionRouteGroupDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val description: String?,
    val included: Boolean?,
    val orderIndex: Int?,
    val inspectionRoute: InspectionRouteDto,
    val parentGroup: InspectionRouteGroupDto?
)
