package com.tech.thermography.android.ui.thermal_anomaly

import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import java.time.LocalDate
import java.util.UUID

sealed class ThermalAnomalyEvent {
    data class PlantSelected(val plant: PlantEntity) : ThermalAnomalyEvent()
    data class EquipmentSelected(val equipment: EquipmentEntity) : ThermalAnomalyEvent()
    data class UpdateRecordName(val value: String) : ThermalAnomalyEvent()
    data class UpdateServiceOrder(val value: String) : ThermalAnomalyEvent()
    data class UpdateAnalysis(val value: String) : ThermalAnomalyEvent()
    data class UpdateCondition(val value: ConditionType) : ThermalAnomalyEvent()
    data class UpdateDeadline(val value: Long?) : ThermalAnomalyEvent()
    data class UpdateRecommendations(val value: String) : ThermalAnomalyEvent()
    data class InspectionRecordSelected(val record: InspectionRecordEntity) : ThermalAnomalyEvent()
    object Save : ThermalAnomalyEvent()
    object Cancel : ThermalAnomalyEvent()
}
