package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "user_info",
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
data class UserInfoEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val position: String?,
    val phoneNumber: String?,
    val companyId: UUID?
)
