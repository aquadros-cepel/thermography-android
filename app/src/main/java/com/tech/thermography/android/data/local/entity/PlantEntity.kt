package com.tech.thermography.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "plant",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BusinessUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessUnitId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],

    indices = [
        Index("companyId"),
        Index("businessUnitId")
    ]
)

data class PlantEntity(
        @PrimaryKey val id: UUID = UUID.randomUUID(),
        val code: String?,
        val name: String?,
        val description: String?,
        val latitude: Double?,
        val longitude: Double?,
        val startDate: LocalDate?,
        val companyId: UUID?,
        val businessUnitId: UUID?
)
