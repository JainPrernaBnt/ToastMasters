package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.AuthResult
import com.bntsoft.toastmasters.domain.model.SignupResult
import com.bntsoft.toastmasters.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun login(identifier: String, password: String): AuthResult<User>

    suspend fun signUp(user: User, password: String): AuthResult<SignupResult>

    suspend fun getCurrentUser(): User?

    suspend fun logout()

    suspend fun userExists(email: String, phone: String): Boolean

    suspend fun sendPasswordResetEmail(email: String): Boolean

    suspend fun updateFcmToken(userId: String, token: String)

    fun observeAuthState(): Flow<User?>
}
