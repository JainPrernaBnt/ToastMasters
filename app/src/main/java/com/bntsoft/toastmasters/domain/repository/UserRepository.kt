package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getCurrentUser(): User?
    fun observeCurrentUser(): Flow<User?>
    suspend fun getUserById(userId: String): Result<User>
    suspend fun getUserByEmail(email: String): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun updateUserRole(userId: String, role: String): Result<Unit>
    suspend fun updateUserStatus(userId: String, isActive: Boolean): Result<Unit>
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit>
    suspend fun removeFcmToken(userId: String): Result<Unit>

//    Assign roles
    suspend fun getAvailableMembers(meetingId: String): List<com.bntsoft.toastmasters.data.model.User>
    suspend fun getRecentRoles(userId: String, limit: Int): List<String>
    suspend fun getAllApprovedUsers(): List<User>
}
