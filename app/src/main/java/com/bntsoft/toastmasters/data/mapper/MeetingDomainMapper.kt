package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MeetingEntity
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import com.bntsoft.toastmasters.domain.model.Meeting
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MeetingDomainMapper @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun mapToDomain(entity: MeetingEntity): Meeting {
        val dateTime = try {
            // Parse date and time separately and then combine
            val date = LocalDate.parse(entity.date, dateFormatter)
            val startTime = LocalTime.parse(entity.startTime, timeFormatter)
            LocalDateTime.of(date, startTime)
        } catch (e: Exception) {
            Timber.e(
                e,
                "Error parsing entity date/time: date=${entity.date}, time=${entity.startTime}"
            )
            throw IllegalArgumentException("Invalid date/time format in database for meeting ${entity.meetingID}")
        }
        
        val endDateTime = if (entity.endTime.isNotEmpty() && entity.endTime.isNotBlank()) {
            try {
                val date = LocalDate.parse(entity.date, dateFormatter)
                val endTime = LocalTime.parse(entity.endTime, timeFormatter)
                LocalDateTime.of(date, endTime)
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
            roleCounts = entity.roleCounts ?: emptyMap(),
            createdAt = entity.createdAt,
            updatedAt = entity.createdAt, // Using created time as updated time if not available
            status = entity.status
        )
    }

    fun mapToDomain(dto: MeetingDto): Meeting {
        // First try to use the pre-parsed dateTime and endDateTime if available
        val dateTime = dto.dateTime ?: try {
            // Fall back to parsing date and time separately
            val datePart = LocalDate.parse(dto.date, dateFormatter)
            val timePart = if (dto.startTime.isNotBlank()) {
                LocalTime.parse(dto.startTime, timeFormatter)
            } else {
                LocalTime.NOON // Default to noon if no time provided
            }
            LocalDateTime.of(datePart, timePart)
        } catch (e: Exception) {
            Timber.e(e, "Error parsing date/time from dto: date=${dto.date}, time=${dto.startTime}")
            LocalDateTime.now()
        }

        // Parse endDateTime if available, otherwise use start time + 2 hours as default
        val endDateTime = dto.endDateTime ?: try {
            if (dto.endTime.isNotBlank()) {
                val datePart = LocalDate.parse(dto.date, dateFormatter)
                val endTime = LocalTime.parse(dto.endTime, timeFormatter)
                LocalDateTime.of(datePart, endTime)
            } else {
                dateTime.plusHours(2) // Default to 2 hours after start time
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing end time from dto: ${dto.endTime}")
            dateTime.plusHours(2) // Default to 2 hours after start time
        }

        return Meeting(
            id = dto.meetingID,
            theme = dto.theme,
            dateTime = dateTime,
            endDateTime = endDateTime,
            location = dto.venue,
            roleCounts = dto.roleCounts ?: emptyMap(),
            createdAt = dto.createdAt,
            updatedAt = dto.createdAt, // Using created time as updated time if not available
            status = dto.status
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
            roleCounts = meeting.roleCounts,
            createdAt = meeting.createdAt,
            status = meeting.status
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
            roleCounts = meeting.roleCounts,
            createdAt = meeting.createdAt,
            status = meeting.status
        )
    }
}
