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
        val taxIdNumber: String?
)
