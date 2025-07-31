package com.bntsoft.toastmasters.data.mapper

interface Mapper<DTO, ENTITY> {
    fun mapFromEntity(entity: ENTITY): DTO
    fun mapToEntity(dto: DTO): ENTITY
}
