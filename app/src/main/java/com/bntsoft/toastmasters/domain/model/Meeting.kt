package com.bntsoft.toastmasters.domain.model

import android.os.Parcelable
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.time.LocalDateTime

@Parcelize
data class Meeting(
    val id: String = "",
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val endDateTime: LocalDateTime? = null,
    val location: String = "",
    val theme: String = "",
    val roleCounts: Map<String, Int> = emptyMap(),
    val assignedRoles: Map<String, String> = emptyMap(),
    val assignedCounts: Map<String, Int> = emptyMap(),
    val officers: Map<String, String> = emptyMap(),
    val isRecurring: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val availability: MeetingAvailability? = null,
    val isEditMode: Boolean = false,
    val status: MeetingStatus = MeetingStatus.NOT_COMPLETED,
    val agendaId: String = "",
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