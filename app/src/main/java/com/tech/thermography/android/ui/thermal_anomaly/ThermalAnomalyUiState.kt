package com.tech.thermography.android.ui.thermal_anomaly

import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity

data class ThermalAnomalyUiState(
    // Dropdown data
    val availablePlants: List<PlantEntity> = emptyList(),
    val filteredEquipments: List<EquipmentEntity> = emptyList(),
    val filteredInspectionRecords: List<InspectionRecordEntity> = emptyList(),

    // Selected values
    val selectedPlant: PlantEntity? = null,
    val selectedEquipment: EquipmentEntity? = null,
    val selectedInspectionRecord: InspectionRecordEntity? = null,

    // Form fields
    val equipmentType: String = "",
    val recordName: String = "",
    val serviceOrder: String = "",
    val analysisDescription: String = "",
    val condition: String = "Baixo Risco",
    val deadlineExecution: Long? = null,
    val recommendations: String = "",
    

    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)
