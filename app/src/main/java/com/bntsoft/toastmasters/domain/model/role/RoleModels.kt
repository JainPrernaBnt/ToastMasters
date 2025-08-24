package com.bntsoft.toastmasters.domain.model.role

import java.time.LocalDateTime

data class Role(
    val id: String,
    val name: String,
    val description: String = "",
    val isLeadership: Boolean = false,
    val isSpeaker: Boolean = false,
    val isEvaluator: Boolean = false
)

data class MemberRole(
    val id: String = "",
    val meetingId: String,
    val memberId: String,
    val roleId: String,
    val assignedAt: LocalDateTime = LocalDateTime.now(),
    val assignedBy: String = "",
    val status: String = "assigned",
    val notes: String = ""
)

data class MemberRolePreference(
    val memberId: String,
    val preferredRoles: List<String> = emptyList(),
    val unavailableRoles: List<String> = emptyList(),
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)

data class AssignRoleRequest(
    val meetingId: String,
    val memberId: String,
    val roleId: String,
    val assignedBy: String = "",
    val notes: String = ""
)

data class RoleAssignmentResponse(
    val assignmentId: String = "",
    val success: Boolean,
    val message: String = "",
    val assignment: MemberRole? = null
)
