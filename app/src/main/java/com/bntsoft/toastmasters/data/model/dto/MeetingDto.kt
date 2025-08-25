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
    val preferredRoles: List<String> = emptyList(),
    val createdAt: Long = 0,
    val status: MeetingStatus = MeetingStatus.NOT_COMPLETED
)
