package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MeetingEntity
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import com.bntsoft.toastmasters.domain.model.Meeting
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MeetingDomainMapper @Inject constructor() {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    
    fun mapToDomain(entity: MeetingEntity): Meeting {
        val dateTime = LocalDateTime.parse("${entity.date}T${entity.startTime}")
        val endDateTime = if (entity.endTime.isNotEmpty()) entity.endTime?.let { LocalDateTime.parse("${entity.date}T$it") } else null
        
        return Meeting(
            id = entity.meetingID,
            theme = entity.theme,
            dateTime = dateTime,
            endDateTime = endDateTime,
            location = entity.venue,
            preferredRoles = entity.preferredRoles,
            isRecurring = entity.isRecurring,
            createdAt = entity.createdAt,
            updatedAt = entity.createdAt // Using created time as updated time if not available
        )
    }
    
    fun mapToDomain(dto: MeetingDto): Meeting {
        val dateTime = LocalDateTime.parse("${dto.date}T${dto.startTime}")
        val endDateTime = if (dto.endTime.isNotEmpty()) dto.endTime?.let { LocalDateTime.parse("${dto.date}T$it") } else null
        
        return Meeting(
            id = dto.meetingID,
            theme = dto.theme,
            dateTime = dateTime,
            endDateTime = endDateTime,
            location = dto.venue,
            preferredRoles = dto.preferredRoles,
            isRecurring = dto.isRecurring,
            createdAt = dto.createdAt,
            updatedAt = dto.createdAt // Using created time as updated time if not available
        )
    }
    
    fun mapToEntity(meeting: Meeting): MeetingEntity {
        return MeetingEntity(
            meetingID = meeting.id,
            date = meeting.dateTime.format(dateFormatter),
            startTime = meeting.dateTime.format(timeFormatter),
            endTime = meeting.endDateTime?.format(timeFormatter) ?: "",
            venue = meeting.location,
            theme = meeting.theme,
            preferredRoles = meeting.preferredRoles,
            createdAt = meeting.createdAt,
            isRecurring = meeting.isRecurring,
            recurringDayOfWeek = if (meeting.isRecurring) meeting.dateTime.dayOfWeek.value else null,
            recurringStartTime = if (meeting.isRecurring) meeting.dateTime.format(timeFormatter) else null,
            recurringEndTime = if (meeting.isRecurring) meeting.endDateTime?.format(timeFormatter) else null
        )
    }
    
    fun mapToDto(meeting: Meeting): MeetingDto {
        return MeetingDto(
            meetingID = meeting.id,
            date = meeting.dateTime.format(dateFormatter),
            startTime = meeting.dateTime.format(timeFormatter),
            endTime = meeting.endDateTime?.format(timeFormatter) ?: "",
            venue = meeting.location,
            theme = meeting.theme,
            preferredRoles = meeting.preferredRoles,
            createdAt = meeting.createdAt,
            isRecurring = meeting.isRecurring,
            recurringDayOfWeek = if (meeting.isRecurring) meeting.dateTime.dayOfWeek.value else null,
            recurringStartTime = if (meeting.isRecurring) meeting.dateTime.format(timeFormatter) else null,
            recurringEndTime = if (meeting.isRecurring) meeting.endDateTime?.format(timeFormatter) else null
        )
    }
}
