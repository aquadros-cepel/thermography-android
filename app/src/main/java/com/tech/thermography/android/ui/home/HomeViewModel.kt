package com.tech.thermography.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.local.repository.EquipmentRepository
import com.tech.thermography.android.data.local.repository.ThermogramRepository
import com.tech.thermography.android.data.local.repository.ThermographicInspectionRecordRepository
import com.tech.thermography.android.data.local.storage.UserSessionStore
import com.tech.thermography.android.flir.AceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentRecordItem(
    val record: ThermographicInspectionRecordEntity,
    val equipment: EquipmentEntity?,
    val thermogram: ThermogramEntity?
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionStore: UserSessionStore,
    private val thermographicRecordRepository: ThermographicInspectionRecordRepository,
    private val equipmentRepository: EquipmentRepository,
    private val thermogramRepository: ThermogramRepository,
    private val aceController: AceController
) : ViewModel() {

    private val _recentRecords = MutableStateFlow<List<RecentRecordItem>>(emptyList())
    val recentRecords: StateFlow<List<RecentRecordItem>> = _recentRecords

    init {
        viewModelScope.launch {
            thermographicRecordRepository.getAllThermographicInspectionRecords()
                .collect { records ->
                    val sorted = records.sortedByDescending { it.createdAt }
                    val items = sorted.map { record ->
                        val equipment = runCatching {
                            equipmentRepository.getEquipmentById(record.equipmentId)
                        }.getOrNull()
                        val thermogram = runCatching {
                            thermogramRepository.getThermogramById(record.thermogramId)
                        }.getOrNull()
                        RecentRecordItem(record, equipment, thermogram)
                    }
                    _recentRecords.value = items
                }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            sessionStore.clearSession()
            onComplete()
        }
    }

    /**
     * Retorna true se uma câmera ACE (USB/direct) estiver conectada.
     * Quando true, o botão Câmera deve abrir ThermogramsCameraScreen.
     * Quando false, deve abrir FlirDiscoveryScreen (câmeras via WiFi).
     */
    fun isAceCameraConnected(): Boolean = aceController.isAceCameraConnected
}
