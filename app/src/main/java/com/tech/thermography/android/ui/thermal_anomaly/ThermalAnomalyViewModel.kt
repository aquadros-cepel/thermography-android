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
import com.tech.thermography.android.flir.FlirThermogramReader
import com.tech.thermography.android.flir.toEntity
import com.tech.thermography.android.data.local.entity.enumeration.EquipmentType
import com.tech.thermography.android.data.local.entity.enumeration.RecordSyncStatus
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
            is ThermalAnomalyEvent.PlantSelected -> handlePlantSelected(event.plant).also { _uiState.update { it.copy(isDirty = true) } }
            is ThermalAnomalyEvent.EquipmentSelected -> handleEquipmentSelected(event.equipment).also { _uiState.update { it.copy(isDirty = true) } }
            is ThermalAnomalyEvent.ComponentSelected -> handleComponentSelected(event.component).also { _uiState.update { it.copy(isDirty = true) } }
            is ThermalAnomalyEvent.UpdateRecordName -> _uiState.update { it.copy(recordName = event.value, isDirty = true) }
            is ThermalAnomalyEvent.UpdateServiceOrder -> _uiState.update { it.copy(serviceOrder = event.value, isDirty = true) }
            is ThermalAnomalyEvent.UpdateAnalysis -> _uiState.update { it.copy(analysisDescription = event.value, isDirty = true) }
            is ThermalAnomalyEvent.UpdateCondition -> {
                _uiState.update { it.copy(condition = event.value, isDirty = true) }
                updateAnalysisAndRecommendations()
                viewModelScope.launch {
                    _uiState.value.selectedPlant?.let { p ->
                        val anomalyRecords = recordRepository.getThermographicInspectionRecordsByPlantId(p.id).first()
                        setThermalRecordName(p, event.value, anomalyRecords)
                    }
                    applyRiskPeriodicity(event.value)
                }
            }
            is ThermalAnomalyEvent.UpdateDeadline -> {
                _uiState.update { it.copy(deadlineExecution = event.value, isDirty = true) }
            }
            is ThermalAnomalyEvent.UpdateNextMonitoring -> {
                _uiState.update { it.copy(nextMonitoring = event.value, isDirty = true) }
            }
            is ThermalAnomalyEvent.UpdateRecommendations -> _uiState.update { it.copy(recommendations = event.value, isDirty = true) }
            is ThermalAnomalyEvent.InspectionRecordSelected -> _uiState.update { it.copy(selectedInspectionRecord = event.record, isDirty = true) }
            is ThermalAnomalyEvent.ThermographicSelectedById -> {
                viewModelScope.launch {
                    try {
                        val therm = recordRepository.getThermographicInspectionRecordById(event.thermographicId)
                        if (therm != null) {
                            val plant = therm.plantId.let { plantRepository.getPlantById(it) }
                            val equipment = therm.equipmentId.let { equipmentRepository.getEquipmentById(it) }
                            val inspectionRecord = therm.routeId?.let { inspectionRecordRepository.getInspectionRecordById(it) }

                            var loadedThermogram: com.tech.thermography.android.data.local.entity.ThermogramEntity? = null
                            var loadedRois: List<com.tech.thermography.android.data.local.entity.ROIEntity> = emptyList()
                            var loadedThermogramRef: com.tech.thermography.android.data.local.entity.ThermogramEntity? = null
                            var loadedRoisRef: List<com.tech.thermography.android.data.local.entity.ROIEntity> = emptyList()

                            try {
                                loadedThermogram = thermogramRepository.getThermogramById(therm.thermogramId)
                                if (loadedThermogram != null) {
                                    loadedRois = roiRepository.getRoisByThermogramId(loadedThermogram.id).first()
                                }
                                
                                therm.thermogramRefId?.let { refId ->
                                    loadedThermogramRef = thermogramRepository.getThermogramById(refId)
                                    if (loadedThermogramRef != null) {
                                        loadedRoisRef = roiRepository.getRoisByThermogramId(loadedThermogramRef!!.id).first()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("ThermAnomVM", "Failed to load thermogram or rois: ${e.message}")
                            }

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
                                    thermogramId = therm.id,
                                    thermogram = loadedThermogram ?: state.thermogram,
                                    thermogramRois = loadedRois.ifEmpty { state.thermogramRois },
                                    selectedRoi = loadedRois.find { it.id == loadedThermogram?.selectedRoiId } ?: loadedRois.firstOrNull(),
                                    
                                    thermogramRef = loadedThermogramRef ?: state.thermogramRef,
                                    thermogramRefRois = loadedRoisRef.ifEmpty { state.thermogramRefRois },
                                    selectedRefRoi = loadedRoisRef.find { it.id == loadedThermogramRef?.selectedRoiId } ?: loadedRoisRef.firstOrNull(),
                                    
                                    thermogramImageUri = loadedThermogram?.localImagePath?.takeIf { it.isNotBlank() }?.toUri() ?: state.thermogramImageUri
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ThermAnomVM", "Failed to load thermographic by id: ${e.message}")
                    }
                }
            }

            is ThermalAnomalyEvent.SelectRoi -> handleRoiSelected(event.roi).also { _uiState.update { it.copy(isDirty = true) } }
            is ThermalAnomalyEvent.SelectRefRoi -> handleRefRoiSelected(event.roi).also { _uiState.update { it.copy(isDirty = true) } }
            is ThermalAnomalyEvent.UpdateThermogramImage -> handleImageSelected(event.uri).also { _uiState.update { it.copy(isDirty = true) } }

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
        viewModelScope.launch {
            equipmentRepository.getEquipmentsByPlantId(plant.id).collect { equipmentList ->
                _uiState.update { it.copy(filteredEquipments = equipmentList) }
            }
            val anomalyRecords = recordRepository.getThermographicInspectionRecordsByPlantId(plant.id).first()
            val condition = _uiState.value.condition
            val currentState = _uiState.value
            if (!currentState.isEditing && currentState.recordName.isBlank()) {
                val n = anomalyRecords.size
                val numberPart = String.format(Locale.US, "%03d", n + 1)
                val codePart = plant.code ?: plant.name ?: plant.id.toString()
                val generated = "CAT_${numberPart}_$codePart"
                _uiState.update { it.copy(recordName = generated) }
            } else {
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
            val componentCode = getEquipmentComponentCode(equipment.type)
            val allComponents = equipmentComponentRepository.getAllEquipmentComponents().first()
            val filteredComponents = if (componentCode.isNotEmpty()) {
                allComponents.filter { it.code == componentCode }
            } else {
                emptyList()
            }
            _uiState.update { it.copy(availableComponents = filteredComponents) }
            updateAnalysisAndRecommendations()
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
        val code = equipment.code?.takeIf { it.isNotBlank() }
        return if (code != null) "$code (${equipment.name})" else equipment.name
    }

    private fun saveRecord() {
        val state = _uiState.value

        if (state.selectedPlant == null || state.selectedEquipment == null) {
            _uiState.update { it.copy(error = "Por favor, selecione Instalação e Equipamento") }
            return
        }

        if (state.isEditing && state.recordName.isBlank()) {
            _uiState.update { it.copy(error = "Por favor, preencha o nome do relatório para edição") }
            return
        }

        var finalRecordName = state.recordName

        viewModelScope.launch {
            try {
                val current = _uiState.value
                val plant = current.selectedPlant ?: state.selectedPlant

                if (!current.isEditing && finalRecordName.isBlank()) {
                    val existing = recordRepository.getThermographicInspectionRecordsByPlantId(plant.id).first()
                    val n = existing.size
                    val numberPart = String.format(Locale.US, "%03d", n + 1)
                    val codePart = plant.code ?: plant.name ?: plant.id.toString()
                    finalRecordName = "CAT_${numberPart}_$codePart"
                    _uiState.update { it.copy(recordName = finalRecordName) }
                }

                _uiState.update { it.copy(isLoading = true, error = null) }
                val conditionType = current.condition
                val createdById = resolveCurrentUserId()

                // 1. Persistir o termograma principal (Monitoramento)
                val thermId: UUID = current.thermogram?.let { t ->
                    val toInsert = t.copy(
                        createdById = createdById,
                        selectedRoiId = current.selectedRoi?.id,
                        maxTempRoi = current.selectedRoi?.maxTemp
                    )
                    if (thermogramRepository.getThermogramById(toInsert.id) != null) {
                        thermogramRepository.updateThermogram(toInsert)
                    } else {
                        thermogramRepository.insertThermogram(toInsert)
                    }
                    
                    // Salvar ROIs associadas ao monitoramento
                    val roiEntities = current.thermogramRois.map { roi -> roi.copy(thermogramId = toInsert.id) }
                    roiRepository.insertROIs(roiEntities)
                    
                    toInsert.id
                } ?: UUID.randomUUID()

                // 2. Persistir o termograma de referência
                val thermRefId: UUID? = current.thermogramRef?.let { tRef ->
                    val toInsertRef = tRef.copy(
                        createdById = createdById,
                        selectedRoiId = current.selectedRefRoi?.id,
                        maxTempRoi = current.selectedRefRoi?.maxTemp
                    )
                    if (thermogramRepository.getThermogramById(toInsertRef.id) != null) {
                        thermogramRepository.updateThermogram(toInsertRef)
                    } else {
                        thermogramRepository.insertThermogram(toInsertRef)
                    }
                    
                    // Salvar ROIs associadas à referência (garantindo o vínculo com o thermogramRef)
                    val roiEntitiesRef = current.thermogramRefRois.map { roi -> roi.copy(thermogramId = toInsertRef.id) }
                    roiRepository.insertROIs(roiEntitiesRef)
                    
                    toInsertRef.id
                }

                val deltaT = calculateTemperatureDifference() ?: 0.0

                val record = ThermographicInspectionRecordEntity(
                    id = if (current.isEditing) current.thermogramId ?: UUID.randomUUID() else UUID.randomUUID(),
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
                    thermogramId = thermId,
                    thermogramRefId = thermRefId,
                    syncStatus = RecordSyncStatus.NEW
                )

                if (current.isEditing) {
                    val existingRecords = recordRepository.getThermographicInspectionRecordsByPlantId(plant.id).first()
                    val original = existingRecords.find { it.name == finalRecordName }
                    val recordToSave = if (original != null) record.copy(id = original.id, syncStatus = RecordSyncStatus.EDITED) else record
                    recordRepository.updateThermographicInspectionRecord(context, recordToSave)
                } else {
                    recordRepository.insertThermographicInspectionRecord(context, record)
                }

                _uiState.update { it.copy(isLoading = false, isSaved = true, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Erro ao salvar registro") }
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
            val updatedThermogramRef = state.thermogramRef?.copy(
                selectedRoiId = roi.id,
                maxTempRoi = roi.maxTemp
            )
            state.copy(
                selectedRefRoi = roi,
                thermogramRef = updatedThermogramRef
            )
        }
    }

    private fun handleImageSelected(uri: Uri) {
        _uiState.update { it.copy(thermogramImageUri = uri) }

        viewModelScope.launch {
            try {
                val result = flirReader.readMetadata(uri, context)
                result.onSuccess { metadata ->
                    val currentUserId = resolveCurrentUserId()
                    val equipmentId = _uiState.value.selectedEquipment?.id ?: UUID.randomUUID()
                    
                    // 1. Criar Thermogram de Monitoramento
                    val thermogramId = UUID.randomUUID()
                    val thermogramMonitoring = metadata.toEntity(
                        id = thermogramId,
                        equipmentId = equipmentId,
                        createdById = currentUserId,
                        localImagePath = uri.toString()
                    )

                    // 2. Criar Thermogram de Referência (Cópia do metadado)
                    val thermogramRefId = UUID.randomUUID()
                    val thermogramReference = metadata.toEntity(
                        id = thermogramRefId,
                        equipmentId = equipmentId,
                        createdById = currentUserId,
                        localImagePath = uri.toString()
                    )

                    // 3. Criar ROIs para Monitoramento (cada ROI tem um novo ID e aponta para thermogramId)
                    val roisMonitoring = metadata.rois.map { 
                        it.toEntity(thermogramId).copy(id = UUID.randomUUID()) 
                    }
                    
                    // 4. Criar ROIs para Referência (cada ROI tem um novo ID e aponta para thermogramRefId)
                    val roisReference = metadata.rois.map { 
                        it.toEntity(thermogramRefId).copy(id = UUID.randomUUID()) 
                    }

                    // SP1 (Sp1) é o primeiro ROI para monitoramento
                    // SP2 (Sp2) é o segundo ROI para referência
                    val selectedRoi = roisMonitoring.firstOrNull()
                    val selectedRefRoi = roisReference.getOrNull(1) ?: roisReference.firstOrNull()

                    _uiState.update {
                        it.copy(
                            thermogram = thermogramMonitoring.copy(
                                selectedRoiId = selectedRoi?.id,
                                maxTempRoi = selectedRoi?.maxTemp
                            ),
                            thermogramRois = roisMonitoring,
                            selectedRoi = selectedRoi,
                            
                            thermogramRef = thermogramReference.copy(
                                selectedRoiId = selectedRefRoi?.id,
                                maxTempRoi = selectedRefRoi?.maxTemp
                            ),
                            thermogramRefRois = roisReference,
                            selectedRefRoi = selectedRefRoi
                        )
                    }
                }
                result.onFailure { error ->
                    _uiState.update {
                        it.copy(error = "Aviso: Não foi possível ler metadados da imagem: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao processar imagem: ${e.message}") }
            }
        }
    }

    private fun updateAnalysisAndRecommendations() {
        val equipment = _uiState.value.selectedEquipment
        val component = _uiState.value.selectedComponent
        val condition = _uiState.value.condition

        if (equipment == null || component == null) {
            _uiState.update { it.copy(recommendations = "", analysisDescription = "") }
            return
        }

        viewModelScope.launch {
            val allDeadlines = riskRepo.getAllRiskPeriodicityDeadlines().first()
            val match = allDeadlines.find { it.name == condition.name }
            if (match != null) {
                val equipmentCode = equipment.code?.takeIf { it.isNotBlank() }
                val equipmentName = equipment.name
                val componentName = component.name
                val equipmentLabel = if (equipmentCode != null) "$equipmentCode ($equipmentName)" else equipmentName
                val analysisDescription = (match.description ?: "")
                    .replace("@COMPONENT", "\"$componentName\"")
                    .replace("@EQUIPMENT", "\"$equipmentLabel\"")
                _uiState.update {
                    it.copy(
                        recommendations = match.recommendations ?: "",
                        analysisDescription = analysisDescription
                    )
                }
            }
        }
    }

    fun calculateTemperatureDifference(): Double? {
        val objectTemp = _uiState.value.selectedRoi?.maxTemp
        val referenceTemp = _uiState.value.selectedRefRoi?.maxTemp ?: _uiState.value.thermogram?.reflectedTemp

        if (objectTemp == null || referenceTemp == null) return null
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

    private fun handleComponentSelected(component: com.tech.thermography.android.data.local.entity.EquipmentComponentEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedComponent = component) }
            updateAnalysisAndRecommendations()
            val deltaT = calculateTemperatureDifference()
            val limits = limitsRepository.getEquipmentComponentTemperatureLimitsByComponentId(component.id)
            val newCondition = classifyRisk(deltaT, limits)
            _uiState.update { it.copy(condition = newCondition) }
            applyRiskPeriodicity(newCondition)
        }
    }

    private suspend fun getPeriodicityForCondition(condition: ConditionType): RiskPeriodicityDeadlineEntity? {
        val all = riskRepo.getAllRiskPeriodicityDeadlines().first()
        fun normalize(s: String?): String = s?.lowercase()?.replace("[^a-z0-9]".toRegex(), "") ?: ""
        val target = normalize(condition.name)
        return all.find { normalize(it.name) == target }
    }

    private suspend fun applyRiskPeriodicity(condition: ConditionType) {
        try {
            val match = getPeriodicityForCondition(condition)
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
                _uiState.update { it.copy(recommendations = rec, deadlineExecution = deadlineMillis, nextMonitoring = nextMonitoringMillis) }
            } else {
                _uiState.update { it.copy(recommendations = "", deadlineExecution = null) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Erro ao carregar periodicidade: ${e.message}") }
        }
    }

    private suspend fun resolveCurrentUserId(): UUID {
        try {
            val token = sessionStore.token.first()
            if (!token.isNullOrBlank()) {
                val parts = token.split('.')
                if (parts.size >= 2) {
                    val payload = parts[1]
                    val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                    val payloadJson = String(decodedBytes)
                    val jo = JSONObject(payloadJson)
                    val candidates = listOf("sub", "user_id", "userId", "id")
                    for (k in candidates) {
                        if (jo.has(k)) {
                            val v = jo.optString(k).takeIf { it.isNotBlank() }
                            if (!v.isNullOrBlank()) {
                                try { return UUID.fromString(v) } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ThermAnomVM", "Failed to parse token for user id: ${e.message}")
        }

        val users = userInfoRepository.getAllUserInfos().first()
        val first = users.firstOrNull()
        if (first != null) return first.id

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
