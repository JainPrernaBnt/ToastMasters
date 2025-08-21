package com.bntsoft.toastmasters.domain.model

data class MeetingAvailability(
    val userId: String = "",
    val meetingId: String = "",
    val isAvailable: Boolean = false,
    val preferredRoles: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
