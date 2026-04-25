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
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView
import com.tech.thermography.android.navigation.NavRoutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import com.tech.thermography.android.ui.camera.components.MeasurementSpotGlyph
import com.tech.thermography.android.ui.camera.components.MeasurementSpotOverlay
import com.tech.thermography.android.ui.camera.components.MeasurementSquareOverlay
import com.tech.thermography.android.ui.camera.ThermogramCameraViewModel.MeasurementSpotState


private class CustomGLSurfaceView(context: Context) : GLSurfaceView(context) {
    var onSizeChangedCallback: ((Int, Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChangedCallback?.invoke(w, h)
    }
}

@Composable
private fun MeasurementToolGlyph(centerColor: Color) {
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
    var isThermalMode by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isLaserOn by remember { mutableStateOf(false) }
    var sp1State by remember { mutableStateOf(MeasurementSpotState(enabled = true)) }
    var bx1State by remember { mutableStateOf(ThermogramCameraViewModel.MeasurementSquareState(label = "Bx1", enabled = false)) }
    var bx2State by remember { mutableStateOf(ThermogramCameraViewModel.MeasurementSquareState(label = "Bx2")) }
    var measurementOverlaySize by remember { mutableStateOf(IntSize.Zero) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun syncMeasurementStates() {
        // Se necessário, envie o estado do spot para o ViewModel
        viewModel.setMeasurementSquareStates(listOf(bx1State, bx2State))
        // TODO: Se o ViewModel suportar spots, envie sp1State também
    }

    var tempRange by remember { mutableStateOf(viewModel.getTemperatureRange()) }

    LaunchedEffect(Unit) {
        while (true) {
            tempRange = viewModel.getTemperatureRange()
            delay(500)
        }
    }

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
                    onStateChange = { newState ->
                        sp1State = newState
                        syncMeasurementStates()
                    }
                )
            }
            MeasurementSquareOverlay(
                state = bx1State,
                overlaySize = measurementOverlaySize,
                onStateChange = { bx1State = it; syncMeasurementStates() }
            )
            MeasurementSquareOverlay(
                state = bx2State,
                overlaySize = measurementOverlaySize,
                onStateChange = { bx2State = it; syncMeasurementStates() }
            )
        }

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
                        TaskBarButton(active = isThermalMode, onClick = { isThermalMode = !isThermalMode }) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Toggle camera view",
                                tint = if (isThermalMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // MeasurementSpot toggle button (Sp1)
                        TaskBarButton(active = sp1State.enabled, onClick = {
                            val newEnabled = !sp1State.enabled
                            sp1State = sp1State.copy(enabled = newEnabled)
                            if (newEnabled) {
                                bx1State = bx1State.copy(enabled = false)
                            }
                            syncMeasurementStates()
                        }) {
                            MeasurementSpotGlyph()
                        }

                        // MeasurementRectangle toggle button (Bx1)
                        TaskBarButton(active = bx1State.enabled, onClick = {
                            bx1State = bx1State.copy(
                                enabled = !bx1State.enabled
                            )
                            if (bx1State.enabled) {
                                sp1State = sp1State.copy(enabled = false)
                            }
                            syncMeasurementStates()
                        }) {
                            MeasurementToolGlyph(centerColor = Color.Red)
                        }

                        TaskBarButton(active = bx2State.enabled, onClick = {
                            bx2State = bx2State.copy(
                                enabled = !bx2State.enabled
                            )
                            syncMeasurementStates()
                        }) {
                            MeasurementToolGlyph(centerColor = Color(0xFF2196F3))
                        }

                        TaskBarButton(onClick = { /* palette selector */ }) {
                            Icon(
                                imageVector = Icons.Filled.ColorLens,
                                contentDescription = "Palette",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

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
                                imageVector = Icons.Filled.FlashOn,
                                contentDescription = "Flashlight",
                                tint = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TaskBarButton(active = isLaserOn, onClick = {
                            viewModel.toggleLaser { success, msg ->
                                if (!success) {
                                    scope.launch { snackbarHostState.showSnackbar("Laser failed: $msg") }
                                } else {
                                    isLaserOn = !isLaserOn
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.GpsFixed,
                                contentDescription = "Laser",
                                tint = if (isLaserOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
