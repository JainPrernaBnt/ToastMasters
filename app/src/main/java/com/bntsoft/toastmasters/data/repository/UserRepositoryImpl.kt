package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.remote.UserService
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userService: UserService
) : UserRepository {

    override suspend fun getCurrentUser(): User? {
        return try {
            userService.getCurrentUser()
        } catch (e: Exception) {
            null
        }
    }

    override fun observeCurrentUser(): Flow<User?> = flow {
        try {
            val user = userService.getCurrentUser()
            emit(user)
        } catch (e: Exception) {
            emit(null)
        }
    }

    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            val user = userService.getUserById(userId)
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getUserByEmail(email: String): Result<User> {
        return try {
            val user = userService.getUserByEmail(email)
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            userService.updateUser(user)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            userService.deleteUser(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val users = userService.searchUsers(query)
            Result.Success(users)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateUserRole(userId: String, role: String): Result<Unit> {
        return try {
            userService.updateUserRole(userId, role)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateUserStatus(userId: String, isActive: Boolean): Result<Unit> {
        return try {
            userService.updateUserStatus(userId, isActive)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            userService.updateFcmToken(userId, token)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeFcmToken(userId: String): Result<Unit> {
        return try {
            userService.removeFcmToken(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAvailableMembers(meetingId: String): List<com.bntsoft.toastmasters.data.model.User> {
        return try {
            // Fetch all users who marked themselves as available for this meeting
            userService.getAvailableMembersForMeeting(meetingId).map { domainUser ->
                com.bntsoft.toastmasters.data.model.User(
                    id = domainUser.id,
                    name = domainUser.name,
                    email = domainUser.email,
                    phoneNumber = domainUser.phoneNumber,
                    address = "",
                    gender = domainUser.gender,
                    joinedDate = domainUser.joinedDate,
                    toastmastersId = domainUser.toastmastersId,
                    clubId = domainUser.clubId,
                    profileImageUrl = "",
                    fcmToken = domainUser.fcmToken ?: "",
                    mentorNames = domainUser.mentorNames,
                    roles = listOf(domainUser.role),
                    status = if (domainUser.isApproved) 
                        com.bntsoft.toastmasters.data.model.User.Status.APPROVED 
                        else com.bntsoft.toastmasters.data.model.User.Status.PENDING_APPROVAL,
                    lastLogin = null,
                    createdAt = domainUser.createdAt,
                    updatedAt = domainUser.updatedAt
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching available members for meeting $meetingId")
            emptyList()
        }
    }

    override suspend fun getRecentRoles(userId: String, limit: Int): List<String> {
        return try {
            // Fetch user's recent roles from past meetings
            // Path: users/{userId}/recentRoles (limited by the 'limit' parameter)
            userService.getUserRecentRoles(userId, limit) ?: emptyList()
        } catch (e: Exception) {
            // Log the error and return empty list
            Timber.e(e, "Error fetching recent roles for user $userId")
            emptyList()
        }
    }

}
