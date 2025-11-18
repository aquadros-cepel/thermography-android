package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*
import com.tech.thermography.android.data.local.entity.enum.DatetimeUnit

@Entity(tableName = "risk_periodicity_deadline")
data class RiskPeriodicityDeadlineEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String?,
    val deadline: Int?,
    val deadlineUnit: DatetimeUnit?,
    val periodicity: Int?,
    val periodicityUnit: DatetimeUnit?,
    val recommendations: String?
)
