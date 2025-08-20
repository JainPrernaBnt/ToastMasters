package com.bntsoft.toastmasters.utils

import com.bntsoft.toastmasters.data.model.User
import com.bntsoft.toastmasters.domain.model.User as DomainUser
import com.bntsoft.toastmasters.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManager @Inject constructor(
    private val userRepository: UserRepository
) {
    
    suspend fun getCurrentUserId(): String? {
        return userRepository.getCurrentUser()?.id
    }
    
    suspend fun getCurrentUser(): DomainUser? {
        return userRepository.getCurrentUser()
    }
    
    fun observeCurrentUser(): Flow<DomainUser?> {
        return userRepository.observeCurrentUser()
    }
    
    suspend fun isUserLoggedIn(): Boolean {
        return userRepository.getCurrentUser() != null
    }
    
    suspend fun isUserApproved(): Boolean {
        return userRepository.getCurrentUser()?.isApproved ?: false
    }
    
    suspend fun getUserRole(): String? {
        return userRepository.getCurrentUser()?.role?.name
    }
    
    fun observeUserRole(): Flow<String?> {
        return userRepository.observeCurrentUser().map { it?.role?.name }
    }
    
    suspend fun isVpEducation(): Boolean {
        return getCurrentUser()?.isVpEducation ?: false
    }
}
