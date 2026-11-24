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
    val taxIdNumber: String? = null,
    val city: String? = null,
    val complement: String? = null,
    val country: String? = null,
    // Base64 do logo, como serializado pelo backend
    val logo: String? = null,
    val logoContentType: String? = null,
    val neighborhood: String? = null,
    val number: String? = null,
    val state: String? = null,
    val street: String? = null,
    val zipCode: String? = null
)
