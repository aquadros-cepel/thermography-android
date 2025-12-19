package com.tech.thermography.android.ui.inspection_report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.repository.InspectionRecordRepository
import com.tech.thermography.android.data.local.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InspectionRecordsViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val inspectionRecordRepository: InspectionRecordRepository
) : ViewModel() {

    private val _selectedPlantId = MutableStateFlow<UUID?>(null)
    val selectedPlantId: StateFlow<UUID?> = _selectedPlantId

    val plants: StateFlow<List<PlantEntity>> = plantRepository.getAllPlants()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val inspectionRecords: StateFlow<List<InspectionRecordEntity>> = inspectionRecordRepository.getAllInspectionRecords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredPlants: StateFlow<List<PlantEntity>> = combine(
        plants,
        _selectedPlantId
    ) { allPlants, selectedId ->
        if (selectedId == null) allPlants else allPlants.filter { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredInspectionRecords: StateFlow<List<InspectionRecordEntity>> = combine(
        inspectionRecords,
        _selectedPlantId
    ) { allRecords, selectedId ->
        if (selectedId == null) allRecords else allRecords.filter { it.plantId == selectedId }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // TODO: Remover após o uso. Correção temporária solicitada para o registro RI_N_S_PAAT_C002_001
        debugResetRecord()
    }

    fun selectPlant(plantId: UUID?) {
        _selectedPlantId.value = plantId
    }

    fun selectPlantFromMap(plantId: UUID) {
        _selectedPlantId.value = plantId
    }

    /**
     * Função utilitária para resetar o status de finished de um registro específico.
     */
    private fun debugResetRecord() {
        viewModelScope.launch {
            try {
                // Obtém a lista atual de registros (snapshot)
                val records = inspectionRecordRepository.getAllInspectionRecords().first()
                val targetCode = "RI_N-S-PAAT_C002_001"
                
                // Encontra o registro pelo código
                val recordToFix = records.find { it.name == targetCode }
                
                if (recordToFix != null && recordToFix.finished == true) {
                    // Atualiza para finished = false
                    val updatedRecord = recordToFix.copy(finished = false)
                    inspectionRecordRepository.insertInspectionRecord(updatedRecord)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
