package com.bntsoft.toastmasters.domain.model

data class MemberResponse(
    val id: String = "",
    val meetingId: String,
    val memberId: String,
    val availability: AvailabilityStatus,
    val preferredRoles: List<String>,
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {

    enum class AvailabilityStatus {
        AVAILABLE,

        NOT_AVAILABLE,

        NOT_CONFIRMED;

        companion object {
            fun fromString(value: String): AvailabilityStatus {
                return values().find { it.name == value } ?: NOT_CONFIRMED
            }
        }
    }

    companion object {
        fun defaultForMeeting(meetingId: String, memberId: String): MemberResponse {
            return MemberResponse(
                meetingId = meetingId,
                memberId = memberId,
                availability = AvailabilityStatus.NOT_CONFIRMED,
                preferredRoles = emptyList()
            )
        }
    }
}
