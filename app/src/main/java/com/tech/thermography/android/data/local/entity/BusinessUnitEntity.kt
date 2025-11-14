package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "business_unit",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("companyId")]
)
data class BusinessUnitEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val title: String?,
    val description: String?,
    val companyId: UUID?
)
