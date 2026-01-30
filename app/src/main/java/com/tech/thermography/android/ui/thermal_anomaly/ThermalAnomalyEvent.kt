package com.tech.thermography.android.ui.thermal_anomaly

import android.net.Uri
import com.tech.thermography.android.data.local.entity.EquipmentComponentEntity
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType

sealed class ThermalAnomalyEvent {
    data class PlantSelected(val plant: PlantEntity) : ThermalAnomalyEvent()
    data class PlantSelectedById(val plantId: java.util.UUID) : ThermalAnomalyEvent()
    data class EquipmentSelected(val equipment: EquipmentEntity) : ThermalAnomalyEvent()
    data class EquipmentSelectedById(val equipmentId: java.util.UUID) : ThermalAnomalyEvent()
    data class ComponentSelected(val component: EquipmentComponentEntity) : ThermalAnomalyEvent()
    data class UpdateRecordName(val value: String) : ThermalAnomalyEvent()
    data class UpdateServiceOrder(val value: String) : ThermalAnomalyEvent()
    data class UpdateAnalysis(val value: String) : ThermalAnomalyEvent()
    data class UpdateCondition(val value: ConditionType) : ThermalAnomalyEvent()
    data class UpdateDeadline(val value: Long?) : ThermalAnomalyEvent()
    data class UpdateNextMonitoring(val value: Long?) : ThermalAnomalyEvent()
    data class UpdateRecommendations(val value: String) : ThermalAnomalyEvent()
    data class InspectionRecordSelected(val record: InspectionRecordEntity) : ThermalAnomalyEvent()
    data class InspectionRecordSelectedById(val recordId: java.util.UUID) : ThermalAnomalyEvent()
    data class ThermographicSelectedById(val thermographicId: java.util.UUID) : ThermalAnomalyEvent()

    // Thermogram events
    data class SelectRoi(val roi: ROIEntity) : ThermalAnomalyEvent()
    data class SelectRefRoi(val roi: ROIEntity) : ThermalAnomalyEvent()
    data class UpdateThermogramImage(val uri: Uri) : ThermalAnomalyEvent()

    object Save : ThermalAnomalyEvent()
    object Cancel : ThermalAnomalyEvent()
}
