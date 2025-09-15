package com.bntsoft.toastmasters.domain.model

import android.os.Parcelable
import com.bntsoft.toastmasters.domain.models.AvailabilityStatus
import kotlinx.parcelize.Parcelize

@Parcelize
data class MeetingAvailability(
    val userId: String = "",
    val meetingId: String = "",
    val status: AvailabilityStatus = AvailabilityStatus.NOT_AVAILABLE,
    val preferredRoles: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isBackout: Boolean = false
): Parcelable {
    // Helper property for backward compatibility
    val isAvailable: Boolean
        get() = status == AvailabilityStatus.AVAILABLE
}
