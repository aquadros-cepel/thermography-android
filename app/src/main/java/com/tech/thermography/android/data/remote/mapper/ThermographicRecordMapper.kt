package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.dao.ThermographicRecordWithRelations
import com.tech.thermography.android.data.local.entity.ThermographicRecord
import com.tech.thermography.android.data.remote.dto.ThermographicRecordCreateDTO
import com.tech.thermography.android.data.local.entity.enumeration.RecordSyncStatus
import com.tech.thermography.android.data.local.entity.enumeration.ThermographicRecordType
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.remote.dto.ROIDto
import com.tech.thermography.android.data.remote.dto.ThermogramDTO
import com.tech.thermography.android.data.remote.dto.IdRefDTO
import com.tech.thermography.android.data.remote.dto.PlantRefDTO

object ThermographicRecordMapper {
    private fun parseType(type: String?): ThermographicRecordType =
        type?.let { ThermographicRecordType.valueOf(it) } ?: ThermographicRecordType.NO_ANOMALY
    private fun parseCondition(condition: String?): ConditionType =
        condition?.let { ConditionType.valueOf(it) } ?: ConditionType.HIGH_RISK
    private fun parseInstant(str: String?): Instant =
        str?.let { Instant.parse(it) } ?: Instant.now()
    private fun parseLocalDate(str: String?): LocalDate? =
        str?.let { LocalDate.parse(it) }

    fun dtoToEntity(dto: ThermographicRecordCreateDTO): ThermographicRecord {
        return ThermographicRecord(
            id = dto.id,
            name = dto.name ?: "",
            type = parseType(dto.type),
            serviceOrder = dto.serviceOrder,
            createdAt = parseInstant(dto.createdAt),
            analysisDescription = dto.analysisDescription,
            condition = parseCondition(dto.condition),
            deltaT = dto.deltaT ?: 0.0,
            periodicity = dto.periodicity,
            deadlineExecution = parseLocalDate(dto.deadlineExecution),
            nextMonitoring = parseLocalDate(dto.nextMonitoring),
            recommendations = dto.recommendations,
            finished = dto.finished,
            finishedAt = null,
            plantId = dto.plant?.id ?: UUID.randomUUID(),
            routeId = dto.route?.id,
            equipmentId = dto.equipment?.id ?: UUID.randomUUID(),
            componentId = dto.component?.id,
            createdById = dto.createdBy?.id ?: UUID.randomUUID(),
            finishedById = dto.finishedBy?.id ?: UUID.randomUUID(),
            thermogramId = dto.thermogram?.id ?: UUID.randomUUID(),
            thermogramRefId = dto.thermogramRef?.id,
            current = dto.current,
            voltage = dto.voltage,
            windSpeed = dto.windSpeed,
            syncStatus = RecordSyncStatus.SYNCED
        )
    }

    fun entityToDto(
        entity: ThermographicRecord,
        plant: PlantEntity?,
        thermogram: ThermogramEntity?,
        thermogramRef: ThermogramEntity?,
        rois: List<ROIDto>?,
        roisRef: List<ROIDto>?
    ): ThermographicRecordCreateDTO {
        return ThermographicRecordCreateDTO(
            id = entity.id,
            name = entity.name,
            type = entity.type.name,
            equipment = IdRefDTO(entity.equipmentId),
            plant = PlantRefDTO(entity.plantId, plant?.name, plant?.code),
            createdAt = entity.createdAt.toString(),
            deadlineExecution = entity.deadlineExecution?.toString(),
            nextMonitoring = entity.nextMonitoring?.toString(),
            condition = entity.condition.name,
            deltaT = entity.deltaT,
            periodicity = entity.periodicity,
            serviceOrder = entity.serviceOrder,
            analysisDescription = entity.analysisDescription,
            recommendations = entity.recommendations,
            finished = entity.finished,
            component = entity.componentId?.let { IdRefDTO(it) },
            thermogram = thermogram?.let {
                ThermogramDTO(
                    id = it.id,
                    imagePath = it.imagePath,
                    audioPath = it.audioPath,
                    imageRefPath = it.imageRefPath,
                    minTemp = it.minTemp,
                    avgTemp = it.avgTemp,
                    maxTemp = it.maxTemp,
                    emissivity = it.emissivity,
                    subjectDistance = it.subjectDistance,
                    atmosphericTemp = it.atmosphericTemp,
                    reflectedTemp = it.reflectedTemp,
                    relativeHumidity = it.relativeHumidity,
                    cameraLens = it.cameraLens,
                    cameraModel = it.cameraModel,
                    imageResolution = it.imageResolution,
                    selectedRoiId = it.selectedRoiId,
                    maxTempRoi = it.maxTempRoi,
                    createdAt = it.createdAt?.toString(),
                    latitude = it.latitude,
                    longitude = it.longitude,
                    rois = rois,
                    phase = it.phase?.name
                )
            },
            thermogramRef = thermogramRef?.let {
                ThermogramDTO(
                    id = it.id,
                    imagePath = it.imagePath,
                    audioPath = it.audioPath,
                    imageRefPath = it.imageRefPath,
                    minTemp = it.minTemp,
                    avgTemp = it.avgTemp,
                    maxTemp = it.maxTemp,
                    emissivity = it.emissivity,
                    subjectDistance = it.subjectDistance,
                    atmosphericTemp = it.atmosphericTemp,
                    reflectedTemp = it.reflectedTemp,
                    relativeHumidity = it.relativeHumidity,
                    cameraLens = it.cameraLens,
                    cameraModel = it.cameraModel,
                    imageResolution = it.imageResolution,
                    selectedRoiId = it.selectedRoiId,
                    maxTempRoi = it.maxTempRoi,
                    createdAt = it.createdAt?.toString(),
                    latitude = it.latitude,
                    longitude = it.longitude,
                    rois = roisRef,
                    phase = it.phase?.name
                )
            },
            route = entity.routeId?.let { IdRefDTO(it) },
            createdBy = IdRefDTO(entity.createdById),
            finishedBy = IdRefDTO(entity.finishedById),
            current = entity.current,
            voltage = entity.voltage,
            windSpeed = entity.windSpeed
        )
    }

    fun entityToDto(rel: ThermographicRecordWithRelations): ThermographicRecordCreateDTO {
        val entity = rel.record
        return entityToDto(
            entity,
            rel.plant,
            rel.thermogram,
            rel.thermogramRef,
            null,
            null
        )
    }
}
