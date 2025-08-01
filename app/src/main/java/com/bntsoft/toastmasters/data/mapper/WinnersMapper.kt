package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.Winners
import com.bntsoft.toastmasters.data.model.dto.WinnersDto
import javax.inject.Inject

class WinnersMapper @Inject constructor() : Mapper<WinnersDto, Winners> {
    override fun mapFromEntity(entity: Winners): WinnersDto {
        return WinnersDto(
            id = entity.id,
            meetingId = entity.meetingId,
            category = entity.category,
            winnerUserId = entity.winnerUserId,
            isGuest = entity.isGuest,
            guestName = entity.guestName
        )
    }

    override fun mapToEntity(dto: WinnersDto): Winners {
        return Winners(
            id = dto.id,
            meetingId = dto.meetingId,
            category = dto.category,
            winnerUserId = dto.winnerUserId,
            isGuest = dto.isGuest,
            guestName = dto.guestName
        )
    }

    fun mapFromEntityList(entities: List<Winners>): List<WinnersDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<WinnersDto>): List<Winners> {
        return dtos.map { mapToEntity(it) }
    }
}
