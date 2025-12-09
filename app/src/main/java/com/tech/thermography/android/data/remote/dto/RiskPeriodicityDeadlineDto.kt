package com.tech.thermography.android.data.remote.dto

import com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit
import java.util.UUID

data class RiskPeriodicityDeadlineDto(
    val id: UUID,
    val name: String,
    val deadline: Int?,
    val deadlineUnit: DatetimeUnit?,
    val periodicity: Int?,
    val periodicityUnit: DatetimeUnit?,
    val recommendations: String?
)
