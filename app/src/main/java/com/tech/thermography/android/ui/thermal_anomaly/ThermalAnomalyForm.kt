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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import com.tech.thermography.android.ui.components.AppOutlinedField
import com.tech.thermography.android.ui.components.AppDatePickerField
import com.tech.thermography.android.ui.components.AppExposedDropdownMenu
import com.tech.thermography.android.ui.thermal_anomaly.ThermalAnomalyEvent

fun ConditionType.displayName(): String = when (this) {
    ConditionType.LOW_RISK -> "Baixo Risco"
    ConditionType.MEDIUM_RISK -> "Médio Risco"
    ConditionType.HIGH_RISK -> "Alto Risco"
    ConditionType.IMMINENT_HIGH_RISK -> "Crítico"
    ConditionType.NORMAL -> "Normal"
}

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
        optionLabelProvider = { it.name ?: "" }
    )

    AppExposedDropdownMenu(
        label = "LOCALIZAÇÃO",
        options = uiState.filteredEquipments,
        selectedOption = uiState.selectedEquipment,
        onOptionSelected = { viewModel.onEvent(ThermalAnomalyEvent.EquipmentSelected(it)) },
        optionLabelProvider = { it.code ?: "" }
    )

    AppOutlinedField(
        value = uiState.equipmentType,
        onValueChange = {},
        label = "EQUIPAMENTO",
//        enabled = false
    )

    AppExposedDropdownMenu(
        label = "ROTEIRO DE INSPEÇÃO",
        options = uiState.filteredInspectionRecords,
        selectedOption = uiState.selectedInspectionRecord,
        onOptionSelected = { viewModel.onEvent(ThermalAnomalyEvent.InspectionRecordSelected(it)) },
        optionLabelProvider = { it.name }
    )

    AppOutlinedField(
        value = uiState.recordName,
        onValueChange = { viewModel.onEvent(ThermalAnomalyEvent.UpdateRecordName(it)) },
        label = "NO RELATÓRIO"
    )

    AppOutlinedField(
        value = uiState.serviceOrder,
        onValueChange = { viewModel.onEvent(ThermalAnomalyEvent.UpdateServiceOrder(it)) },
        label = "ORDEM DE SERVIÇO"
    )
}

@Composable
fun ThermalAnomalyForm(
    viewModel: ThermalAnomalyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Relatório de Registro Termográfico", style = MaterialTheme.typography.headlineSmall)

        // --- ROW SUPERIOR (Registration Data) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RegistrationFields(uiState, viewModel)
        }

        // --- ROW INFERIOR (Analysis Data) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppOutlinedField(
                value = uiState.analysisDescription,
                onValueChange = { viewModel.onEvent(ThermalAnomalyEvent.UpdateAnalysis(it)) },
                label = "DESCRIÇÃO DA ANÁLISE",
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    ConditionType.IMMINENT_HIGH_RISK to "Crítico"
                )
                AppExposedDropdownMenu(
                    label = "CONDIÇÃO",
                    options = conditionOptions,
                    selectedOption = uiState.condition,
                    onOptionSelected = { viewModel.onEvent(ThermalAnomalyEvent.UpdateCondition(it)) },
                    optionLabelProvider = { conditionLabels[it] ?: it.name },
                    modifier = Modifier.weight(1f)
                )

                AppDatePickerField(
                    label = "PRAZO EXECUÇÃO",
                    selectedDate = uiState.deadlineExecution,
                    onDateSelected = { viewModel.onEvent(ThermalAnomalyEvent.UpdateDeadline(it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            AppOutlinedField(
                value = uiState.recommendations,
                onValueChange = { viewModel.onEvent(ThermalAnomalyEvent.UpdateRecommendations(it)) },
                label = "RECOMENDAÇÕES",
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
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
            TextButton(onClick = { viewModel.onEvent(ThermalAnomalyEvent.Cancel) }) {
                Text("Cancelar")
            }
            Button(
                onClick = { viewModel.onEvent(ThermalAnomalyEvent.Save) },
                enabled = !uiState.isLoading
            ) {
                Text("Salvar")
            }
        }
    }
}