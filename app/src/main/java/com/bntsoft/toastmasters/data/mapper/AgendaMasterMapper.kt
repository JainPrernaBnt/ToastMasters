package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.AgendaMaster
import com.bntsoft.toastmasters.data.model.dto.AgendaMasterDto
import javax.inject.Inject

class AgendaMasterMapper @Inject constructor() : Mapper<AgendaMasterDto, AgendaMaster> {

    override fun mapFromEntity(entity: AgendaMaster): AgendaMasterDto {
        return AgendaMasterDto(
            agendaId = entity.agendaId,
            meetingId = entity.meetingId,
            createdBy = entity.createdBy,
            createdAt = entity.createdAt,
            status = entity.status,
            meetingStartTime = entity.meetingStartTime,
            meetingEndTime = entity.meetingEndTime
        )
    }

    override fun mapToEntity(dto: AgendaMasterDto): AgendaMaster {
        return AgendaMaster(
            agendaId = dto.agendaId,
            meetingId = dto.meetingId,
            createdBy = dto.createdBy,
            createdAt = dto.createdAt,
            status = dto.status,
            meetingStartTime = dto.meetingStartTime,
            meetingEndTime = dto.meetingEndTime
        )
    }

    fun mapFromEntityList(entities: List<AgendaMaster>): List<AgendaMasterDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<AgendaMasterDto>): List<AgendaMaster> {
        return dtos.map { mapToEntity(it) }
    }
}
