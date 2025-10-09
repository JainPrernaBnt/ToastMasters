package com.bntsoft.toastmasters.domain.model

data class MemberResponse(
    val id: String = "",
    val meetingId: String,
    val memberId: String,
    val userId: String = "",
    val availability: AvailabilityStatus,
    val preferredRoles: List<String>,
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val backoutTimestamp: Long? = null
) {

    enum class AvailabilityStatus {
        AVAILABLE,
        NOT_AVAILABLE,
        NOT_CONFIRMED,
        NOT_RESPONDED;

        companion object {
            fun fromString(value: String): AvailabilityStatus {
                return values().find { it.name == value } ?: NOT_CONFIRMED
            }
        }
    }

}
