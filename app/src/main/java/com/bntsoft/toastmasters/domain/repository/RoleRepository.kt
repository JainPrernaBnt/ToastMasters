package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.domain.model.role.Role
import com.bntsoft.toastmasters.domain.model.role.MemberRole
import com.bntsoft.toastmasters.domain.model.role.MemberRolePreference
import com.bntsoft.toastmasters.domain.model.role.AssignRoleRequest
import com.bntsoft.toastmasters.domain.model.role.RoleAssignmentResponse
import kotlinx.coroutines.flow.Flow

interface RoleRepository {
    
    // Role management
    suspend fun getAllRoles(): Result<List<Role>>
    suspend fun getRoleById(roleId: String): Result<Role>
    suspend fun getRolesByIds(roleIds: List<String>): Result<List<Role>>
    
    // Role assignments
    suspend fun assignRole(request: AssignRoleRequest): Result<RoleAssignmentResponse>
    suspend fun getMemberRoles(memberId: String): Result<List<MemberRole>>
    suspend fun getMeetingRoles(meetingId: String): Result<List<MemberRole>>
    suspend fun getMemberRoleForMeeting(memberId: String, meetingId: String): Result<MemberRole?>
    suspend fun getMeetingRoleAssignments(meetingId: String): Result<List<MemberRole>>
    suspend fun removeRoleAssignment(assignmentId: String): Result<Unit>
    
    // Member preferences
    suspend fun getMemberPreferences(memberId: String): Result<MemberRolePreference>
    suspend fun updateMemberPreferences(
        memberId: String, 
        preferredRoles: List<String>? = null, 
        unavailableRoles: List<String>? = null
    ): Result<Unit>
    
    // Role history and statistics
    suspend fun getMemberRoleHistory(memberId: String, limit: Int = 10): Result<List<MemberRole>>
    suspend fun getRoleStatistics(roleId: String): Result<Map<String, Any>>
    
    // Batch operations
    suspend fun assignMultipleRoles(requests: List<AssignRoleRequest>): Result<List<RoleAssignmentResponse>>
    
    // Role templates (for common meeting role sets)
    suspend fun getRoleTemplates(): Result<Map<String, List<String>>>
    suspend fun applyRoleTemplate(meetingId: String, templateId: String): Result<Unit>
}
