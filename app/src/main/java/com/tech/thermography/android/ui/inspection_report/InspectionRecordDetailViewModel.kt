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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// Wrapper to combine link entity with loaded equipment entity
data class DisplayAnomaly(
    val record: com.tech.thermography.android.data.local.entity.ThermographicInspectionRecordEntity,
    val componentName: String?
)

data class GroupEquipmentItem(
    val link: InspectionRecordGroupEquipmentEntity,
    val equipment: EquipmentEntity?,
    val anomalies: List<DisplayAnomaly> = emptyList()
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
    private val equipmentRepository: EquipmentRepository,
    private val thermographicRepository: com.tech.thermography.android.data.local.repository.ThermographicInspectionRecordRepository,
    private val equipmentComponentRepository: com.tech.thermography.android.data.local.repository.EquipmentComponentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionRecordDetailUiState())
    val uiState: StateFlow<InspectionRecordDetailUiState> = _uiState.asStateFlow()

    // Exposed mutable set of group ids that should be expanded in the tree
    private val _expandedGroupIds = MutableStateFlow<Set<UUID>>(emptySet())
    val expandedGroupIds: StateFlow<Set<UUID>> = _expandedGroupIds.asStateFlow()

    // Optional equipment id that should be brought into view after expansion
    private val _expandedTargetEquipmentId = MutableStateFlow<UUID?>(null)
    val expandedTargetEquipmentId: StateFlow<UUID?> = _expandedTargetEquipmentId.asStateFlow()

    // Given an equipmentId, find the group(s) that contain it and expand the path from root to that group
    fun expandToEquipment(equipmentId: UUID) {
        viewModelScope.launch {
            try {
                val allGroupsRaw = groupRepository.getAllInspectionRecordGroups().first()
                // find link(s) that reference this equipment
                val links = groupEquipmentRepository.getInspectionRecordGroupEquipmentsWithEquipmentByGroupIdsOnce(allGroupsRaw.map { it.id })
                val targetGroupIds = links.filter { it.link.equipmentId == equipmentId }.mapNotNull { it.link.inspectionRecordGroupId }
                if (targetGroupIds.isEmpty()) return@launch

                // For the first match, build ancestor chain
                val target = targetGroupIds.first()
                val idToGroup = allGroupsRaw.associateBy { it.id }
                val ancestors = mutableListOf<UUID>()
                var curr: UUID? = target
                while (curr != null) {
                    ancestors.add(curr)
                    curr = idToGroup[curr]?.parentGroupId
                }
                _expandedGroupIds.value = ancestors.toSet()
                // set the target equipment id so UI can request bringIntoView
                _expandedTargetEquipmentId.value = equipmentId
            } catch (ex: Exception) {
                // ignore
            }
        }
    }

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

                // Prepare base grouping of link+equipment
                val groupedLinksByGroupId: Map<UUID, List<Pair<InspectionRecordGroupEquipmentEntity, EquipmentEntity?>>> = linksWithEquipment.groupBy { it.link.inspectionRecordGroupId ?: UUID(0,0) }
                    .mapValues { entry -> entry.value.map { rel -> Pair(rel.link, rel.equipment) } }

                // If we have a plantId, collect anomalies reactively and update UI state whenever anomalies change
                if (record?.plantId != null) {
                    viewModelScope.launch {
                        thermographicRepository.getThermographicInspectionRecordsByPlantId(record.plantId).collect { allAnomalies ->
                            val anomaliesByEquipment = allAnomalies.groupBy { it.equipmentId }

                            val groupedItems = groupedLinksByGroupId.mapValues { entry ->
                                entry.value.map { (link, eq) ->
                                    val anomalies = eq?.id?.let { anomaliesByEquipment[it] } ?: emptyList()
                                    // resolve component names for each anomaly record
                                    val displayAnomalies = anomalies.map { rec ->
                                        var compName: String? = null
                                        try {
                                            rec.componentId?.let { cid ->
                                                val comp = equipmentComponentRepository.getEquipmentComponentById(cid)
                                                compName = comp?.name ?: comp?.code
                                            }
                                        } catch (_: Exception) { }
                                        DisplayAnomaly(record = rec, componentName = compName)
                                    }
                                    GroupEquipmentItem(link = link, equipment = eq, anomalies = displayAnomalies)
                                }
                            }

                            _uiState.update {
                                it.copy(
                                    record = record,
                                    plant = plant,
                                    allGroups = groups,
                                    rootGroups = root,
                                    groupEquipments = groupedItems
                                )
                            }
                        }
                    }
                } else {
                    // no plant -> set grouped items without anomalies
                    val groupedItems = groupedLinksByGroupId.mapValues { entry ->
                        entry.value.map { (link, eq) -> GroupEquipmentItem(link = link, equipment = eq, anomalies = emptyList()) }
                    }

                    _uiState.value = InspectionRecordDetailUiState(
                        record = record,
                        plant = plant,
                        allGroups = groups,
                        rootGroups = root,
                        groupEquipments = groupedItems
                    )
                }
            } catch (ex: Exception) {
                Log.e("IRDetailVM", "Error loading inspection record detail", ex)
            }
        }
    }
}
