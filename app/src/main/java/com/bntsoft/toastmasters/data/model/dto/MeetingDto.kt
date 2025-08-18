package com.bntsoft.toastmasters.data.model.dto

data class MeetingDto(
    val meetingID: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venue: String,
    val theme: String,
    val preferredRoles: List<String>,
    val createdAt: Long,
    val isRecurring: Boolean = true,
    val recurringDayOfWeek: Int? = null,
    val recurringStartTime: String? = null,
    val recurringEndTime: String? = null,
)
