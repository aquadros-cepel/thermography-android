package com.tech.thermography.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
    val code: String?,
    val name: String,
    val description: String?,
    val companyId: UUID?
)
