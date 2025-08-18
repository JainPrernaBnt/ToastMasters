package com.bntsoft.toastmasters.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.UserRole
import com.bntsoft.toastmasters.domain.model.AuthResult
import com.bntsoft.toastmasters.domain.model.SignupResult
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.AuthRepository
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(identifier: String, password: String) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                when (val result = authRepository.login(identifier, password)) {
                    is AuthResult.Success -> {
                        val user = result.data
                        if (user.isVpEducation || user.isApproved) {
                            val userRole = if (user.isVpEducation) UserRole.VP_EDUCATION else UserRole.MEMBER
                            _uiState.value = AuthUiState.Success(user, userRole)
                        } else {
                            _uiState.value = AuthUiState.Error("Your account is pending approval")
                            authRepository.logout()
                        }
                    }

                    is AuthResult.Error -> {
                        _uiState.value = AuthUiState.Error(result.message)
                    }

                    AuthResult.Loading -> {
                        // Loading state is already set
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("An unexpected error occurred")
            }
        }
    }

    /**
     * Handles user registration.
     */
    fun signUp(user: User, password: String) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                // Check if user already exists
                if (authRepository.userExists(user.email, user.phoneNumber)) {
                    _uiState.value =
                        AuthUiState.Error("A user with this email or phone already exists")
                    return@launch
                }

                when (val result = authRepository.signUp(user, password)) {
                    is AuthResult.Success -> {
                        val signupResult: SignupResult = result.data
                        val signedUpUser = signupResult.user
                        val userRole = if (signedUpUser.isVpEducation) UserRole.VP_EDUCATION else UserRole.MEMBER
                        _uiState.value = AuthUiState.SignUpSuccess(
                            user = signedUpUser,
                            userRole = userRole,
                            requiresApproval = signupResult.requiresApproval
                        )
                    }

                    is AuthResult.Error -> {
                        _uiState.value = AuthUiState.Error(result.message)
                    }

                    AuthResult.Loading -> {
                        // Loading state is already set
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Failed to create account")
            }
        }
    }


    fun resetState() {
        _uiState.value = AuthUiState.Initial
    }

    fun sendPasswordResetEmail(email: String, onComplete: (kotlin.Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            try {
                val success = authRepository.sendPasswordResetEmail(email)
                if (success) {
                    onComplete(kotlin.Result.success(true))
                } else {
                    onComplete(kotlin.Result.failure(Exception("Failed to send password reset email")))
                }
            } catch (e: Exception) {
                onComplete(kotlin.Result.failure(e))
            }
        }
    }

    sealed class AuthUiState {
        object Initial : AuthUiState()
        object Loading : AuthUiState()
        data class Error(val message: String) : AuthUiState()
        data class Success(val user: User, val userRole: UserRole) : AuthUiState()
        data class SignUpSuccess(val user: User, val userRole: UserRole, val requiresApproval: Boolean) : AuthUiState()
    }
}
