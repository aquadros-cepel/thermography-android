package com.tech.thermography.android.data.remote.dto

import java.util.UUID

data class CompanyDto(
    val id: UUID = UUID.randomUUID(),
    val code: String? = null,
    val name: String = "",
    val description: String? = null,
    val address: String? = null,
    val primaryPhoneNumber: String? = null,
    val secondaryPhoneNumber: String? = null,
    val taxIdNumber: String? = null
)
