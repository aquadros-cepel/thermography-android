package com.tech.thermography.android.ui.auth.login

data class LoginUiState(
    val username: String = "admin",
    val password: String = "admin",
    val rememberMe: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)
