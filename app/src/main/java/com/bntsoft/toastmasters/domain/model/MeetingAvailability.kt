package com.bntsoft.toastmasters.domain.model

enum class AvailabilityStatus {
    AVAILABLE,
    NOT_AVAILABLE,
    NOT_CONFIRMED
}

data class MeetingAvailability(
    val userId: String = "",
    val meetingId: String = "",
    val status: AvailabilityStatus = AvailabilityStatus.NOT_AVAILABLE,
    val preferredRoles: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    // Helper property for backward compatibility
    val isAvailable: Boolean
        get() = status == AvailabilityStatus.AVAILABLE
}
