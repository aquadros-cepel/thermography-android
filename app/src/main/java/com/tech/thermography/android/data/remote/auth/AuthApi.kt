package com.tech.thermography.android.data.remote.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("authenticate")
    suspend fun authenticate(@Body body: AuthRequest): Response<AuthResponse>

    @GET("account")
    suspend fun getAccount(): Response<AccountResponse>

    @POST("register")
    suspend fun register(@Body body: RegisterRequest): Response<Unit>
}

// DTO for account if needed
data class AccountResponse(val login: String? = null, val activated: Boolean = false)

data class RegisterRequest(
    val login: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val langKey: String,
    val phoneNumber: String,
    val activated: Boolean
)
