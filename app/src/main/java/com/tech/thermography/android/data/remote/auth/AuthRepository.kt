package com.tech.thermography.android.data.remote.auth

interface AuthRepository {
    suspend fun login(username: String, password: String, rememberMe: Boolean = false): AuthResult
    suspend fun register(request: RegisterRequest): RegisterResult
}

data class RegisterResult(
    val success: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
