package com.tech.thermography.android.ui.thermal_anomaly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import com.tech.thermography.android.data.local.entity.enumeration.ThermographicInspectionRecordType
import com.tech.thermography.android.data.local.repository.InspectionRecordGroupEquipmentRepository
import com.tech.thermography.android.data.local.repository.InspectionRecordGroupRepository
import com.tech.thermography.android.data.local.repository.EquipmentRepository
import com.tech.thermography.android.data.local.repository.PlantRepository
import com.tech.thermography.android.data.local.repository.ThermographicInspectionRecordRepository
import com.tech.thermography.android.data.local.repository.InspectionRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity

@HiltViewModel
class ThermalAnomalyViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val equipmentRepository: EquipmentRepository,
    private val recordRepository: ThermographicInspectionRecordRepository,
    private val inspectionRecordRepository: InspectionRecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThermalAnomalyUiState())
    val uiState: StateFlow<ThermalAnomalyUiState> = _uiState.asStateFlow()

    private val plants: StateFlow<List<PlantEntity>> = plantRepository.getAllPlants()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val equipments: StateFlow<List<EquipmentEntity>> = equipmentRepository.getAllEquipments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Initialize plants in UI state
        viewModelScope.launch {
            plants.collect { allPlants ->
                _uiState.update { it.copy(availablePlants = allPlants) }
            }
        }
    }

    fun onEvent(event: ThermalAnomalyEvent) {
        when (event) {
            is ThermalAnomalyEvent.PlantSelected -> handlePlantSelected(event.plant)
            is ThermalAnomalyEvent.EquipmentSelected -> handleEquipmentSelected(event.equipment)
            is ThermalAnomalyEvent.UpdateRecordName -> _uiState.update { it.copy(recordName = event.value) }
            is ThermalAnomalyEvent.UpdateServiceOrder -> _uiState.update { it.copy(serviceOrder = event.value) }
            is ThermalAnomalyEvent.UpdateAnalysis -> _uiState.update { it.copy(analysisDescription = event.value) }
            is ThermalAnomalyEvent.UpdateCondition -> {
                _uiState.update { it.copy(condition = event.value) }
                // Atualiza o nome do relatório ao mudar a condição
                val plant = _uiState.value.selectedPlant
                viewModelScope.launch {
                    if (plant != null) {
                        val anomalyRecords = recordRepository.getThermographicInspectionRecordsByPlantId(plant.id).first()
                        setThermalRecordName(plant, event.value, anomalyRecords)
                    }
                }
            }
            is ThermalAnomalyEvent.UpdateDeadline -> {
                val deadlineDate = if (event.value != null) {
                    Instant.ofEpochMilli(event.value).atZone(ZoneId.systemDefault()).toLocalDate()
                } else null
                _uiState.update { it.copy(deadlineExecution = event.value) }
            }
            is ThermalAnomalyEvent.UpdateRecommendations -> _uiState.update { it.copy(recommendations = event.value) }
            ThermalAnomalyEvent.Save -> saveRecord()
            ThermalAnomalyEvent.Cancel -> resetForm()
            is ThermalAnomalyEvent.InspectionRecordSelected -> _uiState.update { it.copy(selectedInspectionRecord = event.record) }
        }
    }

    private fun handlePlantSelected(plant: PlantEntity) {
        _uiState.update { it.copy(
            selectedPlant = plant,
            selectedEquipment = null,
            filteredInspectionRecords = emptyList(),
            equipmentType = ""
        ) }
        // Load equipments for this plant
        viewModelScope.launch {
            equipmentRepository.getEquipmentsByPlantId(plant.id).collect { equipmentList ->
                _uiState.update { it.copy(filteredEquipments = equipmentList) }
            }
            // Após carregar equipamentos, busca registros de anomalia para a planta e atualiza o nome do relatório
            val anomalyRecords = recordRepository.getThermographicInspectionRecordsByPlantId(plant.id).first()
            val condition = _uiState.value.condition
            setThermalRecordName(plant, condition, anomalyRecords)
        }
    }

    private fun handleEquipmentSelected(equipment: EquipmentEntity) {
        _uiState.update { it.copy(
            selectedEquipment = equipment,
            equipmentType = generateEquipmentTypeString(equipment)
        ) }

        viewModelScope.launch {
            val inspectionRecords = inspectionRecordRepository.getInspectionRecordsByEquipmentId(equipment.id)
            _uiState.update { it.copy(filteredInspectionRecords = inspectionRecords) }
        }
    }

    private fun generateEquipmentTypeString(equipment: EquipmentEntity): String {
        val code = equipment.code?.split("-")?.lastOrNull()?.trim().orEmpty()
        val name = equipment.name
        return if (code.isNotBlank() && name.isNotBlank()) {
            "$code ($name)"
        } else if (name.isNotBlank()) {
            name
        } else {
            code
        }
    }

    private fun saveRecord() {
        val state = _uiState.value

        if (state.selectedPlant == null || state.selectedEquipment == null) {
            _uiState.update { it.copy(error = "Por favor, selecione Instalação e Equipamento") }
            return
        }

        if (state.recordName.isBlank()) {
            _uiState.update { it.copy(error = "Por favor, preencha o nome do relatório") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val conditionType = state.condition

                val deadlineDate = if (state.deadlineExecution != null) {
                    Instant.ofEpochMilli(state.deadlineExecution).atZone(ZoneId.systemDefault()).toLocalDate()
                } else null

                val record = ThermographicInspectionRecordEntity(
                    id = UUID.randomUUID(),
                    name = state.recordName,
                    type = ThermographicInspectionRecordType.ANOMALY_INITIAL,
                    serviceOrder = state.serviceOrder.ifBlank { null },
                    createdAt = Instant.now(),
                    analysisDescription = state.analysisDescription.ifBlank { null },
                    condition = conditionType,
                    deltaT = 0.0, // Will be calculated later
                    periodicity = null,
                    deadlineExecution = deadlineDate,
                    nextMonitoring = null,
                    recommendations = state.recommendations.ifBlank { null },
                    finished = false,
                    finishedAt = null,
                    plantId = state.selectedPlant!!.id,
                    routeId = null,
                    equipmentId = state.selectedEquipment!!.id,
                    componentId = null,
                    createdById = UUID.randomUUID(), // Will be set by the app context
                    finishedById = UUID.randomUUID(),
                    thermogramId = UUID.randomUUID(), // Will be linked to thermogram
                    thermogramRefId = null
                )

                recordRepository.insertThermographicInspectionRecord(record)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erro ao salvar registro"
                    )
                }
            }
        }
    }

    private fun resetForm() {
        _uiState.update {
            ThermalAnomalyUiState(
                availablePlants = it.availablePlants,
                filteredEquipments = emptyList()
            )
        }
    }

    suspend fun getInspectionRecords(equipmentId: UUID): List<InspectionRecordEntity> {
        return inspectionRecordRepository.getInspectionRecordsByEquipmentId(equipmentId)
    }

    fun setThermalRecordName(powerStation: PlantEntity?, condition: ConditionType, anomalyRecords: List<ThermographicInspectionRecordEntity>) {
        if (powerStation != null) {
            val lastName = powerStation.code?.split("-")?.lastOrNull() ?: ""
            if (condition == ConditionType.NORMAL) {
                val filteredRecords = anomalyRecords.filter {
                    it.plantId == powerStation.id &&
                    it.condition == ConditionType.NORMAL
                }
                val newName = if (filteredRecords.isNotEmpty()) {
                    "THERMAL_RECORD_${filteredRecords.size + 1}_$lastName"
                } else {
                    "THERMAL_RECORD_1_$lastName"
                }
                _uiState.update { it.copy(recordName = newName) }
            }
        }
    }
}
