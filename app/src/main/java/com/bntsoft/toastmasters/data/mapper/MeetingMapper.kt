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
            venue = entity.venue,
            theme = entity.theme,
            preferredRoles = entity.preferredRoles,
            createdAt = entity.createdAt,
            isRecurring = entity.isRecurring,
            recurringDayOfWeek = entity.recurringDayOfWeek,
            recurringStartTime = entity.recurringStartTime,
            recurringEndTime = entity.recurringEndTime,
            startTime = entity.startTime,
            endTime = entity.endTime
        )
    }

    override fun mapToEntity(dto: MeetingDto): MeetingEntity {
        return MeetingEntity(
            meetingID = dto.meetingID,
            title = dto.title,
            date = dto.date,
            venue = dto.venue,
            theme = dto.theme,
            preferredRoles = dto.preferredRoles,
            createdAt = dto.createdAt,
            isRecurring = dto.isRecurring,
            recurringDayOfWeek = dto.recurringDayOfWeek,
            recurringStartTime = dto.recurringStartTime,
            recurringEndTime = dto.recurringEndTime,
            startTime = dto.startTime,
            endTime = dto.endTime
        )
    }

    fun mapFromEntityList(entities: List<MeetingEntity>): List<MeetingDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<MeetingDto>): List<MeetingEntity> {
        return dtos.map { mapToEntity(it) }
    }
}
