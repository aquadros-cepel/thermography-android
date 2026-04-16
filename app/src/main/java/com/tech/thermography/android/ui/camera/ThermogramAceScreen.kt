package com.tech.thermography.android.ui.camera

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private class CustomGLSurfaceView(context: Context) : GLSurfaceView(context) {
    var onSizeChangedCallback: ((Int, Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSizeChangedCallback?.invoke(w, h)
    }
}

@Composable
fun ThermogramsAceScreen3(
    navController: NavHostController,
    viewModel: ThermogramAceViewModel = hiltViewModel()
) {

    LaunchedEffect(navController) { /* no-op: keep navController referenced for now */ }

    // snapshot counter local state
    var snapshotCount by remember { mutableStateOf(0) }

    // Toggle states for toolbar buttons
    var isThermalMode by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isLaserOn by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var tempRange by remember { mutableStateOf(viewModel.getTemperatureRange()) }

    LaunchedEffect(Unit) {
        while (true) {
            tempRange = viewModel.getTemperatureRange()
            delay(500)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val temperatureBarWidth = (maxWidth * 0.038f).coerceIn(14.dp, 18.dp)
        val temperatureBarHeight = (maxHeight * 0.68f).coerceIn(300.dp, 460.dp)
        val temperatureLabelShape = MaterialTheme.shapes.small

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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(contentAlignment = Alignment.Center) {
                    if (snapshotCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x99000000),
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-36).dp)
                        ) {
                            Text(
                                text = snapshotCount.toString(),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(width = 2.dp, color = Color.LightGray, shape = CircleShape)
                    ) {
                        IconButton(onClick = {
                            viewModel.takeSnapshot { success, msg, _ ->
                                scope.launch {
                                    if (success) {
                                        snapshotCount++
                                        snackbarHostState.showSnackbar("Snapshot saved")
                                    } else {
                                        snackbarHostState.showSnackbar("Snapshot failed: ${msg ?: "unknown"}")
                                    }
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.CenterFocusStrong,
                                contentDescription = "Snapshot",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isThermalMode = !isThermalMode }) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Toggle camera view",
                                tint = if (isThermalMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { /* measurement tools */ }) {
                            Icon(
                                imageVector = Icons.Filled.Build,
                                contentDescription = "Measurement tools",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { /* palette selector */ }) {
                            Icon(
                                imageVector = Icons.Filled.ColorLens,
                                contentDescription = "Palette",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            viewModel.toggleFlash { success, msg ->
                                if (!success)
                                    scope.launch { snackbarHostState.showSnackbar("Flash failed: $msg") }
                                else
                                    isFlashOn = !isFlashOn
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FlashOn,
                                contentDescription = "Flashlight",
                                tint = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            viewModel.toggleLaser { success, msg ->
                                if (!success)
                                    scope.launch { snackbarHostState.showSnackbar("Laser failed: $msg") }
                                else
                                    isLaserOn = !isLaserOn
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
            }
        }
    }
}
