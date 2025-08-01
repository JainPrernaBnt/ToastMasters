package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.Guest
import com.bntsoft.toastmasters.data.model.dto.GuestDto
import javax.inject.Inject

class GuestMapper @Inject constructor() : Mapper<GuestDto, Guest> {
    override fun mapFromEntity(entity: Guest): GuestDto {
        return GuestDto(
            guestId = entity.guestId,
            meetingId = entity.meetingId,
            name = entity.name,
            phone = entity.phone,
            email = entity.email,
            playedRole = entity.playedRole,
            rolePlayed = entity.rolePlayed,
            gender = entity.gender
        )
    }

    override fun mapToEntity(dto: GuestDto): Guest {
        return Guest(
            guestId = dto.guestId,
            meetingId = dto.meetingId,
            name = dto.name,
            phone = dto.phone,
            email = dto.email,
            playedRole = dto.playedRole,
            rolePlayed = dto.rolePlayed,
            gender = dto.gender
        )
    }

    fun mapFromEntityList(entities: List<Guest>): List<GuestDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<GuestDto>): List<Guest> {
        return dtos.map { mapToEntity(it) }
    }
}
