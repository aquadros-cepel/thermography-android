package com.tech.thermography.android.ui.inspection_report

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tech.thermography.android.navigation.NavRoutes
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Polished Inspection Record detail screen.
 */
@Composable
fun InspectionRecordDetailScreen(
    recordId: UUID,
    navController: NavHostController
) {
    val vm: InspectionRecordDetailViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(recordId) {
        vm.load(recordId)
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "Registro de Inspeção: ${uiState.record?.name ?: "--"}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Header card
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "INSTALAÇÃO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = uiState.plant?.name ?: "--", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "FIM PREVISTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = uiState.record?.expectedEndDate?.format(dateFormatter) ?: "--", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // maintenance doc & description compact fields
                OutlinedTextField(
                    value = uiState.record?.maintenanceDocument ?: "",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("DOCUMENTO DE MANUTENÇÃO") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.record?.description ?: "",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("DESCRIÇÃO DO REGISTRO") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Início da inspeção: --", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Encerramento da inspeção: --", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tree card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FB))
        ) {
            if (uiState.rootGroups.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(text = "Nenhum grupo encontrado para este registro.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Verifique se a rota foi sincronizada ou se existem grupos associados.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
                    items(uiState.rootGroups) { grp ->
                        GroupNode(
                            grp = grp,
                            allGroups = uiState.allGroups,
                            groupEquipments = uiState.groupEquipments,
                            level = 0,
                            navController = navController,
                            plantId = uiState.plant?.id,
                            inspectionRecordId = uiState.record?.id
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupNode(
    grp: com.tech.thermography.android.data.local.entity.InspectionRecordGroupEntity,
    allGroups: List<com.tech.thermography.android.data.local.entity.InspectionRecordGroupEntity>,
    groupEquipments: Map<UUID, List<GroupEquipmentItem>>,
    level: Int,
    navController: NavHostController,
    plantId: UUID?,
    inspectionRecordId: UUID?
) {
    var expanded by remember { mutableStateOf(false) }

    // Compact indentation for mobile: 8.dp per level
    val paddingStart = (level * 8).dp
    val rowHeight = 44.dp

    // colors per level
    val rowColor = when (level) {
        0 -> Color(0xFFE9F4FF) // light blue
        1 -> Color(0xFFFFF4E6) // light beige
        else -> Color.Transparent
    }

    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        // Use base left/right gutters so backgrounds don't span full width and can be indented per level
        val baseLeft = 6.dp
        val baseRight = 8.dp

        // Use a Row with leading Spacer so the colored background is indented as a whole
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(baseLeft + paddingStart))
            // increase right padding with level to accentuate visual receding
            val rightInset = baseRight + (level * 4).dp
            Box(modifier = Modifier
                .weight(1f)
                .padding(end = rightInset)
                .background(color = rowColor, shape = RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = grp.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Text(text = grp.code ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (expanded) {
            val children = allGroups.filter { it.parentGroupId == grp.id }
            children.forEach { child ->
                GroupNode(grp = child, allGroups = allGroups, groupEquipments = groupEquipments, level = level + 1, navController = navController, plantId = plantId, inspectionRecordId = inspectionRecordId)
                Spacer(modifier = Modifier.height(4.dp))
            }

            // show equipments for this group (slightly indented from subgroups)
            val equipments = groupEquipments[grp.id]
            equipments?.forEach { item ->
                EquipmentNode(item, level + 1, navController, plantId, inspectionRecordId)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun EquipmentNode(item: GroupEquipmentItem, level: Int, navController: NavHostController, plantId: UUID?, inspectionRecordId: UUID?) {
    // Equipment items are slightly more indented than subgroups; keep compact padding
    val paddingStart = (level * 8).dp
    val rowHeight = 40.dp

    val eq = item.equipment
    val display = if (eq != null) {
        val shortCode = eq.code?.split("-")?.lastOrNull() ?: eq.code ?: item.link.equipmentId?.toString()
        "$shortCode (${eq.name})"
    } else {
        item.link.equipmentId?.toString() ?: "equip"
    }

    // Outer gutter and indented background for equipment
    val baseLeft = 6.dp
    val baseRight = 8.dp
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(baseLeft + paddingStart + 4.dp))
        val rightInsetEq = baseRight + (level * 4).dp
        Box(modifier = Modifier
            .weight(1f)
            .padding(end = rightInsetEq)
            .background(color = Color(0xFFEAF8EE), shape = RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = display, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)

                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            // navigate to thermal anomaly form with query params
                            val p = plantId?.toString() ?: "null"
                            val e = item.link.equipmentId?.toString() ?: "null"
                            val r = inspectionRecordId?.toString() ?: "null"
                            val route = "${NavRoutes.THERMAL_ANOMALY}/$p/$e/$r"
                            navController.navigate(route)
                        },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
