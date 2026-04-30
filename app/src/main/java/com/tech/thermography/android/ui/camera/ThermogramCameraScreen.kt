package com.tech.thermography.android.ui.camera

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView
import com.tech.thermography.android.navigation.NavRoutes
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import com.tech.thermography.android.ui.camera.components.MeasurementSpotGlyph
import com.tech.thermography.android.ui.camera.components.MeasurementSpotOverlay
import com.tech.thermography.android.ui.camera.components.MeasurementSquareOverlay
import com.tech.thermography.android.ui.camera.MeasurementSpotState


import com.flir.thermalsdk.image.Palette
import com.flir.thermalsdk.image.PaletteManager
import com.flir.thermalsdk.image.ThermalValue
import com.flir.thermalsdk.image.TemperatureUnit
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.alpha
import com.tech.thermography.android.ui.components.CompactUiWrapper
import androidx.compose.ui.graphics.graphicsLayer


private class CustomGLSurfaceView(context: Context) : GLSurfaceView(context) {
    var onSizeChangedCallback: ((Int, Int) -> Unit)? = null

    // Corrige assinatura do método para onSizeChanged
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChangedCallback?.invoke(w, h)
    }
}

@Composable
private fun MeasurementSquareToolGlyph(centerColor: Color) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.CenterFocusStrong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(centerColor)
                .border(1.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun MeasurementSpotToolGlyph(centerColor: Color = Color.Red) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.GpsFixed,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(centerColor)
                .border(1.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun MeasurementLaserToolGlyph(centerColor: Color = Color.Red) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.Flare,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(centerColor)
                .border(1.dp, Color.White, CircleShape)
        )
    }
}
@Composable
private fun TaskBarButton(
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        tonalElevation = if (active) 5.dp else 1.dp,
        shadowElevation = if (active) 2.dp else 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = if (active) Color.Black.copy(alpha = 0.35f) else Color.Transparent,
            shape = CircleShape
        )
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun RecentThermogramThumbnail(
    imagePath: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val imageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, key1 = imagePath) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onOpen)
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Thumbnail",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
        // Removed delete button (x)
        // onRemove não é usado, mas mantido para compatibilidade de assinatura
    }
}

fun syncMeasurementStates(
    viewModel: ThermogramCameraViewModel,
    sp1State: MeasurementSpotState,
    bx1State: MeasurementSquareState,
    deltaState: MeasurementSquareState
) {
    // Só envia os MeasurementSquares para o controller
    viewModel.setMeasurementSquareStates(listOf(sp1State, bx1State, deltaState))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermogramsCameraScreen(
    navController: NavHostController,
    viewModel: ThermogramCameraViewModel = hiltViewModel()
) {

    LaunchedEffect(navController) { /* no-op: keep navController referenced for now */ }
    
    // Get the activity context
    val context = LocalContext.current
    val activity = remember(context) { 
        context as? Activity ?: error("Context must be an Activity for screen capture")
    }

    // snapshot counter local state
    val recentThermograms by viewModel.recentThermograms.collectAsState()
    var showSnapshotButton by remember { mutableStateOf(true) }


    // Toggle states for toolbar buttons
    var isThermalMode by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isLaserOn by remember { mutableStateOf(false) }
    var sp1State by remember { mutableStateOf(MeasurementSpotState(label = "Sp1", enabled = true, add = true, remove = false)) }
    var bx1State by remember { mutableStateOf(MeasurementSquareState(label = "Bx1", enabled = false, add = false, remove = false)) }
    var deltaState by remember { mutableStateOf(MeasurementSquareState(label = "Bx2", enabled = false, add = false, remove = false)) }
    // Sincroniza o estado inicial do Sp1 com o ViewModel/controller após todas as variáveis de estado serem declaradas
    LaunchedEffect(Unit) {
        syncMeasurementStates(viewModel, sp1State, bx1State, deltaState)
    }
    var measurementOverlaySize by remember { mutableStateOf(IntSize.Zero) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val measurementTemperatures by viewModel.measurementTemperatures.collectAsState()
    val tempRange by viewModel.temperatureRange.collectAsState()

    // Thermal parameters overlay state (corrigido escopo)
    var showThermalParams by remember { mutableStateOf(false) }
    val thermalParamsUi by viewModel.thermalParametersUi.collectAsState()

    LaunchedEffect(recentThermograms) {
        viewModel.pruneMissingRecentThermograms()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val temperatureBarWidth = (maxWidth * 0.038f).coerceIn(14.dp, 18.dp)
        val temperatureBarHeight = (maxHeight * 0.60f).coerceIn(270.dp, 414.dp) // Reduzido em ~12%
        val temperatureLabelShape = MaterialTheme.shapes.small
        val toolbarHeight = 56.dp
        val recentStripHeight = 90.dp
        val bottomReservedHeight = toolbarHeight + recentStripHeight

        // GLSurfaceView with renderer
        AndroidView(
            factory = { context: Context ->
                CustomGLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    preserveEGLContextOnPause = false
                    onSizeChangedCallback = { w: Int, h: Int ->
                        viewModel.onSurfaceSizeChanged(w, h)
                    }
                    viewModel.attachGlSurface(this)
                    viewModel.start()
//                    viewModel.startStream()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White)
        )

        // Keep overlays above toolbar + recent strip area.
        val measurementModeBottomPadding = bottomReservedHeight + 16.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = measurementModeBottomPadding)
                .onSizeChanged { measurementOverlaySize = it }
        ) {
            if (sp1State.enabled) {
                MeasurementSpotOverlay(
                    state = sp1State,
                    overlaySize = measurementOverlaySize,
                    onStateChange = {
                        sp1State = it;
                        syncMeasurementStates(viewModel, sp1State, bx1State, deltaState)
                    }
                )
            }
            MeasurementSquareOverlay(
                state = bx1State,
                overlaySize = measurementOverlaySize,
                onStateChange = { bx1State = it;
                    syncMeasurementStates(viewModel, sp1State, bx1State, deltaState) }
            )
            MeasurementSquareOverlay(
                state = deltaState,
                overlaySize = measurementOverlaySize,
                onStateChange = { deltaState = it;
                    syncMeasurementStates(viewModel, sp1State, bx1State, deltaState) }
            )
        }

        //Barra lateral com o range temperaturas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 2.dp,top = 8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.End

            ) {
                val maxText = tempRange?.max?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "--"
                val minText = tempRange?.min?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "--"

                Surface(
                    color = Color(0xB0000000),
                    shape = temperatureLabelShape,
                    modifier = Modifier.border(1.dp, Color.White, temperatureLabelShape)
                ) {
                    Text(
                        text = maxText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .width(temperatureBarWidth)
                        .height(temperatureBarHeight)
                        .clip(temperatureLabelShape)
                        .border(1.dp, Color.White, temperatureLabelShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFF6C2), // quase branco (topo)
                                    Color(0xFFFFE066), // amarelo claro
                                    Color(0xFFFFB347), // amarelo/laranja
                                    Color(0xFFFF7A1A), // laranja forte
                                    Color(0xFFE53935), // vermelho
                                    Color(0xFF8E24AA), // roxo
                                    Color(0xFF3949AB), // azul
                                    Color(0xFF0D1B2A)  // azul escuro (base)
                                )
                            )
                        )
                ) {
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = Color(0xB0000000),
                    shape = temperatureLabelShape,
                    modifier = Modifier.border(1.dp, Color.White, temperatureLabelShape)
                ) {
                    Text(
                        text = minText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xCC000000),
                    shape = CircleShape,
                    modifier = Modifier.border(1.dp, Color.White, CircleShape)
                ) {
                    IconButton(
                        onClick = { /* placeholder only: configure min/max later */ },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Configure temperature range",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Stop stream and disconnect when leaving the screen
        DisposableEffect(Unit) {
            onDispose {
                viewModel.stop()
            }
        }

        // Snackbar host
        Box(modifier = Modifier.fillMaxSize()) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }

        //Botão de Snapshot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomReservedHeight + 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(contentAlignment = Alignment.Center) {

                if (showSnapshotButton) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(width = 2.dp, color = Color.LightGray, shape = CircleShape)
                    ) {
                        IconButton(onClick = {
                            showSnapshotButton = false
                            viewModel.takeSnapshotWithOverlay(activity) { success, msg, _ ->
                                scope.launch {
                                    showSnapshotButton = true
                                    if (success) {
//                                                snackbarHostState.showSnackbar("Snapshot with overlay saved")
                                    } else {
                                        snackbarHostState.showSnackbar("Snapshot failed: ${msg ?: "unknown"}")
                                    }
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Camera,
                                contentDescription = "Snapshot",
                                tint = Color.White,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                }
            }
        }

        //Toolbar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(toolbarHeight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Botão único para alternância Sp1 <-> Bx1 <-> Sp1 desabilitado
                        TaskBarButton(
                            active = sp1State.enabled || bx1State.enabled,
                            onClick = {
                                when {
                                    // Se Sp1 está habilitado, ativa Bx1 e desabilita Sp1
                                    sp1State.enabled -> {
                                        sp1State = sp1State.copy(enabled = false, add = false, remove = true)
                                        bx1State = bx1State.copy(enabled = true, add = true, remove = false)
                                    }
                                    // Se Bx1 está habilitado, desabilita Sp1 e coloca Bx1 como remove
                                    bx1State.enabled -> {
                                        sp1State = sp1State.copy(enabled = false, add = false, remove = true)
                                        bx1State = bx1State.copy(enabled = false, add = false, remove = true)
                                    }
                                    // Se ambos desabilitados, volta para Sp1 habilitado
                                    else -> {
                                        sp1State = sp1State.copy(enabled = true, add = true, remove = false)
                                        bx1State = bx1State.copy(enabled = false, add = false, remove = true)
                                    }
                                }
                                syncMeasurementStates(viewModel, sp1State, bx1State, deltaState)
                            }
                        ) {
                            when {
                                sp1State.enabled -> MeasurementSpotToolGlyph(centerColor = Color(0xFFE63600)
                                )
                                bx1State.enabled -> MeasurementSquareToolGlyph(centerColor = Color(0xFFE63600))
                                else -> MeasurementSpotToolGlyph(centerColor = Color.LightGray)
                            }
                        }

                        // MeasurementRectangle button (delta) só habilitado se Sp1 ou Bx1 estiverem habilitados
                        // Estado derivado para habilitação do botão delta
                        val deltaEnabled by remember(sp1State.enabled, bx1State.enabled) {
                            mutableStateOf(sp1State.enabled || bx1State.enabled)
                        }

                        // Efeito colateral para garantir que deltaState seja desabilitado e removido quando deltaEnabled ficar falso
                        DisposableEffect(deltaEnabled) {
                            if (!deltaEnabled && deltaState.enabled) {
                                deltaState = deltaState.copy(enabled = false, remove = true)
                                syncMeasurementStates(viewModel, sp1State, bx1State, deltaState)
                            }
                            onDispose { }
                        }

                        Box(
                            modifier = Modifier.alpha(if (deltaEnabled) 1f else 0.4f)
                        ) {
                                TaskBarButton(
                                    active = deltaState.enabled,
                                    onClick = {
                                        if (deltaEnabled) {
                                            val newEnabled = !deltaState.enabled
                                            if (newEnabled) {
                                                // Ao habilitar, posiciona logo abaixo do Bx1
                                                val offsetY = bx1State.sizeFraction * 1.2f // deslocamento vertical proporcional ao tamanho
                                                val newCenterY = (bx1State.centerYFraction + offsetY).coerceAtMost(0.95f)
                                                deltaState = deltaState.copy(
                                                    enabled = true,
                                                    add = true,
                                                    remove = false,
                                                    centerXFraction = bx1State.centerXFraction,
                                                    centerYFraction = newCenterY
                                                )
                                            } else {
                                                deltaState = deltaState.copy(
                                                    enabled = false,
                                                    add = false,
                                                    remove = true
                                                )
                                            }
                                            syncMeasurementStates(viewModel, sp1State, bx1State, deltaState)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "ΔT",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (deltaState.enabled) Color(0xFFFFA500) else Color.LightGray
                                    )
                                }
                        }

                        // Palette selector
                        var paletteMenuExpanded by remember { mutableStateOf(false) }
                        val currentPalette by viewModel.currentPalette.collectAsState()
                        val filteredPalettesWithLabel = viewModel.filteredPalettesWithLabel
                        TaskBarButton(onClick = { paletteMenuExpanded = true }, active = paletteMenuExpanded) {
                            Icon(
                                imageVector = Icons.Filled.ColorLens,
                                contentDescription = "Palette"
                            )
                        }
                        DropdownMenu(
                            expanded = paletteMenuExpanded,
                            onDismissRequest = { paletteMenuExpanded = false },
                            containerColor = Color.Transparent, // 👈 ESSENCIAL
                            tonalElevation = 0.dp,              // 👈 remove sombra tonal
                            shadowElevation = 0.dp,             // 👈 remove sombra
                            offset = DpOffset(x = 0.dp, y = (-100).dp) // 👈 sobe o menu 20dp
                        ) {
                            filteredPalettesWithLabel.forEach { (palette, label) ->
                                DropdownMenuItem(
                                    trailingIcon = {
                                        if (palette == currentPalette) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .background(
                                                        color = Color.Black.copy(alpha = 0.5f), // 50% de transparência
                                                        shape = RoundedCornerShape(6.dp)
                                                    ),
                                            )
                                        }
                                    },
                                    text = {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Color.Black.copy(alpha = 0.5f), // 50% de transparência
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = Color.White
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectPalette(palette)
                                    }
                                )
                            }
                        }

                        // Fusion mode selector
                        var fusionMenuExpanded by remember { mutableStateOf(false) }
                        val currentFusionMode by viewModel.currentFusionMode.collectAsState()
                        val fusionModesWithLabel = viewModel.fusionModesWithLabel
                        TaskBarButton(onClick = { fusionMenuExpanded = true }, active = fusionMenuExpanded) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = "Image Mode"
                            )
                        }
                        DropdownMenu(
                            expanded = fusionMenuExpanded,
                            onDismissRequest = { fusionMenuExpanded = false },
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            offset = DpOffset(x = 0.dp, y = (-100).dp)
                        ) {
                            fusionModesWithLabel.forEach { (mode, label) ->
                                DropdownMenuItem(
                                    trailingIcon = {
                                        if (mode == currentFusionMode) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .background(
                                                        color = Color.Black.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ),
                                            )
                                        }
                                    },
                                    text = {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Color.Black.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = Color.White
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectFusionMode(mode)
                                        // Removido: fusionMenuExpanded = false
                                    }
                                )
                            }
                        }

                        // Flashlight toggle button
                        TaskBarButton(active = isFlashOn, onClick = {
                            viewModel.toggleFlash { success, msg ->
                                if (!success) {
                                    scope.launch { snackbarHostState.showSnackbar("Flash failed: $msg") }
                                } else {
                                    isFlashOn = !isFlashOn
                                }
                            }
                        }) {
                            Icon(
//                                imageVector = if (isFlashOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOn,
                                imageVector = Icons.Filled.FlashlightOn,
                                contentDescription = "Flashlight",
                                tint = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Laser toggle button
                        TaskBarButton(active = isLaserOn, onClick = {
                            viewModel.toggleLaser { success, msg ->
                                if (!success) {
                                    scope.launch { snackbarHostState.showSnackbar("Laser failed: $msg") }
                                } else {
                                    isLaserOn = !isLaserOn
                                }
                            }
                        }) {
                            MeasurementLaserToolGlyph(centerColor = Color.Red)
                        }

                        // Toolbar: adicionar overlay de configuração
                        TaskBarButton(active = isThermalMode, onClick = { showThermalParams = true }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = if (isThermalMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Always show the recent thermograms area with dark gray background and placeholder if empty
                val recentBarHeight = 72.dp
                Surface(
                    color = Color(0xFF23272A), // dark gray
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(recentBarHeight)
                ) {
                    if (recentThermograms.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Termogramas Recentes...",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(8.dp)) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(recentThermograms) { imagePath ->
                                    RecentThermogramThumbnail(
                                        imagePath = imagePath,
                                        onOpen = {
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("thermogram_recent_list", ArrayList(recentThermograms))
                                            navController.navigate("${NavRoutes.THERMOGRAM_IMAGE}/${Uri.encode(imagePath)}")
                                        },
                                        onRemove = {
                                            viewModel.removeRecentThermogram(imagePath, deleteFile = true)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay watermark in bottom start above toolbar
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            WatermarkOverlay()
        }

        Box(
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .background(Color(0xCC000000), shape = RoundedCornerShape(8.dp))
                .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column {
                val objTemp = measurementTemperatures.spot ?: measurementTemperatures.bx1
                if (objTemp != null) {
                    val objLabel = if (sp1State.enabled) sp1State.label else bx1State.label
                    Text(
                        text = "$objLabel\t" + String.format(Locale.getDefault(), "%.1f\t°C", objTemp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }else
                {
                    Text(
                        text = "°C",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Se delta (Ref) estiver ativo, mostrar Ref e AT
                if (deltaState.enabled && measurementTemperatures.delta != null) {
                    Text(
                        text = "Ref\t" + (measurementTemperatures.delta?.let { String.format(Locale.getDefault(), "%.1f\t°C", it) } ?: "--"),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                    val objTemp = measurementTemperatures.spot ?: measurementTemperatures.bx1
                    val at = if (objTemp != null && measurementTemperatures.delta != null) objTemp - measurementTemperatures.delta!! else null
                    Text(
                        text = "ΔT\t\t" + (at?.let { String.format(Locale.getDefault(), "%.1f\t°C", it) } ?: "--"),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (showThermalParams) {
            ThermalParametersOverlay(
                parameters = thermalParamsUi,
                onValueChange = { viewModel.updateThermalParametersUi { _ -> it } },
                onSave = {
                    viewModel.saveThermalParameters()
                    showThermalParams = false
                },
                onDismiss = { showThermalParams = false }
            )
        }
    }
}

@Composable
private fun WatermarkOverlay(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(start = 8.dp, bottom = 120.dp) // 100dp to sit above toolbar and recent bar
//            .background(Color(0x80000000), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = com.tech.thermography.android.R.drawable.logo_thermal_energy_vb),
            contentDescription = "Logo Thermal Energy",
            modifier = Modifier.size(60.dp)
        )
    }
}

@Composable
private fun ThermalParametersOverlay(
    parameters: ThermogramCameraViewModel.ThermalParametersUi,
    onValueChange: (ThermogramCameraViewModel.ThermalParametersUi) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss, indication = null, interactionSource = remember { MutableInteractionSource() })
            .offset(y = (-60).dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                .padding(24.dp)
                .widthIn(min = 160.dp, max = 240.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Parâmetros Térmicos", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                CompactUiWrapper {
                    OutlinedTextField(
                        value = parameters.distance.toString(),
                        onValueChange = { v ->
                            v.toDoubleOrNull()?.let {
                                onValueChange(parameters.copy(distance = it))
                            }
                        },
                        label = { Text("DISTÂNCIA (m)") },
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                CompactUiWrapper {
                    OutlinedTextField(
                        value = parameters.emissivity.toString(),
                        onValueChange = { v ->
                            v.toDoubleOrNull()?.let {
                                if (it in 0.0..1.0) onValueChange(parameters.copy(emissivity = it))
                            }
                        },
                        label = { Text("EMISSIVIDADE (0.0 - 1.0)") },
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                CompactUiWrapper {
                    OutlinedTextField(
                        value = parameters.reflectedTemperature.value.toString(),
                        onValueChange = { v ->
                            v.toDoubleOrNull()?.let {
                                onValueChange(
                                    parameters.copy(
                                        reflectedTemperature = ThermalValue(
                                            it,
                                            TemperatureUnit.CELSIUS
                                        )
                                    )
                                )
                            }
                        },
                        label = { Text("TEMP. REFLETIDA (°C)") },
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                CompactUiWrapper {
                    OutlinedTextField(
                        value = parameters.atmosphericTemperature.value.toString(),
                        onValueChange = { v ->
                            v.toDoubleOrNull()?.let {
                                 onValueChange(parameters.copy(atmosphericTemperature = ThermalValue(it, TemperatureUnit.CELSIUS)))
                            }
                        },
                        label = { Text("TEMP. ATMOSFÉRICA (°C)") },
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))

                CompactUiWrapper {
                    OutlinedTextField(
                        value = parameters.relativeHumidity.toString(),
                        onValueChange = { v ->
                            v.toDoubleOrNull()?.let {
                                if (it in 0.0..100.0) onValueChange(parameters.copy(relativeHumidity = it))
                            }
                        },
                        label = { Text("UMIDADE RELATIVA (%)") },
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Text("Salvar")
                    }
                }
            Spacer(Modifier.height(8.dp))                
            }
        }
    }
}
