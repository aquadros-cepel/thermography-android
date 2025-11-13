package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.time.LocalDate
import java.util.*

@Entity(
    tableName = "plant",
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


data class PlantEntity(
        @PrimaryKey val id: UUID = UUID.randomUUID(),
        val name: String,
        val title: String?,
        val description: String?,
        val latitude: Double?,
        val longitude: Double?,
        val startDate: LocalDate?,
        val companyId: UUID?
)
