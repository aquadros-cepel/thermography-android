package com.tech.thermography.android.data.remote.auth

import android.util.Log
import com.tech.thermography.android.data.local.storage.UserSessionStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val session: UserSessionStore
) : AuthRepository {

    override suspend fun login(username: String, password: String, rememberMe: Boolean): AuthResult {
        return try {
            val response = api.authenticate(AuthRequest(username, password, rememberMe))
            Log.d("AuthRepository", "Request URL: ${response.raw().request.url}")
            val bearer = response.headers()["Authorization"]
            val token = bearer?.removePrefix("Bearer ")
            if (!token.isNullOrBlank()) {
                session.saveToken(token, rememberMe)
                AuthResult(success = true)
            } else {
                AuthResult(error = "Token inválido")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login failed", e)
            AuthResult(error = e.message)
        }
    }
}
