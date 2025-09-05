package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.domain.model.AgendaItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgendaItemMapper @Inject constructor() : Mapper<AgendaItemDto, AgendaItem> {

    override fun mapFromEntity(entity: AgendaItem): AgendaItemDto {
        return AgendaItemDto(
            id = entity.id,
            meetingId = entity.meetingId,
            orderIndex = entity.orderIndex,
            time = entity.time,
            greenTime = entity.greenTime,
            yellowTime = entity.yellowTime,
            redTime = entity.redTime,
            activity = entity.activity,
            presenterId = entity.presenterId,
            presenterName = entity.presenterName,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun mapToEntity(dto: AgendaItemDto): AgendaItem {
        return AgendaItem(
            id = dto.id,
            meetingId = dto.meetingId,
            orderIndex = dto.orderIndex,
            time = dto.time,
            greenTime = dto.greenTime,
            yellowTime = dto.yellowTime,
            redTime = dto.redTime,
            activity = dto.activity,
            presenterId = dto.presenterId,
            presenterName = dto.presenterName,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun mapFromEntityList(entities: List<AgendaItem>): List<AgendaItemDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<AgendaItemDto>): List<AgendaItem> {
        return dtos.map { mapToEntity(it) }
    }
}
