package com.tech.thermography.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import com.tech.thermography.android.data.local.entity.enumeration.ThermographicInspectionRecordType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "thermographic_inspection_record",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InspectionRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EquipmentComponentEntity::class,
            parentColumns = ["id"],
            childColumns = ["componentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["createdById"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["finishedById"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ThermogramEntity::class,
            parentColumns = ["id"],
            childColumns = ["thermogramId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ThermogramEntity::class,
            parentColumns = ["id"],
            childColumns = ["thermogramRefId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId"), Index("routeId"), Index("equipmentId"), Index("componentId"), Index("createdById"), Index("finishedById"), Index("thermogramId"), Index("thermogramRefId")]
)
data class ThermographicInspectionRecordEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val type: ThermographicInspectionRecordType,
    val serviceOrder: String?,
    val createdAt: Instant,
    val analysisDescription: String?,
    val condition: ConditionType,
    val deltaT: Double,
    val periodicity: Int?,
    val deadlineExecution: LocalDate?,
    val nextMonitoring: LocalDate?,
    val recommendations: String?,
    val finished: Boolean?,
    val finishedAt: Instant?,
    val plantId: UUID,
    val routeId: UUID?,
    val equipmentId: UUID,
    val componentId: UUID?,
    val createdById: UUID,
    val finishedById: UUID,
    val thermogramId: UUID,
    val thermogramRefId: UUID?
)
