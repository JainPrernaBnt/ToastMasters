package com.bntsoft.toastmasters.data.model.dto

import com.bntsoft.toastmasters.domain.models.MeetingStatus
import java.time.LocalDateTime

data class MeetingDto(
    val meetingID: String,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val dateTime: LocalDateTime? = null,
    val endDateTime: LocalDateTime? = null,
    val venue: String = "",
    val theme: String = "No Theme",
    val roleCounts: Map<String, Int> = emptyMap(),
    val assignedCounts: Map<String, Int> = emptyMap(),
    val createdAt: Long = 0,
    val assignedRoles: Map<String, String> = emptyMap(),
    val status: MeetingStatus = MeetingStatus.NOT_COMPLETED,
    val updatedAt: Long = 0,
    val agendaId: String = "",
    val isRecurring: Boolean = false,
    val createdBy: String = "",
    val officers: Map<String, String> = emptyMap()
) {
    constructor(
        meetingID: String,
        date: String = "",
        startTime: String = "",
        endTime: String = "",
        venue: String = "",
        theme: String = "No Theme",
        roleCounts: Map<String, Int> = emptyMap(),
        assignedCounts: Map<String, Int> = emptyMap(),
        createdAt: Long = 0,
        status: MeetingStatus = MeetingStatus.NOT_COMPLETED,
        assignedRoles: Map<String, String> = emptyMap(),
        updatedAt: Long = 0,
        agendaId: String = "",
        isRecurring: Boolean = false,
        createdBy: String = "",
        officers: Map<String, String> = emptyMap()
    ) : this(
        meetingID = meetingID,
        date = date,
        startTime = startTime,
        endTime = endTime,
        dateTime = null,
        endDateTime = null,
        venue = venue,
        theme = theme,
        roleCounts = roleCounts,
        assignedCounts = assignedCounts,
        createdAt = createdAt,
        status = status,
        assignedRoles = assignedRoles,
        updatedAt = updatedAt,
        agendaId = agendaId,
        isRecurring = isRecurring,
        createdBy = createdBy,
        officers = officers
    )
}
