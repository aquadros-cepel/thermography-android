package com.tech.thermography.android.data.remote.auth

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("id_token")
    val idToken: String
)
