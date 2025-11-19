package com.tech.thermography.android.data.remote.auth

interface AuthRepository {
    suspend fun login(username: String, password: String, rememberMe: Boolean = false): AuthResult
}
