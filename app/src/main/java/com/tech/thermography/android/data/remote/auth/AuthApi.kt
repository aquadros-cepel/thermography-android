package com.tech.thermography.android.data.remote.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("api/authenticate")
    suspend fun authenticate(@Body body: AuthRequest): Response<Unit>

    @GET("api/account")
    suspend fun getAccount(): Response<AccountResponse>
}

// DTO for account if needed
data class AccountResponse(val login: String? = null, val activated: Boolean = false)
