package com.tech.thermography.android.ui.thermogram

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.local.repository.ROIRepository
import com.tech.thermography.android.data.local.repository.ThermogramRepository
import com.tech.thermography.android.flir.FlirThermogramReader
import com.tech.thermography.android.flir.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ThermogramUiState(
    val thermogram: ThermogramEntity? = null,
    val rois: List<ROIEntity> = emptyList(),
    val selectedRoi: ROIEntity? = null,
    val selectedRefRoi: ROIEntity? = null,
    val thermogramImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val mode: ThermogramMode = ThermogramMode.VIEW
)

enum class ThermogramMode {
    VIEW,
    EDIT,
    CREATE
}

sealed class ThermogramEvent {
    data class LoadThermogram(val thermogramId: UUID) : ThermogramEvent()
    data class SelectRoi(val roi: ROIEntity) : ThermogramEvent()
    data class UpdateThermogramImage(val uri: Uri) : ThermogramEvent()
    data class UpdateThermogram(val thermogram: ThermogramEntity) : ThermogramEvent()
    data class SetMode(val mode: ThermogramMode) : ThermogramEvent()
    object ClearError : ThermogramEvent()
}

@HiltViewModel
class ThermogramViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val thermogramRepository: ThermogramRepository,
    private val roiRepository: ROIRepository,
    private val flirReader: FlirThermogramReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThermogramUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: ThermogramEvent) {
        when (event) {
            is ThermogramEvent.LoadThermogram -> loadThermogramWithRois(event.thermogramId)
            is ThermogramEvent.SelectRoi -> selectRoi(event.roi)
            is ThermogramEvent.UpdateThermogramImage -> updateThermogramImage(event.uri)
            is ThermogramEvent.UpdateThermogram -> updateThermogram(event.thermogram)
            is ThermogramEvent.SetMode -> setMode(event.mode)
            ThermogramEvent.ClearError -> clearError()
        }
    }

    private fun loadThermogramWithRois(thermogramId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val thermogram = thermogramRepository.getThermogramById(thermogramId)

                if (thermogram == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Termograma não encontrado"
                        )
                    }
                    return@launch
                }

                // Carrega ROIs do termograma
                roiRepository.getRoisByThermogramId(thermogramId).collect { rois ->
                    val selectedRoi = rois.find { it.id == thermogram.selectedRoiId }
                        ?: rois.firstOrNull()
                    val selectedRef = rois.getOrNull(1)

                    // Cria URI da imagem (assumindo que imagePath é um caminho local)
                    val imageUri = thermogram.imagePath.toUri()

                    _uiState.update {
                        it.copy(
                            thermogram = thermogram,
                            rois = rois,
                            selectedRoi = selectedRoi,
                            selectedRefRoi = selectedRef,
                            thermogramImageUri = imageUri,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erro ao carregar termograma: ${e.message}"
                    )
                }
            }
        }
    }

    private fun selectRoi(roi: ROIEntity) {
        viewModelScope.launch {
            val currentThermogram = _uiState.value.thermogram ?: return@launch

            // Atualiza o selectedRoiId no termograma
            val updatedThermogram = currentThermogram.copy(
                selectedRoiId = roi.id,
                maxTempRoi = roi.maxTemp
            )

            try {
                thermogramRepository.updateThermogram(updatedThermogram)
                _uiState.update {
                    it.copy(
                        thermogram = updatedThermogram,
                        selectedRoi = roi
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Erro ao atualizar ROI: ${e.message}")
                }
            }
        }
    }

    private fun updateThermogramImage(uri: Uri) {
        _uiState.update { it.copy(thermogramImageUri = uri, isLoading = true) }

        // Processar metadados da imagem térmica
        viewModelScope.launch {
            try {
                val result = flirReader.readMetadata(uri, context)
                result.onSuccess { metadata ->
                    val currentThermogram = _uiState.value.thermogram

                    // Se já existe thermogram, atualizar. Senão, criar novo
                    val updatedThermogram = if (currentThermogram != null) {
                        currentThermogram.copy(
                            imagePath = uri.toString(),
                            minTemp = metadata.minTemp,
                            avgTemp = metadata.avgTemp,
                            maxTemp = metadata.maxTemp,
                            emissivity = metadata.emissivity,
                            subjectDistance = metadata.subjectDistance,
                            atmosphericTemp = metadata.atmosphericTemp,
                            reflectedTemp = metadata.reflectedTemp,
                            relativeHumidity = metadata.relativeHumidity,
                            cameraLens = metadata.cameraLens,
                            cameraModel = metadata.cameraModel,
                            imageResolution = metadata.imageResolution
                        )
                    } else {
                        metadata.toEntity(
                            equipmentId = UUID.randomUUID(), // TODO: Get from context
                            createdById = UUID.randomUUID(), // TODO: Get from auth
                            imagePath = uri.toString()
                        )
                    }

                    // Criar/atualizar ROIs
                    val roiEntities = metadata.rois.map { it.toEntity(updatedThermogram.id) }

                    _uiState.update {
                        it.copy(
                            thermogram = updatedThermogram,
                            rois = roiEntities,
                            selectedRoi = roiEntities.firstOrNull(),
                            selectedRefRoi = roiEntities.getOrNull(1),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                result.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Aviso: Não foi possível ler metadados: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erro ao processar imagem: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateThermogram(thermogram: ThermogramEntity) {
        viewModelScope.launch {
            try {
                thermogramRepository.updateThermogram(thermogram)
                _uiState.update { it.copy(thermogram = thermogram) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erro ao atualizar termograma: ${e.message}") }
            }
        }
    }

    private fun setMode(mode: ThermogramMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Função auxiliar para calcular diferença de temperatura
    fun calculateTemperatureDifference(): Double? {
        val thermogram = _uiState.value.thermogram ?: return null
        val selectedRoi = _uiState.value.selectedRoi ?: return null
        val reflectedTemp = thermogram.reflectedTemp ?: return null

        return selectedRoi.maxTemp - reflectedTemp
    }
}
