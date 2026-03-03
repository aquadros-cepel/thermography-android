package com.tech.thermography.android.ui.camera

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
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
import androidx.core.view.doOnAttach
import kotlinx.coroutines.launch

@Composable
fun ThermogramsAceScreen1(
    navController: NavHostController,
    viewModel: ThermogramAceViewModel = hiltViewModel()
) {
    // use navController in effect to avoid unused parameter warning
    LaunchedEffect(navController) { /* no-op: keep navController referenced for now */ }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // ⚠️ CRÍTICO: Cleanup ao sair da tela - parar stream e desconectar câmera
        DisposableEffect(Unit) {
            onDispose {
//                viewModel.stop()
            }
        }

        // ⚠️ CRÍTICO: GLSurfaceView com renderer configurado ANTES de anexar à view hierarchy
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    preserveEGLContextOnPause = false

                    // Configura renderer
                    viewModel.attachGlSurface(this)

                    //Inicia Discovery/conexão após o view estar anexado para evitar problemas de lifecycle
                    doOnAttach {
                        viewModel.start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(width = 2.dp, color = Color.LightGray, shape = CircleShape)
        ) {
            IconButton(onClick = {
//                viewModel.startStream()
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
}
