package com.tech.thermography.android.ui.auth.register

data class RegisterUiState(
    val firstName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val login: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
