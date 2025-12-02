package com.tech.thermography.android.data.remote.mapper

import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.remote.dto.PlantDto

object PlantMapper {

    fun dtoToEntity(dto: PlantDto): PlantEntity =
        PlantEntity(
            id = dto.id,
            code = dto.code,
            name = dto.name,
            description = dto.description,
            latitude = dto.latitude,
            longitude = dto.longitude,
            startDate = dto.startDate,
            companyId = dto.companyId,
            businessUnitId = dto.businessUnitId
        )

    fun entityToDto(entity: PlantEntity): PlantDto =
        PlantDto(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            description = entity.description,
            latitude = entity.latitude,
            longitude = entity.longitude,
            startDate = entity.startDate,
            companyId = entity.companyId,
            businessUnitId = entity.businessUnitId
        )

    fun dtoListToEntityList(dtos: List<PlantDto>): List<PlantEntity> =
        dtos.map { dtoToEntity(it) }

    fun entityListToDtoList(entities: List<PlantEntity>): List<PlantDto> =
        entities.map { entityToDto(it) }
}
