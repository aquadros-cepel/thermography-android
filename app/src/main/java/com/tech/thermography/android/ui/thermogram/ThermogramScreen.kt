package com.tech.thermography.android.ui.thermogram

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tech.thermography.android.ui.thermogram.components.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermogramScreen(
    thermogramId: UUID,
    onNavigateBack: () -> Unit,
    viewModel: ThermogramViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLightbox by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onEvent(ThermogramEvent.UpdateThermogramImage(it))
        }
    }

    // Carrega o termograma na primeira composição
    LaunchedEffect(thermogramId) {
        viewModel.onEvent(ThermogramEvent.LoadThermogram(thermogramId))
    }

    // Mostra erro se houver
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // TODO: Mostrar Snackbar com erro
            viewModel.onEvent(ThermogramEvent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Termograma de Monitoramento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (uiState.mode == ThermogramMode.VIEW) {
                        IconButton(
                            onClick = {
                                viewModel.onEvent(ThermogramEvent.SetMode(ThermogramMode.EDIT))
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header com título e controles
                ThermogramHeader(
                    title = "Termograma de Monitoramento",
                    rois = uiState.rois,
                    selectedRoi = uiState.selectedRoi,
                    mode = uiState.mode,
                    onRoiSelected = { roi ->
                        viewModel.onEvent(ThermogramEvent.SelectRoi(roi))
                    },
                    onCameraClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                // Imagem do termograma
                ThermogramImage(
                    imageUri = uiState.thermogramImageUri,
                    onImageClick = { showLightbox = true },
                    modifier = Modifier.fillMaxWidth()
                )

                // Tabela de dados
                uiState.thermogram?.let { thermogram ->
                    ThermogramDataTable(
                        thermogram,
                        uiState.selectedRoi,
                        uiState.selectedRefRoi,
                        viewModel.calculateTemperatureDifference(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Lightbox para visualização ampliada (TODO: implementar)
    if (showLightbox && uiState.thermogramImageUri != null) {
        // TODO: Implementar dialog de lightbox
        AlertDialog(
            onDismissRequest = { showLightbox = false },
            confirmButton = {
                TextButton(onClick = { showLightbox = false }) {
                    Text("Fechar")
                }
            },
            text = {
                ThermogramImage(
                    imageUri = uiState.thermogramImageUri,
                    onImageClick = { },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@Composable
private fun ThermogramHeader(
    title: String,
    rois: List<com.tech.thermography.android.data.local.entity.ROIEntity>,
    selectedRoi: com.tech.thermography.android.data.local.entity.ROIEntity?,
    mode: ThermogramMode,
    onRoiSelected: (com.tech.thermography.android.data.local.entity.ROIEntity) -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ROI Selector
            if (rois.isNotEmpty()) {
                if (mode == ThermogramMode.VIEW) {
                    RoiLabel(selectedRoi = selectedRoi)
                } else {
                    RoiDropdown(
                        rois = rois,
                        selectedRoi = selectedRoi,
                        onRoiSelected = onRoiSelected,
                        enabled = mode != ThermogramMode.VIEW
                    )
                }
            }

            // Camera button (only in edit mode)
            if (mode != ThermogramMode.VIEW) {
                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Selecionar imagem",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
