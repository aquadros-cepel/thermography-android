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

    fun selectPlant(plantId: UUID?) {
        _selectedPlantId.value = plantId
    }

    fun selectPlantFromMap(plantId: UUID) {
        _selectedPlantId.value = plantId
    }
}
