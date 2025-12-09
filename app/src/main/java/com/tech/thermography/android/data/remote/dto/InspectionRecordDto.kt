package com.tech.thermography.android.data.remote.dto

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class InspectionRecordDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val description: String?,
    val maintenanceDocument: String?,
    val createdAt: Instant,
    val expectedStartDate: LocalDate,
    val expectedEndDate: LocalDate,
    val started: Boolean?,
    val startedAt: Instant?,
    val finished: Boolean?,
    val finishedAt: Instant?,
    val plantId: UUID,
    val inspectionRouteId: UUID?,
    val createdById: UUID,
    val startedById: UUID?,
    val finishedById: UUID?
)
