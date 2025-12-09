package com.tech.thermography.android.data.local.entity

import androidx.room.*
import java.util.*
import com.tech.thermography.android.data.local.entity.enumeration.EquipmentInspectionStatus

@Entity(
    tableName = "inspection_record_group_equipment",
    foreignKeys = [
        ForeignKey(
            entity = InspectionRecordGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspectionRecordGroupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("inspectionRecordGroupId"), Index("equipmentId")]
)
data class InspectionRecordGroupEquipmentEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val orderIndex: Int?,
    val status: EquipmentInspectionStatus?,
    val inspectionRecordGroupId: UUID?,
    val equipmentId: UUID?
)
