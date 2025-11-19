package com.tech.thermography.android.data.remote.auth

data class AuthRequest(
    val username: String,
    val password: String,
    val rememberMe: Boolean
)
