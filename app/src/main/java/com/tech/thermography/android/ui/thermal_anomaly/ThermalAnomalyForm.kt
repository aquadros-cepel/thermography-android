package com.tech.thermography.android.ui.thermal_anomaly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import com.tech.thermography.android.ui.components.AppOutlinedField
import com.tech.thermography.android.ui.components.AppDatePickerField
import com.tech.thermography.android.ui.components.AppExposedDropdownMenu
import com.tech.thermography.android.ui.thermal_anomaly.components.EmbeddedThermogramSection
import com.tech.thermography.android.ui.thermogram.ThermogramMode
import java.util.UUID


@Composable
fun RegistrationFields(
    uiState: ThermalAnomalyUiState,
    viewModel: ThermalAnomalyViewModel
) {
    AppExposedDropdownMenu(
        label = "INSTALAÇÃO",
        options = uiState.availablePlants,
        selectedOption = uiState.selectedPlant,
        onOptionSelected = { viewModel.onEvent(ThermalAnomalyEvent.PlantSelected(it)) },
        optionLabelProvider = { it.name ?: "" },
        enabled = false
    )

    AppExposedDropdownMenu(
        label = "LOCALIZAÇÃO DO EQUIPAMENTO",
        options = uiState.filteredEquipments,
        selectedOption = uiState.selectedEquipment,
        onOptionSelected = { viewModel.onEvent(ThermalAnomalyEvent.EquipmentSelected(it)) },
        optionLabelProvider = { it.code ?: "" },
        enabled = false
    )

    AppOutlinedField(
        value = uiState.equipmentType,
        onValueChange = {},
        label = "EQUIPAMENTO",
        enabled = false
    )

    AppExposedDropdownMenu(
        label = "ROTEIRO DE INSPEÇÃO",
        options = uiState.filteredInspectionRecords,
        selectedOption = uiState.selectedInspectionRecord,
        onOptionSelected = { viewModel.onEvent(ThermalAnomalyEvent.InspectionRecordSelected(it)) },
        optionLabelProvider = { it.name },
        enabled = false
    )

    AppOutlinedField(
        value = uiState.recordName,
        onValueChange = { viewModel.onEvent(ThermalAnomalyEvent.UpdateRecordName(it)) },
        label = "NO RELATÓRIO",
        enabled = false
    )

    AppOutlinedField(
        value = uiState.serviceOrder,
        onValueChange = { viewModel.onEvent(ThermalAnomalyEvent.UpdateServiceOrder(it)) },
        label = "ORDEM DE SERVIÇO"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalAnomalyForm(
    plantId: UUID? = null,
    equipmentId: UUID? = null,
    inspectionRecordId: UUID? = null,
    thermographicId: UUID? = null,
    navController: NavHostController? = null,
    viewModel: ThermalAnomalyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    // If we received a thermographicId (editing), load it first so isEditing is set before plant selection
    LaunchedEffect(thermographicId) {
        thermographicId?.let { viewModel.onEvent(ThermalAnomalyEvent.ThermographicSelectedById(it)) }
    }

    // Then process plant/equipment/inspection selection. Processing thermographicId first ensures
    // that when navigating to edit an existing record we won't auto-generate/overwrite the recordName.
    LaunchedEffect(plantId, equipmentId, inspectionRecordId) {
        plantId?.let { viewModel.onEvent(ThermalAnomalyEvent.PlantSelectedById(it)) }
        equipmentId?.let { viewModel.onEvent(ThermalAnomalyEvent.EquipmentSelectedById(it)) }
        inspectionRecordId?.let { viewModel.onEvent(ThermalAnomalyEvent.InspectionRecordSelectedById(it)) }
    }

    // When saved, navigate to InspectionRecordDetail and request it to expand to the equipment
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved && navController != null) {
            val recordId = uiState.selectedInspectionRecord?.id?.toString()
            val equipmentId = uiState.selectedEquipment?.id?.toString()
            if (recordId != null && equipmentId != null) {
                val r = java.net.URLEncoder.encode(recordId, "UTF-8")
                val e = java.net.URLEncoder.encode(equipmentId, "UTF-8")
                val route = "${com.tech.thermography.android.navigation.NavRoutes.INSPECTION_RECORD_DETAIL}/$r?expandEquipmentId=$e"
                try {
                    navController.navigate(route) {
                        popUpTo(com.tech.thermography.android.navigation.NavRoutes.THERMOGRAMS) { inclusive = true }
                    }
                } catch (ex: Exception) {
                    try { navController.popBackStack() } catch (_: Exception) {}
                }
            } else {
                try { navController.popBackStack() } catch (_: Exception) {}
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenterAlignedTopAppBar(
            title = {
                val title = if (uiState.isEditing) "Editar Registro Termográfico" else "Novo Registro Termográfico"
                Text(title)
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (uiState.isDirty) showCancelDialog = true
                    else try { navController?.popBackStack() } catch (_: Exception) {}
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // --- ROW SUPERIOR (Registration Data) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RegistrationFields(uiState, viewModel)
        }

        // --- ROW DO MEIO (THERMOGRAM SCREEN) ---
//        if (uiState.selectedEquipment != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 0.dp)) {
                    EmbeddedThermogramSection(
                        thermogramId = uiState.thermogramId,
                        thermogram = uiState.thermogram,
                        rois = uiState.thermogramRois,
                        refRois = uiState.thermogramRefRois,
                        selectedRoi = uiState.selectedRoi,
                        selectedRefRoi = uiState.selectedRefRoi,
                        thermogramImageUri = uiState.thermogramImageUri,
                        realImageUri = uiState.realImageUri,
                        thermogramRef = uiState.thermogramRef,
                        thermogramRefImageUri = uiState.thermogramRefImageUri,
                        onRefImageSelected = { uri ->
                            viewModel.onEvent(ThermalAnomalyEvent.UpdateRefThermogramImage(uri))
                        },
                        onRealImageSelected = { uri ->
                            viewModel.onEvent(ThermalAnomalyEvent.UpdateRealImage(uri))
                        },
                        mode = ThermogramMode.EDIT,
                        onRoiSelected = { roi ->
                            viewModel.onEvent(ThermalAnomalyEvent.SelectRoi(roi))
                        },
                        onRefRoiSelected = { roi ->
                            viewModel.onEvent(ThermalAnomalyEvent.SelectRefRoi(roi))
                        },
                        onImageSelected = { uri ->
                            viewModel.onEvent(ThermalAnomalyEvent.UpdateThermogramImage(uri))
                        },
                        temperatureDifference = viewModel.calculateTemperatureDifference()
                    )
                }
            }
//        }

        // --- ROW INFERIOR (Analysis Data) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppExposedDropdownMenu(
                    label = "COMPONENTE",
                    options = uiState.availableComponents,
                    selectedOption = uiState.selectedComponent,
                    onOptionSelected = { viewModel.onEvent(ThermalAnomalyEvent.ComponentSelected(it)) },
                    optionLabelProvider = { it.name },
                    modifier = Modifier.weight(1f)
                )
            }

            // CONDITION row alone
            val conditionOptions = listOf(
                ConditionType.NORMAL,
                ConditionType.LOW_RISK,
                ConditionType.MEDIUM_RISK,
                ConditionType.HIGH_RISK,
                ConditionType.IMMINENT_HIGH_RISK
            )
            val conditionLabels = mapOf(
                ConditionType.NORMAL to "Normal",
                ConditionType.LOW_RISK to "Baixo Risco",
                ConditionType.MEDIUM_RISK to "Médio Risco",
                ConditionType.HIGH_RISK to "Alto Risco",
                ConditionType.IMMINENT_HIGH_RISK to "Alto Risco Iminente"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppExposedDropdownMenu(
                    label = "CONDIÇÃO",
                    options = conditionOptions,
                    selectedOption = uiState.condition,
                    onOptionSelected = { viewModel.onEvent(ThermalAnomalyEvent.UpdateCondition(it)) },
                    optionLabelProvider = { conditionLabels[it] ?: it.name },
                    modifier = Modifier.fillMaxWidth(),
                    isCritical = (uiState.condition == ConditionType.IMMINENT_HIGH_RISK)
                )
            }

            // Dates row: EXECUÇÃO and PRÓXIMO MONITORAMENTO
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                AppDatePickerField(
                    label = "MONITORAMENTO",
                    selectedDate = uiState.nextMonitoring,
                    onDateSelected = { viewModel.onEvent(ThermalAnomalyEvent.UpdateNextMonitoring(it)) },
                    modifier = Modifier.weight(1f)
                )

                AppDatePickerField(
                    label = "EXECUÇÃO",
                    selectedDate = uiState.deadlineExecution,
                    onDateSelected = { viewModel.onEvent(ThermalAnomalyEvent.UpdateDeadline(it)) },
                    modifier = Modifier.weight(1f)
                )

            }

            AppOutlinedField(
                value = uiState.analysisDescription,
                onValueChange = { viewModel.onEvent(ThermalAnomalyEvent.UpdateAnalysis(it)) },
                label = "DESCRIÇÃO DA ANÁLISE",
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            AppOutlinedField(
                value = uiState.recommendations,
                onValueChange = { viewModel.onEvent(ThermalAnomalyEvent.UpdateRecommendations(it)) },
                label = "RECOMENDAÇÕES",
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }

        // Error message
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Success message
        if (uiState.isSaved) {
            Text(
                text = "Registro salvo com sucesso!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Botões
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)
        ) {
            TextButton(onClick = {
                if (uiState.isDirty) showCancelDialog = true
                else try { navController?.popBackStack() } catch (_: Exception) {}
            }) {
                Text("Cancelar")
            }
            Button(
                onClick = { viewModel.onEvent(ThermalAnomalyEvent.Save) },
                enabled = !uiState.isLoading
            ) {
                Text("Salvar")
            }
        }
        } // inner scrollable Column
    } // outer Column

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Confirmação") },
            text = {
                Text(
                    "Deseja realmente Cancelar? As alterações feitas não serão salvas.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.onEvent(ThermalAnomalyEvent.Cancel)
                    try { navController?.popBackStack() } catch (_: Exception) {}
                }) {
                    Text("Sim", color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Não", fontSize = 18.sp)
                }
            }
        )
    }
}