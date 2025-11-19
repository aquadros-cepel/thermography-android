package com.tech.thermography.android.data.remote.auth

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
            val bearer = response.headers()["Authorization"]
            val token = bearer?.removePrefix("Bearer ")
            if (!token.isNullOrBlank()) {
                session.saveToken(token, rememberMe)
                AuthResult(success = true)
            } else {
                AuthResult(error = "Token inválido")
            }
        } catch (e: Exception) {
            AuthResult(error = e.message)
        }
    }
}
