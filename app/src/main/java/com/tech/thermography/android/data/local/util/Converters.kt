package com.tech.thermography.android.data.local.util

import androidx.room.TypeConverter
import com.tech.thermography.android.data.local.entity.enum.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(uuid: String?): UUID? = uuid?.let { UUID.fromString(it) }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromEquipmentType(type: EquipmentType?): String? = type?.name

    @TypeConverter
    fun toEquipmentType(value: String?): EquipmentType? = value?.let { EquipmentType.valueOf(it) }

    @TypeConverter
    fun fromPhaseType(type: PhaseType?): String? = type?.name

    @TypeConverter
    fun toPhaseType(value: String?): PhaseType? = value?.let { PhaseType.valueOf(it) }

    @TypeConverter
    fun fromPeriodicity(periodicity: Periodicity?): String? = periodicity?.name

    @TypeConverter
    fun toPeriodicity(value: String?): Periodicity? = value?.let { Periodicity.valueOf(it) }

    @TypeConverter
    fun fromConditionType(type: ConditionType?): String? = type?.name

    @TypeConverter
    fun toConditionType(value: String?): ConditionType? = value?.let { ConditionType.valueOf(it) }

    @TypeConverter
    fun fromDatetimeUnit(unit: DatetimeUnit?): String? = unit?.name

    @TypeConverter
    fun toDatetimeUnit(value: String?): DatetimeUnit? = value?.let { DatetimeUnit.valueOf(it) }

    @TypeConverter
    fun fromThermographicInspectionRecordType(type: ThermographicInspectionRecordType?): String? = type?.name

    @TypeConverter
    fun toThermographicInspectionRecordType(value: String?): ThermographicInspectionRecordType? = 
        value?.let { ThermographicInspectionRecordType.valueOf(it) }

    @TypeConverter
    fun fromEquipmentInspectionStatus(status: EquipmentInspectionStatus?): String? = status?.name

    @TypeConverter
    fun toEquipmentInspectionStatus(value: String?): EquipmentInspectionStatus? = 
        value?.let { EquipmentInspectionStatus.valueOf(it) }
}
