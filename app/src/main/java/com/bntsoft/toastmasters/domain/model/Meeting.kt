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
    val officers: Map<String, String> = emptyMap(), // Map of officer roles to member names
    val isRecurring: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val availability: MeetingAvailability? = null,
    val isEditMode: Boolean = false,
    val status: MeetingStatus = MeetingStatus.NOT_COMPLETED,
    val agendaId: String = "", // ID of the associated agenda
    // Club Information
    val clubName: String = "",
    val clubNumber: String = "",
    val district: String = "",
    val area: String = "",
    val mission: String = ""
): Parcelable {
    // For backward compatibility
    val preferredRoles: List<String> get() = roleCounts.keys.toList()

    fun toggleEditMode() = copy(isEditMode = !isEditMode)
}
