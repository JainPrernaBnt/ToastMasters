package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.model.role.AssignRoleRequest
import com.bntsoft.toastmasters.domain.model.role.MemberRole
import com.bntsoft.toastmasters.domain.model.role.Role
import com.bntsoft.toastmasters.domain.model.role.RoleAssignmentResponse
import com.bntsoft.toastmasters.utils.Resource
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MeetingRepository {
    // Basic meeting operations
    fun getAllMeetings(): Flow<List<Meeting>>
    fun getUpcomingMeetings(afterDate: LocalDate = LocalDate.now()): Flow<List<Meeting>>
    suspend fun getMeetingById(id: String): Meeting?
    suspend fun createMeeting(meeting: Meeting): Resource<Meeting>
    suspend fun updateMeeting(meeting: Meeting): Resource<Unit>
    suspend fun deleteMeeting(id: String): Resource<Unit>
    suspend fun completeMeeting(meetingId: String): Resource<Unit>
    fun getUpcomingMeetingsWithCounts(afterDate: LocalDate = LocalDate.now()): Flow<List<MeetingWithCounts>>
    suspend fun syncMeetings(): Resource<Unit>
    
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
