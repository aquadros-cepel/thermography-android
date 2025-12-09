package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.UserInfoEntity
import com.tech.thermography.android.data.remote.dto.UserInfoDto

object UserInfoMapper {
    fun dtoToEntity(dto: UserInfoDto): UserInfoEntity {
        return UserInfoEntity(
            id = dto.id,
            position = dto.position,
            phoneNumber = dto.phoneNumber,
            companyId = dto.companyId
        )
    }
}
