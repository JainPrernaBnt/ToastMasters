package com.bntsoft.toastmasters.data.model.dto

data class MeetingDto(
    val meetingID: String,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val venue: String = "",
    val theme: String = "No Theme",
    val preferredRoles: List<String> = emptyList(),
    val createdAt: Long = 0,
    val isRecurring: Boolean = false,
    val recurringDayOfWeek: Int? = null,
    val recurringStartTime: String? = null,
    val recurringEndTime: String? = null
)
