package com.bntsoft.toastmasters.data.source.remote

import com.bntsoft.toastmasters.data.model.role.*

/**
 * Data source interface for role-related remote operations
 */
interface RoleRemoteDataSource {
    // Role operations
    suspend fun getAllRoles(): List<RoleDto>
    suspend fun getRoleById(roleId: String): RoleDto?
    suspend fun getRolesByIds(roleIds: List<String>): List<RoleDto>
    
    // Role assignment operations
    suspend fun assignRole(request: AssignRoleRequestDto): RoleAssignmentResponseDto
    suspend fun getMemberRoles(memberId: String): List<MemberRoleDto>
    suspend fun getMeetingRoles(meetingId: String): List<MemberRoleDto>
    suspend fun getMeetingRoleAssignments(meetingId: String): List<MemberRoleDto>
    suspend fun getMemberRoleForMeeting(memberId: String, meetingId: String): MemberRoleDto?
    suspend fun removeRoleAssignment(assignmentId: String)
    
    // Member preferences
    suspend fun getMemberPreferences(memberId: String): MemberRolePreferenceDto
    suspend fun updateMemberPreferences(
        memberId: String,
        preferredRoles: List<String>?,
        unavailableRoles: List<String>?
    )
    
    // Role history and statistics
    suspend fun getMemberRoleHistory(memberId: String, limit: Int): List<MemberRoleDto>
    suspend fun getRoleStatistics(roleId: String): Map<String, Any>
    
    // Batch operations
    suspend fun assignMultipleRoles(requests: List<AssignRoleRequestDto>): List<RoleAssignmentResponseDto>
    
    // Role templates
    suspend fun getRoleTemplates(): Map<String, List<String>>
    suspend fun applyRoleTemplate(meetingId: String, templateId: String)
}
