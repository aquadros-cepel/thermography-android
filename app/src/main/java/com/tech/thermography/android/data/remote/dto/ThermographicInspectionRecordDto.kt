package com.tech.thermography.android.data.remote.dto

import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import com.tech.thermography.android.data.local.entity.enumeration.ThermographicInspectionRecordType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ThermographicInspectionRecordDto(
    val id: UUID,
    val name: String,
    val type: ThermographicInspectionRecordType,
    val serviceOrder: String?,
    val createdAt: Instant,
    val analysisDescription: String?,
    val condition: ConditionType,
    val deltaT: Double,
    val periodicity: Int?,
    val deadlineExecution: LocalDate?,
    val nextMonitoring: LocalDate?,
    val recommendations: String?,
    val finished: Boolean?,
    val finishedAt: Instant?,
    val plantId: UUID,
    val routeId: UUID?,
    val equipmentId: UUID,
    val componentId: UUID?,
    val createdById: UUID,
    val finishedById: UUID,
    val thermogramId: UUID,
    val thermogramRefId: UUID?
)
