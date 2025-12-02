package com.tech.thermography.android.data.remote.sync

import com.tech.thermography.android.data.remote.dto.PlantDto
import retrofit2.http.GET

interface SyncApi {
    @GET("plants")
    suspend fun getAllPlants(): List<PlantDto>
}