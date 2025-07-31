package com.bntsoft.toastmasters.data.model.dto

data class MeetingAvailabilityDto(
    val availabilityId: Int = 0,
    val meetingId: Int,
    val userId: Int,
    val preferredRole: String,
    val available: String,  // "yes", "no", "not sure"
    val submittedOn: String // e.g., "2025-07-30 14:50"
)
