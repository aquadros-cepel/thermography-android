package com.tech.thermography.android.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tech.thermography.android.navigation.DeviceUtils

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (Boolean) -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isTablet = DeviceUtils.isTablet(context)
    val scrollState = rememberScrollState()

    if (uiState.isAuthenticated) {
        LaunchedEffect(uiState.isAuthenticated) { onLoginSuccess(uiState.shouldSync) }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val carouselHeight = screenHeight * 0.4f
    val formMinHeight = if (isTablet) screenHeight * 0.6f else screenHeight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isTablet) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .height(carouselHeight)
            ) {
                LoginImagesCarousel(modifier = Modifier.fillMaxSize())
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = formMinHeight)
                .padding(horizontal = if (isTablet) 64.dp else 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LoginFormContent(
                uiState = uiState,
                isTablet = isTablet,
                onUsernameChanged = { viewModel.onUsernameChanged(it) },
                onPasswordChanged = { viewModel.onPasswordChanged(it) },
                onRememberChanged = { viewModel.onRememberChanged(it) },
                onLoginClicked = { viewModel.login() },
                onNavigateToCreateAccount = onNavigateToCreateAccount
            )
        }
    }
}
