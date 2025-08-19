package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.User
import kotlinx.coroutines.flow.Flow

interface MemberRepository {

    fun getPendingApprovals(): Flow<List<User>>

    suspend fun approveMember(userId: String, mentorNames: List<String>, isNewMember: Boolean): Boolean

    suspend fun rejectMember(userId: String, reason: String? = null): Boolean

    fun getAllMembers(includePending: Boolean = false): Flow<List<User>>

    fun getMentors(): Flow<List<User>>

    suspend fun updateMember(user: User): Boolean

    suspend fun getMemberById(userId: String): User?

    suspend fun getMemberByEmail(email: String): User?

    suspend fun getMemberByPhone(phoneNumber: String): User?

    fun observeMember(userId: String): Flow<User?>
}
