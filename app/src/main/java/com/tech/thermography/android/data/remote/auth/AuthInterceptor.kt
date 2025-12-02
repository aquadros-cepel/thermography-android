package com.tech.thermography.android.data.remote.auth

import com.tech.thermography.android.data.local.storage.UserSessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val session: UserSessionStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.toString()

        // Se for login, não adiciona Authorization
        if (url.endsWith("/authenticate")) {
            return chain.proceed(original)
        }

        // Para as demais requisições, pega o token
        val token = runBlocking { session.token.first() }

        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
