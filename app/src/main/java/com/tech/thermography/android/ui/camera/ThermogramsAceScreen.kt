package com.tech.thermography.android.ui.camera

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ThermogramsAceScreen(
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
                // ViewModel.onCleared() será chamado automaticamente
            }
        }

        // ⚠️ CRÍTICO: GLSurfaceView com renderer configurado ANTES de anexar à view hierarchy
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    preserveEGLContextOnPause = false
                    // Configura renderer e inicia discovery/conexão
                    viewModel.attachGlSurface(this)
                    viewModel.start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
