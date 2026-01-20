package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.InspectionRecordGroupEntity
import com.tech.thermography.android.data.remote.dto.InspectionRecordGroupDto

object InspectionRecordGroupMapper {
    fun dtoToEntity(dto: InspectionRecordGroupDto): InspectionRecordGroupEntity {
        return InspectionRecordGroupEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            orderIndex = dto.orderIndex,
            finished = dto.finished,
            finishedAt = dto.finishedAt,
            inspectionRecordId = dto.inspectionRecord?.id,
            parentGroupId = dto.parentGroup?.id
        )
    }
}
