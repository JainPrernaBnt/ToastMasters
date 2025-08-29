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
    val createdAt: Long = 0,
    val status: MeetingStatus = MeetingStatus.NOT_COMPLETED
) {
    constructor(
        meetingID: String,
        date: String = "",
        startTime: String = "",
        endTime: String = "",
        venue: String = "",
        theme: String = "No Theme",
        roleCounts: Map<String, Int> = emptyMap(),
        createdAt: Long = 0,
        status: MeetingStatus = MeetingStatus.NOT_COMPLETED
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
        createdAt = createdAt,
        status = status
    )
}
