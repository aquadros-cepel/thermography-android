package com.tech.thermography.android.flir

import android.content.Context
import android.net.Uri
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid
import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dados extraídos de um termograma FLIR
 */
data class ThermogramMetadata(
    // Dados da câmera
    val cameraModel: String?,
    val cameraLens: String?,
    val imageResolution: String?,

    // Temperaturas principais
    val minTemp: Double?,
    val maxTemp: Double?,
    val avgTemp: Double?,

    // Parâmetros de medição
    val emissivity: Double?,
    val reflectedTemp: Double?,
    val atmosphericTemp: Double?,
    val relativeHumidity: Double?,
    val subjectDistance: Double?,
    val referenceTemperature: Double?,

    // Metadados da imagem
    val createdAt: Instant?,
    val latitude: Double?,
    val longitude: Double?,

    // ROIs (Regiões de Interesse)
    val rois: List<ROIMetadata> = emptyList()
)

/**
 * Dados de uma região de interesse (ROI)
 */
data class ROIMetadata(
    val id: UUID = UUID.randomUUID(),
    val type: String,
    val label: String,
    val maxTemp: Double,
    val minTemp: Double?,
    val avgTemp: Double?
)

/**
 * Serviço responsável por ler metadados de termogramas FLIR
 * Utiliza o FLIR Thermal SDK para extrair informações de imagens térmicas
 */
@Singleton
class FlirThermogramReader @Inject constructor() {

    /**
     * Lê os metadados de um termograma a partir de um URI
     */
    suspend fun readMetadata(uri: Uri, context: Context): Result<ThermogramMetadata> {
        return try {
            try { ThermalSdkAndroid.init(context.applicationContext) } catch (_: Exception) {}
            val metadata = FlirThermogramReaderJava.readMetadata(context, uri)
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Extension function para converter ThermogramMetadata em ThermogramEntity
 */
fun ThermogramMetadata.toEntity(
    id: UUID = UUID.randomUUID(),
    equipmentId: UUID,
    createdById: UUID,
    imagePath: String,
    audioPath: String? = null,
    imageRefPath: String? = null,
    selectedRoiId: UUID? = null
): ThermogramEntity {
    return ThermogramEntity(
        id = id,
        imagePath = imagePath,
        audioPath = audioPath,
        imageRefPath = imageRefPath ?: imagePath,
        minTemp = minTemp,
        avgTemp = avgTemp,
        maxTemp = maxTemp,
        emissivity = emissivity,
        subjectDistance = subjectDistance,
        atmosphericTemp = atmosphericTemp,
        reflectedTemp = reflectedTemp,
        relativeHumidity = relativeHumidity,
        cameraLens = cameraLens,
        cameraModel = cameraModel,
        imageResolution = imageResolution,
        selectedRoiId = selectedRoiId,
        maxTempRoi = rois.firstOrNull()?.maxTemp,
        createdAt = createdAt,
        latitude = latitude,
        longitude = longitude,
        equipmentId = equipmentId,
        createdById = createdById
    )
}

/**
 * Extension function para converter ROIMetadata em ROIEntity
 */
fun ROIMetadata.toEntity(thermogramId: UUID): ROIEntity {
    return ROIEntity(
        id = id,
        type = type,
        label = label,
        maxTemp = maxTemp,
        thermogramId = thermogramId
    )
}
