package com.bntsoft.toastmasters.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MeetingWithRole(
    val meeting: Meeting,
    var assignedRole: String = "",
    val assignedRoles: List<String> = emptyList()
) : Parcelable {
    init {
        // Ensure assignedRole is set if assignedRoles has values but assignedRole is empty
        if (assignedRole.isEmpty() && assignedRoles.isNotEmpty()) {
            assignedRole = assignedRoles.first()
        }
    }
    
    // Secondary constructor for backward compatibility
    constructor(meeting: Meeting, role: String) : this(
        meeting = meeting,
        assignedRole = role,
        assignedRoles = if (role.isNotBlank()) listOf(role) else emptyList()
    )
}
