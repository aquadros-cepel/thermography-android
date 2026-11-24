package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(tableName = "company")
data class CompanyEntity(
        @PrimaryKey val id: UUID = UUID.randomUUID(),
        val code: String?,
        val name: String,
        val description: String?,
        val address: String?,
        val primaryPhoneNumber: String?,
        val secondaryPhoneNumber: String?,
        val taxIdNumber: String?,
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
