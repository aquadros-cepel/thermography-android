package com.tech.thermography.android.ui.camera

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

@Composable
fun ThermogramsAceScreen3(
    navController: NavHostController,
    viewModel: ThermogramAceViewModel1 = hiltViewModel()
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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // GLSurfaceView with renderer
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    preserveEGLContextOnPause = false
                    viewModel.attachGlSurface(this)
                    viewModel.start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // snackbar host
        Box(modifier = Modifier.fillMaxSize()) {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
        }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.stop()
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
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
                            val nextState = !isFlashOn
                            viewModel.setFlash(nextState) { success, msg ->
                                if (success) isFlashOn = nextState
                                else scope.launch { snackbarHostState.showSnackbar("Flash failed: $msg") }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FlashOn,
                                contentDescription = "Flashlight",
                                tint = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            val nextState = !isLaserOn
                            viewModel.setLaser(nextState) { success, msg ->
                                if (success) isLaserOn = nextState
                                else scope.launch { snackbarHostState.showSnackbar("Laser failed: $msg") }
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
