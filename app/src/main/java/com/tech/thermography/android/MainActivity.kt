package com.tech.thermography.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tech.thermography.android.navigation.AppNavHost
import com.tech.thermography.android.ui.auth.login.LoginFormContent
import com.tech.thermography.android.ui.auth.login.LoginUiState
import com.tech.thermography.android.ui.theme.ThermographyAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThermographyAndroidTheme {
                AppNavHost()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginFormContentPreview() {
    ThermographyAndroidTheme {
        LoginFormContent(
            uiState = LoginUiState(error = "Usuário ou senha inválidos"),
            isTablet = false,
            onUsernameChanged = {},
            onPasswordChanged = {},
            onRememberChanged = {},
            onLoginClicked = {},
            onNavigateToCreateAccount = {}
        )
    }
}
