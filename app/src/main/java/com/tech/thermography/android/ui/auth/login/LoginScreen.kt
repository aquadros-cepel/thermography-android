package com.tech.thermography.android.ui.auth.login

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMotionApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded

    if (uiState.isAuthenticated) {
        LaunchedEffect(Unit) { onLoginSuccess() }
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isTablet) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .weight(1f)) {
                LoginImagesCarousel()
            }
        }

        Box(modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .padding(horizontal = if (isTablet) 64.dp else 24.dp),
            contentAlignment = Alignment.Center) {

            if (isTablet) {
                val scene = remember {
                    """
                    {
                      ConstraintSets: {
                        start: {
                          carousel: { alpha: 0, translationX: -200 },
                          smallImage: { alpha: 0, translationX: 200 },
                          form: { alpha: 0, translationX: 200 }
                        },
                        end: {
                          carousel: { alpha: 1, translationX: 0 },
                          smallImage: { alpha: 1, translationX: 0 },
                          form: { alpha: 1, translationX: 0 }
                        }
                      },
                      Transitions: {
                        default: {
                          from: 'start',
                          to: 'end',
                          duration: 700
                        }
                      }
                    }
                    """.trimIndent()
                }
                var progress by remember { mutableStateOf(0f) }
                LaunchedEffect(Unit) {
                    val steps = 20
                    repeat(steps) { i ->
                        progress = (i + 1) / steps.toFloat()
                        delay(30)
                    }
                }
                MotionLayout(
                    motionScene = MotionScene(scene),
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier) {
                        LoginFormContent(uiState = uiState, isTablet = isTablet,
                            onUsernameChanged = { viewModel.onUsernameChanged(it) },
                            onPasswordChanged = { viewModel.onPasswordChanged(it) },
                            onRememberChanged = { viewModel.onRememberChanged(it) },
                            onLoginClicked = { viewModel.login() },
                            onNavigateToCreateAccount = onNavigateToCreateAccount)
                    }
                    Box(modifier = Modifier) { /* handled above */ }
                }
            } else {
                LoginFormContent(uiState = uiState, isTablet = false,
                    onUsernameChanged = { viewModel.onUsernameChanged(it) },
                    onPasswordChanged = { viewModel.onPasswordChanged(it) },
                    onRememberChanged = { viewModel.onRememberChanged(it) },
                    onLoginClicked = { viewModel.login() },
                    onNavigateToCreateAccount = onNavigateToCreateAccount)
            }
        }
    }
}
