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
        val endDateTime = entity.endTime?.let { LocalDateTime.parse("${entity.date}T$it") }
        
        return Meeting(
            id = entity.meetingID,
            title = entity.title,
            dateTime = dateTime,
            endDateTime = endDateTime,
            location = entity.venue,
            isRecurring = entity.isRecurring,
            createdAt = entity.createdAt,
            updatedAt = entity.createdAt // Using created time as updated time if not available
        )
    }
    
    fun mapToDomain(dto: MeetingDto): Meeting {
        val dateTime = LocalDateTime.parse("${dto.date}T${dto.startTime}")
        val endDateTime = dto.endTime?.let { LocalDateTime.parse("${dto.date}T$it") }
        
        return Meeting(
            id = dto.meetingID,
            title = dto.title,
            dateTime = dateTime,
            endDateTime = endDateTime,
            location = dto.venue,
            isRecurring = dto.isRecurring,
            createdAt = dto.createdAt,
            updatedAt = dto.createdAt // Using created time as updated time if not available
        )
    }
    
    fun mapToEntity(meeting: Meeting): MeetingEntity {
        return MeetingEntity(
            meetingID = meeting.id,
            title = meeting.title,
            date = meeting.dateTime.format(dateFormatter),
            startTime = meeting.dateTime.format(timeFormatter),
            endTime = meeting.endDateTime?.format(timeFormatter) ?: "",
            venue = meeting.location,
            theme = "", // Not in domain model
            preferredRoles = emptyList(), // Not in domain model
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
            title = meeting.title,
            date = meeting.dateTime.format(dateFormatter),
            startTime = meeting.dateTime.format(timeFormatter),
            endTime = meeting.endDateTime?.format(timeFormatter) ?: "",
            venue = meeting.location,
            theme = "", // Not in domain model
            preferredRoles = emptyList(), // Not in domain model
            createdAt = meeting.createdAt,
            isRecurring = meeting.isRecurring,
            recurringDayOfWeek = if (meeting.isRecurring) meeting.dateTime.dayOfWeek.value else null,
            recurringStartTime = if (meeting.isRecurring) meeting.dateTime.format(timeFormatter) else null,
            recurringEndTime = if (meeting.isRecurring) meeting.endDateTime?.format(timeFormatter) else null
        )
    }
}
