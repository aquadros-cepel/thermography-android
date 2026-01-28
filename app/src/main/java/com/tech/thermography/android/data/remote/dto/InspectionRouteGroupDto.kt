package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class InspectionRouteGroupDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val description: String?,
    val included: Boolean?,
    val orderIndex: Int?,
    // Make inspectionRoute nullable to reflect JSON where it can be null for child groups
    val inspectionRoute: InspectionRouteDto?,
    val parentGroup: InspectionRouteGroupDto?
)
