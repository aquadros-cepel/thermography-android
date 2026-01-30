package com.tech.thermography.android.ui.thermal_anomaly

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import com.tech.thermography.android.data.local.entity.enumeration.ThermographicInspectionRecordType
import com.tech.thermography.android.data.local.repository.EquipmentComponentRepository
import com.tech.thermography.android.data.local.repository.EquipmentRepository
import com.tech.thermography.android.data.local.repository.PlantRepository
import com.tech.thermography.android.data.local.repository.ThermographicInspectionRecordRepository
import com.tech.thermography.android.data.local.repository.InspectionRecordRepository
import com.tech.thermography.android.data.local.repository.ROIRepository
import com.tech.thermography.android.data.local.repository.ThermogramRepository
import com.tech.thermography.android.data.local.entity.enumeration.EquipmentType
import com.tech.thermography.android.data.flir.FlirThermogramReader
import com.tech.thermography.android.data.flir.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.RiskPeriodicityDeadlineEntity
import com.tech.thermography.android.data.local.repository.EquipmentComponentTemperatureLimitsRepository
import com.tech.thermography.android.data.local.repository.RiskPeriodicityDeadlineRepository
import com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit
import java.time.temporal.ChronoUnit

@HiltViewModel
class ThermalAnomalyViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val plantRepository: PlantRepository,
    private val equipmentRepository: EquipmentRepository,
    private val equipmentComponentRepository: EquipmentComponentRepository,
    private val recordRepository: ThermographicInspectionRecordRepository,
    private val inspectionRecordRepository: InspectionRecordRepository,
    private val thermogramRepository: ThermogramRepository,
    private val roiRepository: ROIRepository,
    private val flirReader: FlirThermogramReader,
    private val limitsRepository: EquipmentComponentTemperatureLimitsRepository,
    private val riskRepo: RiskPeriodicityDeadlineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThermalAnomalyUiState())
    val uiState: StateFlow<ThermalAnomalyUiState> = _uiState.asStateFlow()

    private val plants: StateFlow<List<PlantEntity>> = plantRepository.getAllPlants()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Initialize plants in UI state
        viewModelScope.launch {
            plants.collect { allPlants ->
                _uiState.update { it.copy(availablePlants = allPlants) }
            }
        }

        // Ensure RiskPeriodicityDeadline mock data exists in DB at startup (if table empty)
        viewModelScope.launch {
            try {
                // Reset and populate mock data so updates in repository are applied immediately
                riskRepo.resetMockData()
            } catch (e: Exception) {
                Log.e("SyncDebug", "Failed to ensure RiskPeriodicityDeadline mock: ${e.message}")
            }
        }
    }

    fun onEvent(event: ThermalAnomalyEvent) {
        when (event) {
            is ThermalAnomalyEvent.PlantSelectedById -> {
                viewModelScope.launch {
                    try {
                        val plant = plantRepository.getPlantById(event.plantId)
                        if (plant != null) handlePlantSelected(plant)
                    } catch (e: Exception) {
                        Log.w("ThermAnomVM", "PlantSelectedById failed: ${e.message}")
                    }
                }
            }
            is ThermalAnomalyEvent.EquipmentSelectedById -> {
                viewModelScope.launch {
                    try {
                        val equipment = equipmentRepository.getEquipmentById(event.equipmentId)
                        if (equipment != null) handleEquipmentSelected(equipment)
                    } catch (e: Exception) {
                        Log.w("ThermAnomVM", "EquipmentSelectedById failed: ${e.message}")
                    }
                }
            }
            is ThermalAnomalyEvent.InspectionRecordSelectedById -> {
                viewModelScope.launch {
                    try {
                        val record = inspectionRecordRepository.getInspectionRecordById(event.recordId)
                        if (record != null) _uiState.update { it.copy(selectedInspectionRecord = record) }
                    } catch (e: Exception) {
                        Log.w("ThermAnomVM", "InspectionRecordSelectedById failed: ${e.message}")
                    }
                }
            }
            is ThermalAnomalyEvent.PlantSelected -> handlePlantSelected(event.plant)
            is ThermalAnomalyEvent.EquipmentSelected -> handleEquipmentSelected(event.equipment)
            is ThermalAnomalyEvent.ComponentSelected -> handleComponentSelected(event.component)
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
                    // also apply periodicity/recommendation based on new condition
                    applyRiskPeriodicity(event.value)
                }
            }
            is ThermalAnomalyEvent.UpdateDeadline -> {
                val deadlineDate = if (event.value != null) {
                    Instant.ofEpochMilli(event.value).atZone(ZoneId.systemDefault()).toLocalDate()
                } else null
                _uiState.update { it.copy(deadlineExecution = event.value) }
            }
            is ThermalAnomalyEvent.UpdateNextMonitoring -> {
                _uiState.update { it.copy(nextMonitoring = event.value) }
            }
            is ThermalAnomalyEvent.UpdateRecommendations -> _uiState.update { it.copy(recommendations = event.value) }
            is ThermalAnomalyEvent.InspectionRecordSelected -> _uiState.update { it.copy(selectedInspectionRecord = event.record) }

            // Thermogram events
            is ThermalAnomalyEvent.SelectRoi -> handleRoiSelected(event.roi)
            is ThermalAnomalyEvent.SelectRefRoi -> handleRefRoiSelected(event.roi)
            is ThermalAnomalyEvent.UpdateThermogramImage -> handleImageSelected(event.uri)

            ThermalAnomalyEvent.Save -> saveRecord()
            ThermalAnomalyEvent.Cancel -> resetForm()
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
            equipmentType = generateEquipmentTypeString(equipment),
            selectedComponent = null
        ) }

        viewModelScope.launch {
            val inspectionRecords = inspectionRecordRepository.getInspectionRecordsByEquipmentId(equipment.id)
            _uiState.update { it.copy(filteredInspectionRecords = inspectionRecords) }
            
            // Filter equipment components based on equipment type
            val componentCode = getEquipmentComponentCode(equipment.type)
            val allComponents = equipmentComponentRepository.getAllEquipmentComponents().first()
            val filteredComponents = if (componentCode.isNotEmpty()) {
                allComponents.filter { it.code == componentCode }
            } else {
                emptyList()
            }
            _uiState.update { it.copy(availableComponents = filteredComponents) }
        }
    }

    private fun getEquipmentComponentCode(equipmentType: com.tech.thermography.android.data.local.entity.enumeration.EquipmentType): String {
        return when (equipmentType) {
            EquipmentType.AUTOTRANSFORMER -> "TF"
            EquipmentType.POWER_TRANSFORMER -> "TF"
            EquipmentType.SHUNT_REACTOR -> "TF"
            EquipmentType.CURRENT_TRANSFORMER -> "TC"
            EquipmentType.POTENTIAL_TRANSFORMER -> "TP"
            EquipmentType.CIRCUIT_BREAKER -> "DJ"
            EquipmentType.DISCONNECT_SWITCH -> "SC"
            EquipmentType.SURGE_ARRESTER -> "PR"
            EquipmentType.CAPACITOR_BANK -> "CA"
            EquipmentType.BATTERY_BANK -> "BT"
            EquipmentType.ELECTRICAL_PANEL -> "PA"
            EquipmentType.PROTECTION_CONTROL_PANEL -> "PA"
            EquipmentType.METAL_CLAD_SWITCHGEAR -> "CB"
            else -> ""
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
                    nextMonitoring = state.nextMonitoring?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
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

    private fun handleRoiSelected(roi: com.tech.thermography.android.data.local.entity.ROIEntity) {
        _uiState.update { state ->
            // update selectedRoi
            val updatedThermogram = state.thermogram?.copy(
                selectedRoiId = roi.id,
                maxTempRoi = roi.maxTemp
            )
            state.copy(
                selectedRoi = roi,
                thermogram = updatedThermogram
            )
        }
    }

    private fun handleRefRoiSelected(roi: com.tech.thermography.android.data.local.entity.ROIEntity) {
        _uiState.update { state ->
            state.copy(
                selectedRefRoi = roi,
            )
        }
    }

    private fun handleImageSelected(uri: Uri) {
        _uiState.update { it.copy(thermogramImageUri = uri) }

        // Processar metadados da imagem térmica em background
        viewModelScope.launch {
            try {
                val result = flirReader.readMetadata(uri, context)
                result.onSuccess { metadata ->
                    // Criar thermogram entity
                    val thermogramEntity = metadata.toEntity(
                        equipmentId = _uiState.value.selectedEquipment?.id ?: UUID.randomUUID(),
                        createdById = UUID.randomUUID(), // TODO: Get from auth
                        imagePath = uri.toString()
                    )

                    // Criar ROI entities
                    val roiEntities = metadata.rois.map { it.toEntity(thermogramEntity.id) }

                    // Atualizar UI state: selectedRoi = first, selectedRefRoi = second (se existir)
                    _uiState.update {
                        it.copy(
                            thermogram = thermogramEntity,
                            thermogramRois = roiEntities,
                            selectedRoi = roiEntities.firstOrNull(),
                            selectedRefRoi = roiEntities.getOrNull(1)
                        )
                    }
                }
                result.onFailure { error ->
                    // Se falhar ao ler metadados, ainda mantém a imagem
                    _uiState.update {
                        it.copy(
                            error = "Aviso: Não foi possível ler metadados da imagem: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Erro ao processar imagem: ${e.message}"
                    )
                }
            }
        }
    }

    fun calculateTemperatureDifference(): Double? {
        val thermogram = _uiState.value.thermogram
        val selectedRoi = _uiState.value.selectedRoi
        val selectedRefRoi = _uiState.value.selectedRefRoi
        val reflectedTemp = thermogram?.reflectedTemp

        val objectTemp = thermogram?.maxTempRoi ?: selectedRoi?.maxTemp ?: thermogram?.maxTemp
        val referenceTemp = selectedRefRoi?.maxTemp ?: reflectedTemp ?: return null

        if (objectTemp == null) return null
        return objectTemp - referenceTemp
    }

    private fun getRisk(value: Double, condition: String?): Boolean {
        if (condition.isNullOrBlank()) return false
        val lower = condition.lowercase().trim()
        if (lower == "não aplicável" || lower == "na") return false

        val parts = condition.split(" ")
        for (part in parts) {
            val regex = Regex("(<=|>=|<|>|=|≤|≥)(\\d+(\\.\\d+)?)")
            val match = regex.find(part)
            if (match == null) continue
            val operator = match.groupValues[1]
            val limit = match.groupValues[2].toDoubleOrNull() ?: continue

            when (operator) {
                "<", "<=" , "≤" -> if (!(value < limit || operator == "<=" && value <= limit || operator == "≤" && value <= limit)) return false
                ">", ">=" , "≥" -> if (!(value > limit || operator == ">=" && value >= limit || operator == "≥" && value >= limit)) return false
                "=" -> if (!(value == limit)) return false
            }
        }
        return true
    }

    private fun classifyRisk(value: Double?, limits: com.tech.thermography.android.data.local.entity.EquipmentComponentTemperatureLimitsEntity?): ConditionType {
        if (value == null || limits == null) return ConditionType.NORMAL
        return when {
            getRisk(value, limits.imminentHighRisk) -> ConditionType.IMMINENT_HIGH_RISK
            getRisk(value, limits.highRisk) -> ConditionType.HIGH_RISK
            getRisk(value, limits.mediumRisk) -> ConditionType.MEDIUM_RISK
            getRisk(value, limits.lowRisk) -> ConditionType.LOW_RISK
            getRisk(value, limits.normal) -> ConditionType.NORMAL
            else -> ConditionType.NORMAL
        }
    }

    // Update component selection handler
    private fun handleComponentSelected(component: com.tech.thermography.android.data.local.entity.EquipmentComponentEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedComponent = component) }

            // obtain deltaT
            val deltaT = calculateTemperatureDifference()
            // fetch limits for this component by componentId
            val limits = component.id?.let { limitsRepository.getEquipmentComponentTemperatureLimitsByComponentId(it) }

            val newCondition = classifyRisk(deltaT, limits)

            _uiState.update { it.copy(condition = newCondition) }

            // also apply periodicity/recommendation based on new condition
            applyRiskPeriodicity(newCondition)

            // no need to set error flag; UI uses condition to style the dropdown
        }
    }

    private suspend fun getPeriodicityForCondition(condition: ConditionType): RiskPeriodicityDeadlineEntity? {
        val all = riskRepo.getAllOrMock()
        fun normalize(s: String?): String = s?.lowercase()?.replace("[^a-z0-9]".toRegex(), "") ?: ""
        val target = normalize(condition.name)
        val match = all.find { normalize(it.name) == target }
        if (match != null) return match

        // Fallback: build inline defaults matching the JSON you provided
        return when (condition) {
            ConditionType.NORMAL -> RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(), name = "NORMAL", deadline = -1, deadlineUnit = null, periodicity = -1, periodicityUnit = null, recommendations = "Encerrar Monitoramento"
            )
            ConditionType.LOW_RISK -> RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(), name = "LOW_RISK", deadline = 6, deadlineUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.YEAR, periodicity = 6, periodicityUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.MONTH, recommendations = "Recomenda-se que a intervenção para correção da anomalia seja executada em aproveitamento, na próxima manutenção preventiva periódica do equipamento principal respeitando um prazo máximo de seis anos."
            )
            ConditionType.MEDIUM_RISK -> RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(), name = "MEDIUM_RISK", deadline = 3, deadlineUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.YEAR, periodicity = 3, periodicityUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.MONTH, recommendations = "Recomenda-se que a intervenção para correção da anomalia seja executada em aproveitamento, na próxima manutenção preventiva periódica do equipamento principal respeitando um prazo máximo de três anos."
            )
            ConditionType.HIGH_RISK -> RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(), name = "HIGH_RISK", deadline = 3, deadlineUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.MONTH, periodicity = 15, periodicityUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.DAY, recommendations = "A manutenção para correção da anomalia deve ser executada em até 90 dias a partir de uma intervenção programada. Caso haja dúvidas quanto ao diagnóstico da criticidade da anomalia térmica o termografista responsável deverá solicitar apoio ao CCT (Comitê corporativo de Termografia) para validação da criticidade."
            )
            ConditionType.IMMINENT_HIGH_RISK -> RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(), name = "IMMINENT_HIGH_RISK", deadline = 48, deadlineUnit = com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit.HOUR, periodicity = 0, periodicityUnit = null, recommendations = "A gerência deverá validar a criticidade da anomalia térmica ou reclassificá-la, alterando se necessário a criticidade no cadastro do sistema Thermal Energy com as respectivas justificativas."
            )
        }
    }

    private suspend fun applyRiskPeriodicity(condition: ConditionType) {
        try {
            val match = getPeriodicityForCondition(condition)
            Log.d("SyncDebug", "applyRiskPeriodicity: resolved match=${match?.name}")
            if (match != null) {
                val rec = match.recommendations ?: ""
                val deadlineMillis: Long? = if (match.deadline == null || match.deadline < 0 || match.deadlineUnit == null) {
                    null
                } else {
                    val nowZ = ZonedDateTime.now(ZoneId.systemDefault())
                    val amount = match.deadline.toLong()
                    val unit = when (match.deadlineUnit) {
                        DatetimeUnit.HOUR -> ChronoUnit.HOURS
                        DatetimeUnit.DAY -> ChronoUnit.DAYS
                        DatetimeUnit.WEEK -> ChronoUnit.WEEKS
                        DatetimeUnit.MONTH -> ChronoUnit.MONTHS
                        DatetimeUnit.YEAR -> ChronoUnit.YEARS
                    }
                    nowZ.plus(amount, unit).toInstant().toEpochMilli()
                }
                // compute nextMonitoring from periodicity if present
                val nextMonitoringMillis: Long? = if (match.periodicity == null || match.periodicity < 0 || match.periodicityUnit == null) {
                    null
                } else {
                    val nowZ = ZonedDateTime.now(ZoneId.systemDefault())
                    val amount = match.periodicity.toLong()
                    val unit = when (match.periodicityUnit) {
                        DatetimeUnit.HOUR -> ChronoUnit.HOURS
                        DatetimeUnit.DAY -> ChronoUnit.DAYS
                        DatetimeUnit.WEEK -> ChronoUnit.WEEKS
                        DatetimeUnit.MONTH -> ChronoUnit.MONTHS
                        DatetimeUnit.YEAR -> ChronoUnit.YEARS
                        else -> ChronoUnit.DAYS
                    }
                    nowZ.plus(amount, unit).toInstant().toEpochMilli()
                }

                Log.d("SyncDebug", "applyRiskPeriodicity: setting rec='${rec.take(80)}' deadlineMillis=$deadlineMillis")
                _uiState.update { it.copy(recommendations = rec, deadlineExecution = deadlineMillis, nextMonitoring = nextMonitoringMillis) }
            } else {
                Log.d("SyncDebug", "applyRiskPeriodicity: no periodicity available for condition=${condition.name}")
                _uiState.update { it.copy(recommendations = "", deadlineExecution = null) }
            }
        } catch (e: Exception) {
            // on error, don't crash; set error message
            Log.e("SyncDebug", "applyRiskPeriodicity error: ${e.message}")
            _uiState.update { it.copy(error = "Erro ao carregar periodicidade: ${e.message}") }
        }
    }

    /**
     * Helper to preselect plant, equipment and inspectionRecord by their IDs when navigating from other screens.
     */
    fun selectInitialIds(plantId: UUID?, equipmentId: UUID?, inspectionRecordId: UUID?) {
        viewModelScope.launch {
            try {
                if (plantId != null) {
                    val plant = plantRepository.getPlantById(plantId)
                    if (plant != null) onEvent(ThermalAnomalyEvent.PlantSelected(plant))
                }

                if (equipmentId != null) {
                    val equipment = equipmentRepository.getEquipmentById(equipmentId)
                    if (equipment != null) onEvent(ThermalAnomalyEvent.EquipmentSelected(equipment))
                }

                if (inspectionRecordId != null) {
                    // try to find the inspection record in available filteredInspectionRecords or repository
                    val possible = inspectionRecordRepository.getInspectionRecordById(inspectionRecordId)
                    if (possible != null) onEvent(ThermalAnomalyEvent.InspectionRecordSelected(possible))
                }
            } catch (ex: Exception) {
                Log.w("ThermAnomVM", "Failed to preselect IDs: ${ex.message}")
            }
        }
    }
}
