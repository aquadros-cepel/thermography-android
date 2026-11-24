package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.CompanyEntity
import com.tech.thermography.android.data.remote.dto.CompanyDto

object CompanyMapper {
    fun dtoToEntity(dto: CompanyDto): CompanyEntity {
        return CompanyEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            address = dto.address,
            primaryPhoneNumber = dto.primaryPhoneNumber,
            secondaryPhoneNumber = dto.secondaryPhoneNumber,
            taxIdNumber = dto.taxIdNumber,
            city = dto.city,
            complement = dto.complement,
            country = dto.country,
            logo = dto.logo,
            logoContentType = dto.logoContentType,
            neighborhood = dto.neighborhood,
            number = dto.number,
            state = dto.state,
            street = dto.street,
            zipCode = dto.zipCode
        )
    }
}
