package com.bntsoft.toastmasters.domain.model

import android.os.Parcelable
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
data class Meeting(
    val id: String = "",
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val endDateTime: LocalDateTime? = null,
    val location: String = "",
    val theme: String = "",
    val roleCounts: Map<String, Int> = emptyMap(), // e.g. "Speaker" to 2, "General Evaluator" to 2
    val assignedRoles: Map<String, String> = emptyMap(), // Map of role name to member ID
    val assignedCounts: Map<String, Int> = emptyMap(), // Map of role name to count of assigned members
    val isRecurring: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val availability: MeetingAvailability? = null,
    val isEditMode: Boolean = false,
    val status: MeetingStatus = MeetingStatus.NOT_COMPLETED
): Parcelable {
    // For backward compatibility
    val preferredRoles: List<String> get() = roleCounts.keys.toList()

    fun toggleEditMode() = copy(isEditMode = !isEditMode)
}
