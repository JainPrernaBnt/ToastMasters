package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.ClubOfficers
import com.bntsoft.toastmasters.data.model.dto.ClubOfficersDto
import javax.inject.Inject

class ClubOfficersMapper @Inject constructor() : Mapper<ClubOfficersDto, ClubOfficers> {

    override fun mapFromEntity(entity: ClubOfficers): ClubOfficersDto {
        return ClubOfficersDto(
            officersId = entity.officersId,
            meetingId = entity.meetingId,
            userId = entity.userId,
            role = entity.role,
            termStart = entity.termStart,
            termEnd = entity.termEnd
        )
    }

    override fun mapToEntity(dto: ClubOfficersDto): ClubOfficers {
        return ClubOfficers(
            officersId = dto.officersId,
            meetingId = dto.meetingId,
            userId = dto.userId,
            role = dto.role,
            termStart = dto.termStart,
            termEnd = dto.termEnd
        )
    }

    fun mapFromEntityList(entities: List<ClubOfficers>): List<ClubOfficersDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<ClubOfficersDto>): List<ClubOfficers> {
        return dtos.map { mapToEntity(it) }
    }
}
