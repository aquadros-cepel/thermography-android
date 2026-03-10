package com.tech.thermography.android.ui.thermal_anomaly.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.scale
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
    refRois: List<ROIEntity>,
    selectedRoi: ROIEntity?,
    selectedRefRoi: ROIEntity?,
    thermogramImageUri: Uri?,
    // Reference thermogram params
    thermogramRef: ThermogramEntity?,
    thermogramRefImageUri: Uri?,
    onRefImageSelected: (Uri) -> Unit,
    mode: ThermogramMode,
    onRoiSelected: (ROIEntity) -> Unit,
    onRefRoiSelected: (ROIEntity) -> Unit,
    onImageSelected: (Uri) -> Unit,
    temperatureDifference: Double?,
    modifier: Modifier = Modifier
) {
    var showLightbox by remember { mutableStateOf(false) }
    val singleRoi = rois.size <= 1
    var showRefThermogram by remember(singleRoi) { mutableStateOf(singleRoi) }

    val context = LocalContext.current

    // Decide qual URI usar
    val displayImageUri = remember(thermogramImageUri, thermogram?.localImagePath) {
        if (thermogramImageUri != null) {
            thermogramImageUri
        } else if (!thermogram?.localImagePath.isNullOrBlank()) {
            File(thermogram!!.localImagePath).toUri()
        } else {
            null
        }
    }

    val displayRefImageUri = remember(thermogramRefImageUri, thermogramRef?.localImagePath) {
        if (thermogramRefImageUri != null) {
            thermogramRefImageUri
        } else if (!thermogramRef?.localImagePath.isNullOrBlank()) {
            File(thermogramRef!!.localImagePath).toUri()
        } else {
            null
        }
    }

    // URI temporária para foto da câmera
    val photoUri = remember {
        try {
            val photoFile = File(context.cacheDir, "thermogram_${System.currentTimeMillis()}.jpg")
            photoFile.parentFile?.mkdirs()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val photoRefUri = remember {
        try {
            val photoFile = File(context.cacheDir, "thermogram_ref_${System.currentTimeMillis()}.jpg")
            photoFile.parentFile?.mkdirs()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Gerenciamento de Permissões
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    // Launcher para GALERIA (Usando GetContent para permitir navegação em pastas se necessário)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    val galleryRefLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        uri?.let { 
            onRefImageSelected(it)
            showRefThermogram = true
        } 
    }

    // Launcher para CÂMERA
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) onImageSelected(photoUri)
    }

    val cameraRefLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean -> 
        if (success && photoRefUri != null) {
            onRefImageSelected(photoRefUri)
            showRefThermogram = true
        }
    }

    // Permissões
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted && photoUri != null) cameraLauncher.launch(photoUri)
    }

    val permissionRefLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> 
        hasCameraPermission = isGranted; 
        if (isGranted && photoRefUri != null) cameraRefLauncher.launch(photoRefUri) 
    }

    // Função auxiliar para forçar o Android a ver arquivos novos na pasta da FLIR
    val refreshFlirGallery = {
        try {
            val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val flirDir = File(dcim, "100_FLIR")
            if (flirDir.exists() && flirDir.isDirectory) {
                // Escaneia a própria pasta e arquivos nela para atualizar o MediaStore
                val files = flirDir.listFiles()?.map { it.absolutePath }?.toTypedArray()
                if (files != null && files.isNotEmpty()) {
                    MediaScannerConnection.scanFile(context, files, null, null)
                } else {
                    // Se não conseguir listar, escaneia pelo menos a pasta
                    MediaScannerConnection.scanFile(context, arrayOf(flirDir.absolutePath), null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

            if (mode != ThermogramMode.VIEW) {
                Row(horizontalArrangement = Arrangement.Start) {
                    IconButton(
                        onClick = { 
                            refreshFlirGallery()
                            galleryLauncher.launch("image/*") 
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeria", tint = MaterialTheme.colorScheme.primary)
                    }

//                    IconButton(
//                        onClick = {
//                            if (hasCameraPermission) photoUri?.let { cameraLauncher.launch(it) }
//                            else permissionLauncher.launch(Manifest.permission.CAMERA)
//                        },
//                        modifier = Modifier.size(40.dp)
//                    ) {
//                        Icon(Icons.Default.PhotoCamera, contentDescription = "Câmera",
//                            tint = if (photoUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
//                    }
                }
            }
        }

        ThermogramImage(
            imageUri = displayImageUri,
            onImageClick = { if (displayImageUri != null) showLightbox = true },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (rois.isNotEmpty()) {
                if (mode == ThermogramMode.VIEW) RoiLabel(selectedRoi = selectedRoi)
                else RoiDropdownInline(label = "ÁREA DO OBJETO", rois = rois, selected = selectedRoi, onSelect = onRoiSelected, modifier = Modifier.weight(1f))
            }

            if (!singleRoi && !showRefThermogram && rois.size > 1) {
                if (mode == ThermogramMode.VIEW) RoiLabel(selectedRoi = selectedRefRoi)
                else RoiDropdownInline(label = "ÁREA DE REFERÊNCIA", rois = rois, selected = selectedRefRoi, onSelect = onRefRoiSelected, modifier = Modifier.weight(1f))
            } else if (rois.isNotEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (displayImageUri != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Termograma de Referência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mode != ThermogramMode.VIEW) {
                        IconButton(onClick = { 
                            refreshFlirGallery()
                            galleryRefLauncher.launch("image/*") 
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeria Ref", tint = MaterialTheme.colorScheme.primary)
                        }
//                        IconButton(onClick = {
//                            if (hasCameraPermission) photoRefUri?.let { cameraRefLauncher.launch(it) }
//                            else permissionRefLauncher.launch(Manifest.permission.CAMERA)
//                        }, modifier = Modifier.size(40.dp)) {
//                            Icon(Icons.Default.PhotoCamera, contentDescription = "Câmera Ref",
//                                tint = if (photoRefUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
//                        }
                    }

                    Switch(
                        checked = showRefThermogram,
                        onCheckedChange = { if (!singleRoi) showRefThermogram = it },
                        enabled = !singleRoi,
                        modifier = Modifier.scale(0.6f)
                    )
                }
            }

            if (showRefThermogram) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThermogramImage(imageUri = displayRefImageUri,
                        onImageClick = { if (displayRefImageUri != null) showLightbox = true },
                        modifier = Modifier.fillMaxWidth())

                    val currentRefRois = if (refRois.isNotEmpty()) refRois else rois
                    if (currentRefRois.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (mode == ThermogramMode.VIEW) RoiLabel(selectedRoi = selectedRefRoi)
                            else RoiDropdownInline(label = "ÁREA DE REFERÊNCIA", rois = currentRefRois, selected = selectedRefRoi, onSelect = onRefRoiSelected, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        thermogram?.let { thermo ->
            ThermogramDataTable(thermogram = thermo, selectedRoi = selectedRoi, selectedRefRoi = selectedRefRoi, temperatureDifference = temperatureDifference, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showLightbox && displayImageUri != null) {
        Dialog(onDismissRequest = { showLightbox = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), color = MaterialTheme.colorScheme.surface) {
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = displayImageUri,
                        contentDescription = "Imagem térmica ampliada",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 6f)
                                    offsetX += pan.x; offsetY += pan.y
                                }
                            }
                            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = 1f; offsetX = 0f; offsetY = 0f }) }
                    )
                    IconButton(onClick = { showLightbox = false }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
