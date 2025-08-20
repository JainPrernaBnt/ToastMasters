package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MeetingEntity
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import com.bntsoft.toastmasters.domain.model.Meeting
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime

class MeetingDomainMapper @Inject constructor() {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    
    fun mapToDomain(entity: MeetingEntity): Meeting {
        val dateTime = try {
            LocalDateTime.parse("${entity.date}T${entity.startTime}", dateTimeFormatter)
        } catch (e: Exception) {
            Timber.e(e, "Error parsing entity date/time: date=${entity.date}, time=${entity.startTime}")
            throw IllegalArgumentException("Invalid date/time format in database for meeting ${entity.meetingID}")
        }
        val endDateTime = if (entity.endTime.isNotEmpty() && entity.endTime.isNotBlank()) {
            try {
                LocalDateTime.parse("${entity.date}T${entity.endTime}", dateTimeFormatter)
            } catch (e: Exception) {
                Timber.e(e, "Error parsing entity end time: ${entity.endTime}")
                null
            }
        } else null
        
        return Meeting(
            id = entity.meetingID,
            theme = entity.theme,
            dateTime = dateTime,
            endDateTime = endDateTime,
            location = entity.venue,
            availableRoles = entity.preferredRoles,
            isRecurring = entity.isRecurring,
            createdAt = entity.createdAt,
            updatedAt = entity.createdAt // Using created time as updated time if not available
        )
    }
    
    fun mapToDomain(dto: MeetingDto): Meeting {
        // Use current time as fallback
        val fallbackTime = LocalDateTime.now()
        
        val dateTime = try {
            val datePart = if (dto.date.isNotBlank()) {
                LocalDate.parse(dto.date, dateFormatter)
            } else {
                fallbackTime.toLocalDate()
            }
            
            val timePart = if (dto.startTime.isNotBlank()) {
                try {
                    LocalTime.parse(dto.startTime, timeFormatter)
                } catch (e: Exception) {
                    LocalTime.NOON // Default to noon if time parsing fails
                }
            } else {
                LocalTime.NOON // Default to noon if no time provided
            }
            
            LocalDateTime.of(datePart, timePart)
        } catch (e: Exception) {
            fallbackTime
        }
        
        // Don't care about end time for now, just use start time + 1 hour
        val endDateTime = dateTime.plusHours(1)
        
        return Meeting(
            id = dto.meetingID,
            theme = dto.theme,
            dateTime = dateTime,
            endDateTime = endDateTime,
            location = dto.venue,
            availableRoles = dto.preferredRoles,
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
