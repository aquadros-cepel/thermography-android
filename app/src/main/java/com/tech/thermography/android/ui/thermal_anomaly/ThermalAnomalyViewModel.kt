package com.tech.thermography.android.ui.thermal_anomaly

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
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
import com.tech.thermography.android.data.local.repository.EquipmentComponentTemperatureLimitsRepository
import com.tech.thermography.android.data.local.repository.RiskPeriodicityDeadlineRepository
import com.tech.thermography.android.data.local.repository.UserInfoRepository
import com.tech.thermography.android.data.local.repository.ROIRepository
import com.tech.thermography.android.data.local.storage.UserSessionStore
import com.tech.thermography.android.data.local.entity.enumeration.DatetimeUnit
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.RiskPeriodicityDeadlineEntity
import com.tech.thermography.android.data.flir.FlirThermogramReader
import com.tech.thermography.android.data.flir.toEntity
import com.tech.thermography.android.data.local.entity.enumeration.EquipmentType
import com.tech.thermography.android.data.local.repository.ThermogramRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import org.json.JSONObject

@HiltViewModel
class ThermalAnomalyViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val plantRepository: PlantRepository,
    private val equipmentRepository: EquipmentRepository,
    private val equipmentComponentRepository: EquipmentComponentRepository,
    private val recordRepository: ThermographicInspectionRecordRepository,
    private val inspectionRecordRepository: InspectionRecordRepository,
    private val flirReader: FlirThermogramReader,
    private val thermogramRepository: ThermogramRepository,
    private val limitsRepository: EquipmentComponentTemperatureLimitsRepository,
    private val riskRepo: RiskPeriodicityDeadlineRepository,
    private val userInfoRepository: UserInfoRepository,
    private val roiRepository: ROIRepository,
    private val sessionStore: UserSessionStore
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
                viewModelScope.launch {
                    _uiState.value.selectedPlant?.let { p ->
                        val anomalyRecords = recordRepository.getThermographicInspectionRecordsByPlantId(p.id).first()
                        setThermalRecordName(p, event.value, anomalyRecords)
                    }
                    // also apply periodicity/recommendation based on new condition
                    applyRiskPeriodicity(event.value)
                }
            }
            is ThermalAnomalyEvent.UpdateDeadline -> {
                // store epoch millis directly on UI state; no local conversion needed here
                _uiState.update { it.copy(deadlineExecution = event.value) }
            }
            is ThermalAnomalyEvent.UpdateNextMonitoring -> {
                _uiState.update { it.copy(nextMonitoring = event.value) }
            }
            is ThermalAnomalyEvent.UpdateRecommendations -> _uiState.update { it.copy(recommendations = event.value) }
            is ThermalAnomalyEvent.InspectionRecordSelected -> _uiState.update { it.copy(selectedInspectionRecord = event.record) }
            is ThermalAnomalyEvent.ThermographicSelectedById -> {
                viewModelScope.launch {
                    try {
                        val therm = recordRepository.getThermographicInspectionRecordById(event.thermographicId)
                        if (therm != null) {
                            // load related entities
                            val plant = therm.plantId.let { plantRepository.getPlantById(it) }
                            val equipment = therm.equipmentId.let { equipmentRepository.getEquipmentById(it) }
                            val inspectionRecord = therm.routeId?.let { inspectionRecordRepository.getInspectionRecordById(it) }

                            // load thermogram and its ROIs so the image and ROIs are available for editing
                            var loadedThermogram: com.tech.thermography.android.data.local.entity.ThermogramEntity? = null
                            var loadedRois: List<com.tech.thermography.android.data.local.entity.ROIEntity> = emptyList()
                            try {
                                // therm.thermogramId is non-nullable in the entity; attempt load
                                loadedThermogram = thermogramRepository.getThermogramById(therm.thermogramId)
                                if (loadedThermogram != null) {
                                    loadedRois = roiRepository.getRoisByThermogramId(loadedThermogram.id).first()
                                }
                            } catch (e: Exception) {
                                Log.w("ThermAnomVM", "Failed to load thermogram or rois: ${e.message}")
                            }

                            // update UI state fields
                            _uiState.update { state ->
                                state.copy(
                                    selectedPlant = plant ?: state.selectedPlant,
                                    selectedEquipment = equipment ?: state.selectedEquipment,
                                    selectedInspectionRecord = inspectionRecord ?: state.selectedInspectionRecord,
                                    recordName = therm.name,
                                    serviceOrder = therm.serviceOrder ?: "",
                                    analysisDescription = therm.analysisDescription ?: "",
                                    condition = therm.condition,
                                    recommendations = therm.recommendations ?: "",
                                    deadlineExecution = therm.deadlineExecution?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                                    nextMonitoring = therm.nextMonitoring?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                                    isEditing = true,
                                    thermogram = loadedThermogram ?: state.thermogram,
                                    thermogramRois = if (loadedRois.isNotEmpty()) loadedRois else state.thermogramRois,
                                    selectedRoi = loadedRois.firstOrNull { it.id == loadedThermogram?.selectedRoiId } ?: loadedRois.firstOrNull() ?: state.selectedRoi,
                                    selectedRefRoi = loadedRois.getOrNull(1) ?: state.selectedRefRoi,
                                    thermogramImageUri = loadedThermogram?.imagePath?.takeIf { it.isNotBlank() }?.toUri() ?: state.thermogramImageUri
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ThermAnomVM", "Failed to load thermographic by id: ${e.message}")
                    }
                }
            }

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
            // If we're not editing an existing record and the recordName is empty, generate a CAT_xxx_<plantCode> name
            val currentState = _uiState.value
            if (!currentState.isEditing && currentState.recordName.isBlank()) {
                val n = anomalyRecords.size
                val numberPart = String.format(Locale.US, "%03d", n + 1)
                val codePart = plant.code ?: plant.name ?: plant.id.toString()
                val generated = "CAT_${numberPart}_$codePart"
                _uiState.update { it.copy(recordName = generated) }
            } else {
                // fallback for existing logic based on condition (keeps previous behavior)
                setThermalRecordName(plant, condition, anomalyRecords)
            }
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

    private fun getEquipmentComponentCode(equipmentType: EquipmentType): String {
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

        // If we're editing, require a non-empty recordName (do not modify it)
        if (state.isEditing && state.recordName.isBlank()) {
            _uiState.update { it.copy(error = "Por favor, preencha o nome do relatório para edição") }
            return
        }

        // We'll compute/generate finalRecordName inside a coroutine (suspend calls must be inside coroutine)
        var finalRecordName = state.recordName

        viewModelScope.launch {
            try {
                // Re-evaluate state inside coroutine (to get any updates that happened since caller)
                val current = _uiState.value
                val plant = current.selectedPlant ?: state.selectedPlant

                // If recordName is blank and not editing, generate the CAT_###_<plantCode> using repository (suspend)
                if (!current.isEditing && finalRecordName.isBlank()) {
                    // At this point we validated earlier that a plant is selected, so plant is non-null
                    val existing = recordRepository.getThermographicInspectionRecordsByPlantId(plant.id).first()
                    val n = existing.size
                    val numberPart = String.format(Locale.US, "%03d", n + 1)
                    val codePart = plant.code ?: plant.name ?: plant.id.toString()
                    finalRecordName = "CAT_${numberPart}_$codePart"
                    // update UI so user sees generated name
                    _uiState.update { it.copy(recordName = finalRecordName) }
                }

                _uiState.update { it.copy(isLoading = true, error = null) }
                val conditionType = current.condition

                // Ensure we have a valid user to reference in FK columns
                val createdById = resolveCurrentUserId()

                // If a thermogram is present in the UI state, persist it first and use its id as thermogramId.
                // If none present, create a minimal placeholder thermogram so FK constraints are satisfied.
                val thermogramState = current.thermogram
                val thermogramIdToUse: UUID = if (thermogramState != null) {
                    val toInsert = thermogramState.copy(createdById = createdById)
                    // If thermogram already exists in DB, perform update to preserve/replace imagePath; otherwise insert
                    val existingTherm = thermogramRepository.getThermogramById(toInsert.id)
                    if (existingTherm != null) {
                        thermogramRepository.updateThermogram(toInsert)
                    } else {
                        thermogramRepository.insertThermogram(toInsert)
                    }
                    toInsert.id
                } else {
                    // create placeholder thermogram using current.selectedEquipment (up-to-date)
                    val equipmentIdForPlaceholder = requireNotNull(current.selectedEquipment).id
                    val placeholder = com.tech.thermography.android.data.local.entity.ThermogramEntity(
                        id = UUID.randomUUID(),
                        imagePath = "",
                        audioPath = null,
                        imageRefPath = "",
                        minTemp = null,
                        avgTemp = null,
                        maxTemp = null,
                        emissivity = null,
                        subjectDistance = null,
                        atmosphericTemp = null,
                        reflectedTemp = null,
                        relativeHumidity = null,
                        cameraLens = null,
                        cameraModel = null,
                        imageResolution = null,
                        selectedRoiId = null,
                        maxTempRoi = null,
                        createdAt = Instant.now(),
                        latitude = null,
                        longitude = null,
                        equipmentId = equipmentIdForPlaceholder,
                        createdById = createdById
                    )
                    thermogramRepository.insertThermogram(placeholder)
                    placeholder.id
                }

                // compute deltaT from selected ROIs / thermogram
                val deltaT = calculateTemperatureDifference() ?: 0.0

                // Insert related ROIs entities (ensure thermogram exists first)
                val roiEntities = current.thermogramRois.map { roi ->
                    // ROIEntity fields: id, type, label, maxTemp, thermogramId
                    roi.copy(
                        thermogramId = thermogramIdToUse
                    )
                }
                // insert each ROI sequentially to preserve order and avoid concurrency FK issues
                for (r in roiEntities) {
                    roiRepository.insertROI(r)
                }

                // Now create and persist the ThermographicInspectionRecord (after thermogram and ROIs)
                val record = ThermographicInspectionRecordEntity(
                    id = UUID.randomUUID(),
                    name = finalRecordName,
                    type = ThermographicInspectionRecordType.ANOMALY_INITIAL,
                    serviceOrder = state.serviceOrder.ifBlank { null },
                    createdAt = Instant.now(),
                    analysisDescription = state.analysisDescription.ifBlank { null },
                    condition = conditionType,
                    deltaT = deltaT,
                    periodicity = null,
                    deadlineExecution = state.deadlineExecution?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
                    nextMonitoring = state.nextMonitoring?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
                    recommendations = state.recommendations.ifBlank { null },
                    finished = false,
                    finishedAt = null,
                    plantId = requireNotNull(state.selectedPlant).id,
                    routeId = state.selectedInspectionRecord?.id,
                    equipmentId = requireNotNull(state.selectedEquipment).id,
                    componentId = state.selectedComponent?.id,
                    createdById = createdById,
                    finishedById = createdById,
                    thermogramId = thermogramIdToUse,
                    thermogramRefId = null
                )

                // persist the record
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

    @Suppress("unused")
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
                    val currentUserId = resolveCurrentUserId()
                    val thermogramEntity = metadata.toEntity(
                        equipmentId = _uiState.value.selectedEquipment?.id ?: UUID.randomUUID(),
                        createdById = currentUserId,
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
            val limits = limitsRepository.getEquipmentComponentTemperatureLimitsByComponentId(component.id)

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
                id = UUID.randomUUID(), name = "LOW_RISK", deadline = 6, deadlineUnit = DatetimeUnit.YEAR, periodicity = 6, periodicityUnit = DatetimeUnit.MONTH, recommendations = "Recomenda-se que a intervenção para correção da anomalia seja executada em aproveitamento, na próxima manutenção preventiva periódica do equipamento principal respeitando um prazo máximo de seis anos."
            )
            ConditionType.MEDIUM_RISK -> RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(), name = "MEDIUM_RISK", deadline = 3, deadlineUnit = DatetimeUnit.YEAR, periodicity = 3, periodicityUnit = DatetimeUnit.MONTH, recommendations = "Recomenda-se que a intervenção para correção da anomalia seja executada em aproveitamento, na próxima manutenção preventiva periódica do equipamento principal respeitando um prazo máximo de três anos."
            )
            ConditionType.HIGH_RISK -> RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(), name = "HIGH_RISK", deadline = 3, deadlineUnit = DatetimeUnit.MONTH, periodicity = 15, periodicityUnit = DatetimeUnit.DAY, recommendations = "A manutenção para correção da anomalia deve ser executada em até 90 dias a partir de uma intervenção programada. Caso haja dúvidas quanto ao diagnóstico da criticidade da anomalia térmica o termografista responsável deverá solicitar apoio ao CCT (Comitê corporativo de Termografia) para validação da criticidade."
            )
            ConditionType.IMMINENT_HIGH_RISK -> RiskPeriodicityDeadlineEntity(
                id = UUID.randomUUID(), name = "IMMINENT_HIGH_RISK", deadline = 48, deadlineUnit = DatetimeUnit.HOUR, periodicity = 0, periodicityUnit = null, recommendations = "A gerência deverá validar a criticidade da anomalia térmica ou reclassificá-la, alterando se necessário a criticidade no cadastro do sistema Thermal Energy com as respectivas justificativas."
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
    @Suppress("unused")
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

    // Resolve the current/logged-in user id.
    // Strategy: try to parse JWT from sessionStore.token to get a user id claim (sub/id/userId),
    // otherwise use first UserInfo from DB, otherwise create a default user and return its id.
    private suspend fun resolveCurrentUserId(): UUID {
        // try token -> parse JWT
        try {
            val token = sessionStore.token.first()
            if (!token.isNullOrBlank()) {
                val parts = token.split('.')
                if (parts.size >= 2) {
                    val payload = parts[1]
                    // decode URL-safe base64
                    val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                    val payloadJson = String(decodedBytes)
                    val jo = JSONObject(payloadJson)
                    val candidates = listOf("sub", "user_id", "userId", "id")
                    for (k in candidates) {
                        if (jo.has(k)) {
                            val v = jo.optString(k).takeIf { it.isNotBlank() }
                            if (!v.isNullOrBlank()) {
                                try {
                                    return UUID.fromString(v)
                                } catch (_: Exception) {
                                    // not a UUID string -> ignore
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ThermAnomVM", "Failed to parse token for user id: ${e.message}")
        }

        // fallback: first user in DB
        val users = userInfoRepository.getAllUserInfos().first()
        val first = users.firstOrNull()
        if (first != null) return first.id

        // create a default local user
        val defaultUser = com.tech.thermography.android.data.local.entity.UserInfoEntity(
            id = UUID.randomUUID(),
            position = "Local User",
            phoneNumber = null,
            companyId = null
        )
        userInfoRepository.insertUserInfo(defaultUser)
        return defaultUser.id
    }
}
