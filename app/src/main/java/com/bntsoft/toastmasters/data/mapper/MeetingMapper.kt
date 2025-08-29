package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MeetingEntity
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import javax.inject.Inject

class MeetingMapper @Inject constructor() : Mapper<MeetingDto, MeetingEntity> {

    override fun mapFromEntity(entity: MeetingEntity): MeetingDto {
        return MeetingDto(
            meetingID = entity.meetingID,
            date = entity.date,
            venue = entity.venue,
            theme = entity.theme,
            roleCounts = entity.roleCounts,
            createdAt = entity.createdAt,
            startTime = entity.startTime,
            endTime = entity.endTime,
            status = entity.status
        )
    }

    override fun mapToEntity(dto: MeetingDto): MeetingEntity {
        return MeetingEntity(
            meetingID = dto.meetingID,
            date = dto.date,
            venue = dto.venue,
            theme = dto.theme,
            roleCounts = dto.roleCounts,
            createdAt = dto.createdAt,
            startTime = dto.startTime,
            endTime = dto.endTime,
            status = dto.status
        )
    }

    fun mapFromEntityList(entities: List<MeetingEntity>): List<MeetingDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<MeetingDto>): List<MeetingEntity> {
        return dtos.map { mapToEntity(it) }
    }
}
