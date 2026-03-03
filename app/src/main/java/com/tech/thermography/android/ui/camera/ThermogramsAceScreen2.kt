package com.tech.thermography.android.ui.camera

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView
import com.tech.thermography.android.flir.CameraState


@Composable
fun ThermogramsAceScreen2(
    navController: NavHostController,
    viewModel: ThermogramAceViewModel = hiltViewModel()
) {
//    val state by viewModel.state.collectAsState()
//    var glView by remember { mutableStateOf<GLSurfaceView?>(null) }
//
//    // Auto start/stop on enter/leave
//    DisposableEffect(Unit) {
////        viewModel.start()
//        onDispose {
//            viewModel.stop()
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            verticalArrangement = Arrangement.Top
//        ) {
//            // Connection status
//            Text(
//                text = when (state) {
//                    is CameraState.Idle -> "Idle"
//                    is CameraState.Discovering -> "Discovering..."
//                    is CameraState.Connecting -> "Connecting..."
//                    is CameraState.Connected -> "Connected"
//                    is CameraState.Streaming -> "Streaming"
//                    is CameraState.Error -> "Error: ${(state as CameraState.Error).message}"
//                },
//                style = MaterialTheme.typography.bodyLarge,
//                modifier = Modifier.padding(16.dp)
//            )
//            Box(modifier = Modifier.weight(1f)) {
//                AndroidView(
//                    factory = { context ->
//                        GLSurfaceView(context).apply {
//                            setEGLContextClientVersion(3)
//                            preserveEGLContextOnPause = false
//                            viewModel.attachSurface(this)
//                            glView = this
//                            viewModel.start()
//                        }
//                    },
//                    modifier = Modifier.fillMaxSize()
//                )
//            }
//        }
//    }
}
