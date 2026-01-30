package com.tech.thermography.android.ui.thermal_anomaly

import android.net.Uri
import com.tech.thermography.android.data.local.entity.EquipmentComponentEntity
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import java.util.UUID

data class ThermalAnomalyUiState(
    // Dropdown data
    val availablePlants: List<PlantEntity> = emptyList(),
    val filteredEquipments: List<EquipmentEntity> = emptyList(),
    val filteredInspectionRecords: List<InspectionRecordEntity> = emptyList(),
    val availableComponents: List<EquipmentComponentEntity> = emptyList(),

    // Selected values
    val selectedPlant: PlantEntity? = null,
    val selectedEquipment: EquipmentEntity? = null,
    val selectedInspectionRecord: InspectionRecordEntity? = null,
    val selectedComponent: EquipmentComponentEntity? = null,

    // Form fields
    val equipmentType: String = "",
    val recordName: String = "",
    val serviceOrder: String = "",
    val analysisDescription: String = "",
    val condition: ConditionType = ConditionType.NORMAL,
    val deadlineExecution: Long? = null,
    val nextMonitoring: Long? = null,
    val recommendations: String = "",

    // Thermogram data
    val thermogramId: UUID? = null,
    val thermogram: ThermogramEntity? = null,
    val thermogramRois: List<ROIEntity> = emptyList(),
    val selectedRoi: ROIEntity? = null,
    val selectedRefRoi: ROIEntity? = null,
    val thermogramImageUri: Uri? = null,

    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)
