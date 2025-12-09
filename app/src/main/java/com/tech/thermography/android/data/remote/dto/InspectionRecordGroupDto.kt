package com.tech.thermography.android.data.remote.dto

import java.time.Instant
import java.util.UUID

data class InspectionRecordGroupDto(
    val id: UUID = UUID.randomUUID(),
    val code: String? = null,
    val name: String = "",
    val description: String? = null,
    val orderIndex: Int? = null,
    val finished: Boolean? = null,
    val finishedAt: Instant? = null,
    val inspectionRecordId: UUID_DTO? = null,
    val parentGroupId: UUID_DTO? = null
)
