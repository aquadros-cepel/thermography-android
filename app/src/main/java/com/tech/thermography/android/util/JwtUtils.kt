package com.tech.thermography.android.util

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.nio.charset.Charset

object JwtUtils {
    /**
     * Verifica se o token JWT está expirado.
     * Retorna true se expirado ou inválido, false caso contrário.
     */
    fun isExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        
        try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val charset: Charset = Charset.forName("UTF-8")
            val payloadJson = String(decodedBytes, charset)
            val jsonObject = JSONObject(payloadJson)
            
            if (jsonObject.has("exp")) {
                val expirationTimeInSeconds = jsonObject.getLong("exp")
                val currentTimeInSeconds = System.currentTimeMillis() / 1000
                
                // Se o tempo de expiração for menor que o tempo atual, o token expirou
                return expirationTimeInSeconds < currentTimeInSeconds
            }
            
            // Se não tiver campo 'exp', assume que não expira (ou tratar conforme regra de negócio)
            return false
        } catch (e: Exception) {
            Log.e("JwtUtils", "Erro ao decodificar token", e)
            return true
        }
    }
}
