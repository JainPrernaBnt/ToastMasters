package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.model.role.MemberRole
import com.bntsoft.toastmasters.domain.model.role.Role
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    // Member management
    fun getPendingApprovals(): Flow<List<User>>
    suspend fun approveMember(userId: String, mentorNames: List<String>): Boolean
    suspend fun rejectMember(userId: String, reason: String? = null): Boolean
    fun getAllMembers(includePending: Boolean = false): Flow<List<User>>
    fun getMentors(): Flow<List<User>>
    suspend fun updateMember(user: User): Boolean
    suspend fun getMemberById(userId: String): User?
    suspend fun getMemberByEmail(email: String): User?
    suspend fun getMemberByPhone(phoneNumber: String): User?
    fun observeMember(userId: String): Flow<User?>
    
    // Role-related operations
    suspend fun getMemberRoles(memberId: String): Result<List<MemberRole>>
    suspend fun getMemberRoleHistory(memberId: String, limit: Int = 10): Result<List<MemberRole>>
    suspend fun getMembersForRole(roleId: String, limit: Int = 20): Result<List<User>>
    
    // Role preferences
    suspend fun getMemberRolePreferences(memberId: String): Result<Set<String>>
    suspend fun updateMemberRolePreference(
        memberId: String, 
        preferredRole: String, 
        isPreferred: Boolean
    ): Result<Unit>
    
    // Role availability
    suspend fun getAvailableMembersForRole(roleId: String, meetingId: String): Result<List<User>>
    suspend fun getMemberAvailabilityForRole(
        memberId: String, 
        roleId: String, 
        meetingId: String
    ): Result<Boolean>
    
    // Role statistics
    suspend fun getMemberRoleStatistics(memberId: String): Result<Map<String, Int>>
    suspend fun getClubRoleStatistics(): Result<Map<String, Int>>
}
