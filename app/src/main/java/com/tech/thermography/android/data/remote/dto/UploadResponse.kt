package com.tech.thermography.android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UploadResponse(
    @SerializedName("imagePath")
    val imagePath: String,
    @SerializedName("audioPath")
    val audioPath: String? = null,
    @SerializedName("audio")
    val audio: String? = null
)
