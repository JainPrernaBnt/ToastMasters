package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.role.AssignRoleRequest
import com.bntsoft.toastmasters.domain.model.role.MemberRole
import com.bntsoft.toastmasters.domain.model.role.Role
import com.bntsoft.toastmasters.domain.model.role.RoleAssignmentResponse
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FirebaseMeetingDataSource {

    fun getAllMeetings(): Flow<List<Meeting>>

    fun getUpcomingMeetings(afterDate: LocalDate = LocalDate.now()): Flow<List<Meeting>>

    suspend fun getMeetingById(id: String): Meeting?

    suspend fun createMeeting(meeting: Meeting): Result<Meeting>

    suspend fun updateMeeting(meeting: Meeting): Result<Unit>

    suspend fun deleteMeeting(id: String): Result<Unit>

    suspend fun completeMeeting(meetingId: String): Result<Unit>

    suspend fun sendMeetingNotification(meeting: Meeting): Result<Unit>
    
    // Role assignment operations
    suspend fun assignRole(request: AssignRoleRequest): Result<RoleAssignmentResponse>
    suspend fun getAssignedRoles(meetingId: String): Result<List<MemberRole>>
    suspend fun getAssignedRole(meetingId: String, memberId: String): Result<MemberRole?>
    suspend fun removeRoleAssignment(assignmentId: String): Result<Unit>
    suspend fun updateRoleAssignment(assignment: MemberRole): Result<Unit>
    
    // Role availability
    suspend fun getAvailableRoles(meetingId: String): Result<List<Role>>
    suspend fun getMembersForRole(meetingId: String, roleId: String): Result<List<String>>
    
    // Batch operations
    suspend fun assignMultipleRoles(requests: List<AssignRoleRequest>): Result<List<RoleAssignmentResponse>>
    suspend fun getMeetingRoleAssignments(meetingId: String): Result<Map<String, MemberRole>>
    
    // Role statistics
    suspend fun getMeetingRoleStatistics(meetingId: String): Result<Map<String, Any>>
    
    // Role templates
    suspend fun applyRoleTemplate(meetingId: String, templateId: String): Result<Unit>
    suspend fun getAvailableRoleTemplates(): Result<Map<String, String>>
}
