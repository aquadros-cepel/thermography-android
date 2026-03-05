package com.tech.thermography.android.data.remote.dto

import java.util.UUID

// Referência simples por id
open class IdRefDTO(
    val id: UUID
)

// PlantRefDTO herda IdRefDTO e adiciona name e code
class PlantRefDTO(
    id: UUID,
    val name: String?,
    val code: String?
) : IdRefDTO(id)

// Thermogram DTO
class ThermogramDTO(
    val id: UUID,
    val imagePath: String?,
    val audioPath: String?,
    val imageRefPath: String?,
    val minTemp: Double?,
    val avgTemp: Double?,
    val maxTemp: Double?,
    val emissivity: Double?,
    val subjectDistance: Double?,
    val atmosphericTemp: Double?,
    val reflectedTemp: Double?,
    val relativeHumidity: Double?,
    val cameraLens: String?,
    val cameraModel: String?,
    val imageResolution: String?,
    val selectedRoiId: UUID?,
    val maxTempRoi: Double?,
    val createdAt: String?,
    val latitude: Double?,
    val longitude: Double?,
    val rois: List<ROIDto>? // Alterado de RoiDTO para ROIDto
)

// DTO principal
class ThermographicInspectionRecordCreateDTO(
    val id: UUID,
    val name: String?,
    val type: String?,
    val equipment: IdRefDTO?,
    val plant: PlantRefDTO?,
    val createdAt: String?,
    val deadlineExecution: String?,
    val nextMonitoring: String?,
    val condition: String?,
    val deltaT: Double?,
    val periodicity: Int?,
    val serviceOrder: String?,
    val analysisDescription: String?,
    val recommendations: String?,
    val finished: Boolean?,
    val component: IdRefDTO?,
    val thermogram: ThermogramDTO?,
    val thermogramRef: ThermogramDTO?,
    val route: IdRefDTO?,
    val createdBy: IdRefDTO?,
    val finishedBy: IdRefDTO?
)
