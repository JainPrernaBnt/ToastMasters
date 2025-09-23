package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MeetingEntity
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import com.bntsoft.toastmasters.domain.model.Meeting
import android.util.Log
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MeetingDomainMapper @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // ---------------- Domain Mapping ----------------
    fun mapToDomain(entity: MeetingEntity): Meeting {
        val dateTime = try {
            val date = LocalDate.parse(entity.date, dateFormatter)
            val startTime = LocalTime.parse(entity.startTime, timeFormatter)
            LocalDateTime.of(date, startTime)
        } catch (e: Exception) {
            Log.e("MeetingMapper", "Error parsing entity date/time: date=${entity.date}, time=${entity.startTime}", e)
            throw IllegalArgumentException("Invalid date/time format in database for meeting ${entity.meetingID}")
        }

        val endDateTime = if (entity.endTime.isNotBlank()) {
            try {
                val date = LocalDate.parse(entity.date, dateFormatter)
                val endTime = LocalTime.parse(entity.endTime, timeFormatter)
                LocalDateTime.of(date, endTime)
            } catch (e: Exception) {
                Log.e("MeetingMapper", "Error parsing entity end time: ${entity.endTime}", e)
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
            assignedCounts = entity.assignedCounts ?: emptyMap(),
            assignedRoles = entity.assignedRoles ?: emptyMap(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt ?: entity.createdAt,
            status = entity.status,
            isRecurring = entity.isRecurring ?: false,
            createdBy = entity.createdBy ?: ""
        )
    }

    fun mapToDomain(dto: MeetingDto): Meeting {
        val dateTime = dto.dateTime ?: try {
            val datePart = LocalDate.parse(dto.date, dateFormatter)
            val timePart = if (dto.startTime.isNotBlank()) {
                LocalTime.parse(dto.startTime, timeFormatter)
            } else LocalTime.NOON
            LocalDateTime.of(datePart, timePart)
        } catch (e: Exception) {
            Log.e("MeetingMapper", "Error parsing DTO date/time: date=${dto.date}, time=${dto.startTime}", e)
            LocalDateTime.now()
        }

        val endDateTime = dto.endDateTime ?: try {
            if (dto.endTime.isNotBlank()) {
                val datePart = LocalDate.parse(dto.date, dateFormatter)
                val endTime = LocalTime.parse(dto.endTime, timeFormatter)
                LocalDateTime.of(datePart, endTime)
            } else dateTime.plusHours(2)
        } catch (e: Exception) {
            Log.e("MeetingMapper", "Error parsing DTO end time: ${dto.endTime}", e)
            dateTime.plusHours(2)
        }

        // recompute assigned counts from roles
        val recomputedCounts = computeAssignedCounts(dto.assignedRoles ?: emptyMap())
        val finalAssignedCounts = dto.assignedCounts?.toMutableMap() ?: mutableMapOf()
        recomputedCounts.forEach { (role, count) -> finalAssignedCounts[role] = count }

        return Meeting(
            id = dto.meetingID,
            theme = dto.theme,
            dateTime = dateTime,
            endDateTime = endDateTime,
            location = dto.venue,
            roleCounts = dto.roleCounts ?: emptyMap(),
            assignedRoles = dto.assignedRoles ?: emptyMap(),
            assignedCounts = finalAssignedCounts,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            status = dto.status,
            isRecurring = dto.isRecurring ?: false,
            createdBy = dto.createdBy ?: ""
        )
    }

    // ---------------- Entity Mapping ----------------
    fun mapToEntity(meeting: Meeting): MeetingEntity {
        val recomputedCounts = computeAssignedCounts(meeting.assignedRoles)
        val finalCounts = meeting.assignedCounts.toMutableMap().apply {
            recomputedCounts.forEach { (role, count) -> this[role] = count }
        }

        return MeetingEntity(
            meetingID = meeting.id,
            date = meeting.dateTime.format(dateFormatter),
            startTime = meeting.dateTime.format(timeFormatter),
            endTime = meeting.endDateTime?.format(timeFormatter) ?: "",
            venue = meeting.location,
            theme = meeting.theme,
            roleCounts = meeting.roleCounts,
            assignedCounts = finalCounts,
            assignedRoles = meeting.assignedRoles,
            createdAt = meeting.createdAt,
            updatedAt = meeting.updatedAt,
            status = meeting.status,
            isRecurring = meeting.isRecurring,
            createdBy = meeting.createdBy.ifEmpty { null }
        )
    }

    // ---------------- DTO Mapping ----------------
    fun mapToDto(meeting: Meeting): MeetingDto {
        val recomputedCounts = computeAssignedCounts(meeting.assignedRoles)
        val finalCounts = meeting.assignedCounts.toMutableMap().apply {
            recomputedCounts.forEach { (role, count) -> this[role] = count }
        }

        return MeetingDto(
            meetingID = meeting.id,
            date = meeting.dateTime.format(dateFormatter),
            startTime = meeting.dateTime.format(timeFormatter),
            endTime = meeting.endDateTime?.format(timeFormatter) ?: "",
            venue = meeting.location,
            theme = meeting.theme,
            roleCounts = meeting.roleCounts,
            createdAt = meeting.createdAt,
            status = meeting.status,
            assignedRoles = meeting.assignedRoles,
            assignedCounts = finalCounts,
            updatedAt = meeting.updatedAt,
            isRecurring = meeting.isRecurring,
            createdBy = meeting.createdBy,
        )
    }

    // ---------------- Helpers ----------------
    private fun computeAssignedCounts(assignedRoles: Map<String, String>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        assignedRoles.forEach { (role, _) ->
            counts[role] = (counts[role] ?: 0) + 1
        }
        return counts
    }
}
