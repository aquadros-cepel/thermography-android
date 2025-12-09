package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class ROIDto(
    val id: UUID,
    val type: String,
    val label: String,
    val maxTemp: Double,
    val thermogramId: UUID)
