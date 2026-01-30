package com.tech.thermography.android.ui.inspection_report

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEntity
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEquipmentEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.entity.EquipmentEntity
import com.tech.thermography.android.data.local.repository.InspectionRecordGroupEquipmentRepository
import com.tech.thermography.android.data.local.repository.InspectionRecordGroupRepository
import com.tech.thermography.android.data.local.repository.InspectionRecordRepository
import com.tech.thermography.android.data.local.repository.PlantRepository
import com.tech.thermography.android.data.local.repository.EquipmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// Wrapper to combine link entity with loaded equipment entity
data class GroupEquipmentItem(
    val link: InspectionRecordGroupEquipmentEntity,
    val equipment: EquipmentEntity?
)

data class InspectionRecordDetailUiState(
    val record: InspectionRecordEntity? = null,
    val plant: PlantEntity? = null,
    val allGroups: List<InspectionRecordGroupEntity> = emptyList(),
    val rootGroups: List<InspectionRecordGroupEntity> = emptyList(),
    val groupEquipments: Map<UUID, List<GroupEquipmentItem>> = emptyMap()
)

@HiltViewModel
class InspectionRecordDetailViewModel @Inject constructor(
    private val inspectionRecordRepository: InspectionRecordRepository,
    private val groupRepository: InspectionRecordGroupRepository,
    private val groupEquipmentRepository: InspectionRecordGroupEquipmentRepository,
    private val plantRepository: PlantRepository,
    private val equipmentRepository: EquipmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionRecordDetailUiState())
    val uiState: StateFlow<InspectionRecordDetailUiState> = _uiState.asStateFlow()

    fun load(recordId: UUID) {
        viewModelScope.launch {
            try {
                val record = inspectionRecordRepository.getInspectionRecordById(recordId)

                // Fetch plant if record available
                val plant = record?.plantId?.let { plantRepository.getPlantById(it) }

                // Fetch all groups once and include descendants even if their inspectionRecordId is null
                val allGroupsRaw = groupRepository.getAllInspectionRecordGroups().first()

                // Start with groups that directly reference the inspectionRecord
                val starting = allGroupsRaw.filter { it.inspectionRecordId == recordId }
                Log.d("IRDetailVM", "recordId=$recordId startingGroups=${starting.map { it.id }}")

                val included = mutableListOf<InspectionRecordGroupEntity>()
                val includedIds = mutableSetOf<UUID>()
                val queue = ArrayDeque<UUID>()

                starting.forEach { g ->
                    included.add(g)
                    g.id.let { includedIds.add(it); queue.add(it) }
                }

                while (queue.isNotEmpty()) {
                    val parentId = queue.removeFirst()
                    val children = allGroupsRaw.filter { it.parentGroupId == parentId }
                    if (children.isNotEmpty()) Log.d("IRDetailVM", "parent=$parentId children=${children.map { it.id }}")
                    for (child in children) {
                        if (!includedIds.contains(child.id)) {
                            included.add(child)
                            includedIds.add(child.id)
                            queue.add(child.id)
                        }
                    }
                }

                // Sort included groups by orderIndex for display (stable)
                val groups = included.sortedBy { it.orderIndex ?: 0 }
                // root groups are those without parent OR whose parent was not included in the set
                val root = groups.filter { it.parentGroupId == null || !includedIds.contains(it.parentGroupId) }
                Log.d("IRDetailVM", "includedGroupCount=${groups.size} rootCount=${root.size}")

                // fetch link+equipment in a single transaction for included group ids
                val includedGroupIds = groups.map { it.id }
                val linksWithEquipment = if (includedGroupIds.isNotEmpty()) {
                    groupEquipmentRepository.getInspectionRecordGroupEquipmentsWithEquipmentByGroupIdsOnce(includedGroupIds)
                } else emptyList()

                val groupedItems: Map<UUID, List<GroupEquipmentItem>> = linksWithEquipment.groupBy { it.link.inspectionRecordGroupId ?: UUID(0,0) }
                    .mapValues { entry ->
                        entry.value.map { rel ->
                            GroupEquipmentItem(link = rel.link, equipment = rel.equipment)
                        }
                    }

                // Log equipment counts for included groups
                groups.forEach { g ->
                    val count = groupedItems[g.id]?.size ?: 0
                    if (count > 0) {
                        Log.d("IRDetailVM", "group=${g.id} equipmentCount=$count equipmentIds=${groupedItems[g.id]?.map { it.link.id }}")
                    }
                }

                _uiState.value = InspectionRecordDetailUiState(
                    record = record,
                    plant = plant,
                    allGroups = groups,
                    rootGroups = root,
                    groupEquipments = groupedItems
                )
            } catch (ex: Exception) {
                Log.e("IRDetailVM", "Error loading inspection record detail", ex)
            }
        }
    }
}
