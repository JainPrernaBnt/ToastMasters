package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MeetingEntity
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import javax.inject.Inject

class MeetingMapper @Inject constructor() : Mapper<MeetingDto, MeetingEntity> {

    override fun mapFromEntity(entity: MeetingEntity): MeetingDto {
        return MeetingDto(
            meetingID = entity.meetingID,
            title = entity.title,
            date = entity.date,
            time = entity.time,
            venue = entity.venue,
            theme = entity.theme,
            preferredRoles = entity.preferredRoles,
            createdAt = entity.createdAt
        )
    }

    override fun mapToEntity(dto: MeetingDto): MeetingEntity {
        return MeetingEntity(
            meetingID = dto.meetingID,
            title = dto.title,
            date = dto.date,
            time = dto.time,
            venue = dto.venue,
            theme = dto.theme,
            preferredRoles = dto.preferredRoles,
            createdAt = dto.createdAt
        )
    }

    fun mapFromEntityList(entities: List<MeetingEntity>): List<MeetingDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<MeetingDto>): List<MeetingEntity> {
        return dtos.map { mapToEntity(it) }
    }
}
