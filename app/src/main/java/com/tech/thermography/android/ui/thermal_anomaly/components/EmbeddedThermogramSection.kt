package com.tech.thermography.android.ui.thermal_anomaly.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.ui.components.AppExposedDropdownMenu
import com.tech.thermography.android.ui.thermogram.ThermogramMode
import com.tech.thermography.android.ui.thermogram.components.RoiLabel
import com.tech.thermography.android.ui.thermogram.components.ThermogramImage
import com.tech.thermography.android.ui.thermogram.components.ThermogramDataTable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import java.io.File
import java.util.UUID

@Composable
private fun RoiDropdownInline(
    label: String,
    rois: List<ROIEntity>,
    selected: ROIEntity?,
    onSelect: (ROIEntity) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AppExposedDropdownMenu(
        label = label,
        options = rois,
        selectedOption = selected,
        onOptionSelected = { onSelect(it) },
        optionLabelProvider = { it.label ?: "" },
        modifier = modifier,
        enabled = enabled
    )
}

/**
 * Componente embarcado de Thermogram para uso dentro do ThermalAnomalyForm
 * Versão simplificada sem TopBar e navegação própria
 */
@Composable
fun EmbeddedThermogramSection(
    thermogramId: UUID?,
    thermogram: ThermogramEntity?,
    rois: List<ROIEntity>,
    selectedRoi: ROIEntity?,
    selectedRefRoi: ROIEntity?,
    thermogramImageUri: Uri?,
    mode: ThermogramMode,
    onRoiSelected: (ROIEntity) -> Unit,
    onRefRoiSelected: (ROIEntity) -> Unit,
    onImageSelected: (Uri) -> Unit,
    temperatureDifference: Double?,
    modifier: Modifier = Modifier
) {
    var showLightbox by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // URI temporária para foto da câmera (criada de forma segura)
    val photoUri = remember {
        try {
            val photoFile = File(context.cacheDir, "thermogram_${System.currentTimeMillis()}.jpg")
            photoFile.parentFile?.mkdirs()
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Verificar se tem permissão de câmera
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher para GALERIA (com Intent para permitir navegar para pastas específicas)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    // Launcher para CÂMERA
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) {
            onImageSelected(photoUri)
        }
    }

    // Launcher para solicitar permissão de câmera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted && photoUri != null) {
            cameraLauncher.launch(photoUri)
        }
    }


    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp), // keep only vertical padding, no horizontal
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header com título e controles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Termograma de Monitoramento",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Row(
                // removed lateral spacing
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Botões de Galeria e Câmera (sempre visíveis no modo edit)
                if (mode != ThermogramMode.VIEW) {
                    Row(
                        // removed lateral spacing between icons
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Botão Galeria
                        IconButton(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Galeria",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Botão Câmera
                        IconButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    photoUri?.let { uri ->
                                        cameraLauncher.launch(uri)
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            enabled = true
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Câmera",
                                tint = if (photoUri != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Imagem do termograma
        ThermogramImage(
            imageUri = thermogramImageUri,
            onImageClick = { if (thermogramImageUri != null) showLightbox = true },
            modifier = Modifier.fillMaxWidth()
        )

        Row {
            // ROI Selector (Object area)
            if (rois.isNotEmpty()) {
                if (mode == ThermogramMode.VIEW) {
                    RoiLabel(selectedRoi = selectedRoi)
                } else {
                    RoiDropdownInline(
                        label = "ÁREA DO OBJETO",
                        rois = rois,
                        selected = selectedRoi,
                        onSelect = onRoiSelected,
                        modifier = Modifier.width(200.dp),
                        enabled = true
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ROI Selector (Reference area)
            if (rois.isNotEmpty()) {
                if (mode == ThermogramMode.VIEW) {
                    RoiLabel(selectedRoi = selectedRefRoi)
                } else {
                    RoiDropdownInline(
                        label = "ÁREA DE REFERÊNCIA",
                        rois = rois,
                        selected = selectedRefRoi,
                        onSelect = onRefRoiSelected,
                        modifier = Modifier.width(200.dp),
                        enabled = true
                    )
                }
            }

        }
        // Tabela de dados
        thermogram?.let { thermo ->
            ThermogramDataTable(
                thermogram = thermo,
                selectedRoi = selectedRoi,
                selectedRefRoi = selectedRefRoi,
                temperatureDifference = temperatureDifference,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Lightbox para visualização ampliada
    if (showLightbox && thermogramImageUri != null) {
        // Fullscreen dialog with zoom & pan support
        Dialog(
            onDismissRequest = { showLightbox = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                color = MaterialTheme.colorScheme.surface
            ) {
                // Transform state
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    // Image with zoom & pan
                    AsyncImage(
                        model = thermogramImageUri,
                        contentDescription = "Imagem térmica ampliada",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    // Apply zoom with limits
                                    val newScale = (scale * zoom).coerceIn(1f, 6f)
                                    scale = newScale
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = {
                                    // reset
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                })
                            }
                    )

                    // Close button top-right
                    IconButton(
                        onClick = { showLightbox = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
