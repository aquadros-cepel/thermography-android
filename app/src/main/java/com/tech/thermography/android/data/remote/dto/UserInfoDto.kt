package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class UserInfoDto(
    val id: UUID,
    val position: String?,
    val phoneNumber: String?,
    val companyId: UUID?
)
