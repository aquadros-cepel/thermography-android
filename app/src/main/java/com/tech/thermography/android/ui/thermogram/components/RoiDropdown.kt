package com.tech.thermography.android.ui.thermogram.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.ui.components.AppExposedDropdownMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoiDropdown(
    label: String = "ROI",
    rois: List<ROIEntity>,
    selectedRoi: ROIEntity?,
    onRoiSelected: (ROIEntity) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AppExposedDropdownMenu<ROIEntity>(
        label = label,
        options = rois,
        selectedOption = selectedRoi,
        onOptionSelected = { onRoiSelected(it) },
        optionLabelProvider = { it.label ?: "" },
        modifier = modifier.width(200.dp),
        enabled = enabled
    )
}

// Versão simplificada para modo VIEW (apenas mostra o texto)
@Composable
fun RoiLabel(
    selectedRoi: ROIEntity?,
    modifier: Modifier = Modifier
) {
    Text(
        text = selectedRoi?.label ?: "--",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
