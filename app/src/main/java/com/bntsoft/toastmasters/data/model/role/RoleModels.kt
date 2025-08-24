package com.bntsoft.toastmasters.data.model.role

import java.time.LocalDateTime

/**
 * Data Transfer Object for Role
 */
data class RoleDto(
    val id: String,
    val name: String,
    val description: String = "",
    val isLeadership: Boolean = false,
    val isSpeaker: Boolean = false,
    val isEvaluator: Boolean = false
)

/**
 * Data Transfer Object for MemberRole
 */
data class MemberRoleDto(
    val id: String,
    val meetingId: String,
    val memberId: String,
    val roleId: String,
    val assignedAt: LocalDateTime = LocalDateTime.now(),
    val notes: String = ""
)

/**
 * Data Transfer Object for MemberRolePreference
 */
data class MemberRolePreferenceDto(
    val memberId: String,
    val preferredRoles: List<String> = emptyList(),
    val unavailableRoles: List<String> = emptyList(),
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)

/**
 * Data Transfer Object for AssignRoleRequest
 */
data class AssignRoleRequestDto(
    val meetingId: String,
    val memberId: String,
    val roleId: String,
    val notes: String = ""
)

/**
 * Data Transfer Object for RoleAssignmentResponse
 */
data class RoleAssignmentResponseDto(
    val success: Boolean,
    val message: String = "",
    val assignment: MemberRoleDto? = null
)

/**
 * Data class for role statistics
 */
data class RoleStatisticsDto(
    val roleId: String,
    val totalAssignments: Int,
    val uniqueMembers: Int,
    val lastAssigned: LocalDateTime?,
    val averagePerMeeting: Double
)

/**
 * Data class for role template
 */
data class RoleTemplateDto(
    val id: String,
    val name: String,
    val description: String,
    val roleAssignments: Map<String, String> // roleId to roleName
)
