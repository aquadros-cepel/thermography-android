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
    val plant: UUID_DTO,
    val route: UUID_DTO?,
    val equipment: UUID_DTO,
    val component: UUID_DTO?,
    val createdBy: UUID_DTO,
    val finishedBy: UUID_DTO,
    val thermogram: UUID_DTO,
    val thermogramRef: UUID_DTO?
)
