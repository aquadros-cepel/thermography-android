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
                AuthResult(error = "Usuário ou senha inválidos")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login failed", e)
            AuthResult(error = e.message)
        }
    }

    override suspend fun register(request: RegisterRequest): RegisterResult {
        return try {
            val response = api.register(request)
            if (response.isSuccessful) {
                RegisterResult(
                    success = true,
                    message = "Usuário cadastrado com sucesso. \n\n Um link para acessar a plataforma web Thermal Energy foi enviada para o seu e-mail. \n\nUse o login e senha cadastrados para acessar o aplicativo Thermal Energy e sua plataforma web."
                )
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = if (!errorBody.isNullOrBlank()) {
                    try {
                        val json = org.json.JSONObject(errorBody)
                        val message = json.optString("message")
                        when (message) {
                            "error.userexists" -> "Login está sendo usado"
                            "error.emailexists" -> "E-mail já está em uso"
                            else -> json.optString("detail", "Falha ao criar conta")
                        }
                    } catch (_: Exception) {
                        "Falha ao criar conta"
                    }
                } else {
                    "Falha ao criar conta"
                }
                RegisterResult(error = errorMsg)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration failed", e)
            RegisterResult(error = e.message ?: "Erro de rede")
        }
    }
}
