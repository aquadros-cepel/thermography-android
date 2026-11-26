package com.tech.thermography.android.ui.inspection_report

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tech.thermography.android.R
import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEquipmentEntity
import com.tech.thermography.android.data.local.entity.enumeration.EquipmentInspectionStatus
import com.tech.thermography.android.navigation.DeviceUtils
import com.tech.thermography.android.navigation.NavRoutes
import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.UUID

/**
 * Polished Inspection Record detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionRecordDetailScreen(
    recordId: UUID,
    navController: NavHostController,
    expandToEquipmentId: UUID? = null
) {
    val vm: InspectionRecordDetailViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(recordId) {
        vm.load(recordId)
        expandToEquipmentId?.let { vm.expandToEquipment(it) }
    }

    // formatters: date only and date + time
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        CenterAlignedTopAppBar(
            title = { Text(uiState.record?.name ?: "--",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
            },

            navigationIcon = {
                IconButton(onClick = { navController.navigate(NavRoutes.INSPECTION_RECORDS) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        )

        // Header card
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

                Spacer(modifier = Modifier.height(8.dp))

                // START/END: show formatted date+time if present (startedAt and finishedAt are Instant?)
                val startedText = uiState.record?.startedAt?.let { startedInstant ->
                    try {
                        java.time.ZonedDateTime.ofInstant(startedInstant, ZoneId.systemDefault()).format(dateTimeFormatter) + " h"
                    } catch (_: Exception) { "--" }
                } ?: "--"

                val finishedText = uiState.record?.finishedAt?.let { finishedInstant ->
                    try {
                        java.time.ZonedDateTime.ofInstant(finishedInstant, ZoneId.systemDefault()).format(dateTimeFormatter) + " h"
                    } catch (_: Exception) { "--" }
                } ?: "--"

                Text(text = "Inspeção iniciada\t\t: $startedText", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Inspeção encerrada\t: $finishedText", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tree card — card background should be WHITE; nodes keep colored palette
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                            inspectionRecordId = uiState.record?.id,
                            expandedGroupIds = vm.expandedGroupIds,
                            onStatusChange = vm::updateEquipmentStatus
                        )
                        Spacer(modifier = Modifier.height(8.dp)) // Ajuste: aumenta o espaçamento entre os itens da árvore
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
    inspectionRecordId: UUID?,
    expandedGroupIds: kotlinx.coroutines.flow.StateFlow<Set<UUID>>,
    onStatusChange: (InspectionRecordGroupEquipmentEntity, EquipmentInspectionStatus) -> Unit,
) {
    val expandedIds by expandedGroupIds.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(expandedIds, grp.id) {
        expanded = expandedIds.contains(grp.id)
    }

    // Cores fixas para os níveis, independente do tema
    val rowColor = when (level) {
        0 -> Color(0xFFE6F2FF) // light blue
        1 -> Color(0xFFFFF3E0) // light beige
        else -> Color(0xFFEAF7EE) // light green
    }
    val textColor = Color.Black
    val iconColor = Color(0xFF294C7A) // substitui o preto pelo azul escuro

    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        val baseLeft = 6.dp
        val baseRight = 8.dp
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(baseLeft + (level * 8).dp))
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
                        .height(44.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = grp.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )

//                    Text(text = grp.code ?: "", style = MaterialTheme.typography.bodySmall, color = textColor)
                }
            }
        }

        if (expanded) {
            val children = allGroups.filter { it.parentGroupId == grp.id }
            children.forEach { child ->
                GroupNode(grp = child, allGroups = allGroups, groupEquipments = groupEquipments, level = level + 1, navController = navController, plantId = plantId, inspectionRecordId = inspectionRecordId, expandedGroupIds = expandedGroupIds, onStatusChange = onStatusChange)
                Spacer(modifier = Modifier.height(4.dp))
            }

            // show equipments for this group (slightly indented from subgroups)
            val equipments = groupEquipments[grp.id]
            equipments?.forEach { item ->
                EquipmentNode(item, level + 1, navController, plantId, inspectionRecordId, onStatusChange)
                Spacer(modifier = Modifier.height(4.dp))
                // render anomalies (components) under equipment
                val anomalies = item.anomalies
                anomalies.forEach { anomalyDisplay ->
                    AnomalyComponentRow(anomalyDisplay, navController, plantId, item.equipment?.id, inspectionRecordId)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun EquipmentNode(
    item: GroupEquipmentItem,
    level: Int,
    navController: NavHostController,
    plantId: UUID?,
    inspectionRecordId: UUID?,
    onStatusChange: (InspectionRecordGroupEquipmentEntity, EquipmentInspectionStatus) -> Unit
) {
    val paddingStart = (level * 8).dp
    val rowHeight = 40.dp
    val eq = item.equipment
    val display = if (eq != null) {
        val code = eq.code?.takeIf { it.isNotBlank() }
        if (code != null) "${eq.name} (${code})" else eq.name
    } else {
        item.link.equipmentId?.toString() ?: "equip"
    }
    val baseLeft = 6.dp
    val baseRight = 8.dp
    val rowColor = if (item.link.status == EquipmentInspectionStatus.INSPECTED) Color(0xFFEAF7EE) else EquipmentGray
    val textColor = Color.Black
    val iconColor = Color(0xFF294C7A) // substitui o preto pelo azul escuro
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(baseLeft + paddingStart + 4.dp))
        val rightInsetEq = baseRight + (level * 4).dp
        Box(modifier = Modifier
            .weight(1f)
            .padding(end = rightInsetEq)
            .background(color = rowColor, shape = RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EquipmentStatusSelector(
                    status = item.link.status,
                    isTablet = DeviceUtils.isTablet(LocalContext.current),
                    arrowTint = iconColor,
                    onSelected = { onStatusChange(item.link, it) }
                )
                Spacer(modifier = Modifier.width(2.dp))

                Text(text = display, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = textColor)
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            val p = plantId?.toString() ?: "null"
                            val e = item.link.equipmentId?.toString() ?: "null"
                            val r = inspectionRecordId?.toString() ?: "null"
                            val rp = URLEncoder.encode(p, "UTF-8")
                            val re = URLEncoder.encode(e, "UTF-8")
                            val rr = URLEncoder.encode(r, "UTF-8")
                            val route = "${NavRoutes.THERMAL_ANOMALY}?plantId=$rp&equipmentId=$re&inspectionRecordId=$rr&thermographicId=null"
                            try { navController.navigate(route) } catch (ex: Exception) { android.util.Log.e("IRDetailScreen","Navigation failed for route=$route", ex); try { navController.navigate(NavRoutes.THERMOGRAMS) } catch (_: Exception) {} }
                        },
                    tint = iconColor
                )
            }
        }
    }
}

private val statusOptions = listOf(
    EquipmentInspectionStatus.NOT_INSPECTED,
    EquipmentInspectionStatus.INSPECTED,
    EquipmentInspectionStatus.OUT_OF_OPERATION,
    EquipmentInspectionStatus.INACCESSIBLE,
    EquipmentInspectionStatus.OTHER
)

private fun EquipmentInspectionStatus.label(): String = when (this) {
    EquipmentInspectionStatus.NOT_INSPECTED -> "Não Inspecionado"
    EquipmentInspectionStatus.INSPECTED -> "Inspecionado"
    EquipmentInspectionStatus.OUT_OF_OPERATION -> "Fora de Operação"
    EquipmentInspectionStatus.INACCESSIBLE -> "Inacessível"
    EquipmentInspectionStatus.OTHER -> "Outros"
}

private fun EquipmentInspectionStatus.iconRes(): Int = when (this) {
    EquipmentInspectionStatus.NOT_INSPECTED -> R.drawable.ic_status_not_inspected
    EquipmentInspectionStatus.INSPECTED -> R.drawable.ic_status_inspected
    EquipmentInspectionStatus.OUT_OF_OPERATION -> R.drawable.ic_status_out_of_operation
    EquipmentInspectionStatus.INACCESSIBLE -> R.drawable.ic_status_inaccessible
    EquipmentInspectionStatus.OTHER -> R.drawable.ic_status_other
}

private val EquipmentGray = Color(0xFFEEF0F2)

// The inset keeps the gray fill inside the PNG's circular border.
@Composable
private fun StatusIcon(status: EquipmentInspectionStatus, contentDescription: String?) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(21.dp)
                .background(color = EquipmentGray, shape = CircleShape)
        )
        Icon(
            painter = painterResource(status.iconRes()),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Tablet: closed combo shows icon + label; phone: icon only. The open menu always shows both. */
@Composable
private fun EquipmentStatusSelector(
    status: EquipmentInspectionStatus?,
    isTablet: Boolean,
    arrowTint: Color,
    onSelected: (EquipmentInspectionStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = status ?: EquipmentInspectionStatus.NOT_INSPECTED

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIcon(current, current.label())
            if (isTablet) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = current.label(), style = MaterialTheme.typography.bodySmall, color = Color.Black)
            }
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null, tint = arrowTint)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            statusOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    leadingIcon = { StatusIcon(option, null) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun AnomalyComponentRow(
    anomalyDisplay: DisplayAnomaly,
    navController: NavHostController,
    plantId: UUID?,
    equipmentId: UUID?,
    inspectionRecordId: UUID?
) {
    val rowHeight = 40.dp

    val anomaly = anomalyDisplay.record

    // Row background for components (slightly off-white for contrast) and badge color per condition
    val rowBg = Color(0xFFF8F9FA)
    val (labelText, badgeColor) = when (anomaly.condition) {
        com.tech.thermography.android.data.local.entity.enumeration.ConditionType.IMMINENT_HIGH_RISK -> "Alto Risco Iminente" to Color(0xFFF8D7DA)
        com.tech.thermography.android.data.local.entity.enumeration.ConditionType.HIGH_RISK -> "Alto Risco" to Color(0xFFFEEAEA)
        com.tech.thermography.android.data.local.entity.enumeration.ConditionType.MEDIUM_RISK -> "Médio Risco" to Color(0xFFFCEFD8)
        com.tech.thermography.android.data.local.entity.enumeration.ConditionType.LOW_RISK -> "Baixo Risco" to Color(0xFFE8F6FF)
        else -> "Normal" to Color(0xFFDFF5E6)
    }

    val iconColor = Color(0xFF294C7A) // substitui o preto pelo azul escuro

    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(32.dp)) // slight indent relative to equipment
        Box(modifier = Modifier
            .weight(1f)
            .background(color = rowBg, shape = RoundedCornerShape(8.dp))
        ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show component name if available, otherwise fallback to record name
                val leftText = anomalyDisplay.componentName ?: anomaly.name
                Text(text = leftText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.Black)

                // Condition badge as small rounded box
                Box(modifier = Modifier
                    .background(color = badgeColor, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = labelText, style = MaterialTheme.typography.labelSmall, color = Color.Black)
                }

                IconButton(onClick = {
                    // navigate to thermal anomaly with this thermographic record id for editing
                    val p = plantId?.toString() ?: "null"
                    val e = equipmentId?.toString() ?: "null"
                    val r = inspectionRecordId?.toString() ?: "null"
                    val t = anomaly.id.toString()
                    val rp = URLEncoder.encode(p, "UTF-8")
                    val re = URLEncoder.encode(e, "UTF-8")
                    val rr = URLEncoder.encode(r, "UTF-8")
                    val rt = URLEncoder.encode(t, "UTF-8")
                    val route = "${NavRoutes.THERMAL_ANOMALY}?plantId=$rp&equipmentId=$re&inspectionRecordId=$rr&thermographicId=$rt"
                    try { navController.navigate(route) } catch (ex: Exception) { android.util.Log.e("IRDetailScreen","Navigation failed for route=$route", ex); try { navController.navigate(NavRoutes.THERMOGRAMS) } catch (_: Exception) {} }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = iconColor
                    )
                }
            }
        }
    }
}
