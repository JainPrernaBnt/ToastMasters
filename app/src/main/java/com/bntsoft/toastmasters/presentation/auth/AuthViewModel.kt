package com.bntsoft.toastmasters.presentation.auth

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.data.model.UserDeserializer
import com.bntsoft.toastmasters.domain.model.AuthResult
import com.bntsoft.toastmasters.domain.model.SignupResult
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.models.UserRole
import com.bntsoft.toastmasters.domain.repository.AuthRepository
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.presentation.auth.AuthViewModel.AuthUiState.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val firestoreService: FirestoreService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var pendingLoginEmail: String? = null
    private var pendingLoginPassword: String? = null

    fun login(identifier: String, password: String) {
        _uiState.value = AuthUiState.Loading
        pendingLoginEmail = if (identifier.contains("@")) identifier else null
        pendingLoginPassword = password

        viewModelScope.launch {
            try {
                when (val result = authRepository.login(identifier, password)) {
                    is AuthResult.Success -> {
                        val user = result.data
                        if (user.role == UserRole.VP_EDUCATION || (user.role == UserRole.MEMBER && user.isApproved)) {
                            _uiState.value = AuthUiState.Success(user, user.role)
                        } else {
                            _uiState.value = AuthUiState.Error("Your account is pending approval")
                            authRepository.logout()
                        }
                    }

                    is AuthResult.DeviceConflict -> {
                        _uiState.value = AuthUiState.DeviceConflict(
                            message = result.message,
                            email = result.email,
                            onContinue = { confirmLoginOnNewDevice(result.email, result.password) },
                            onCancel = { resetState() }
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
                _uiState.value = AuthUiState.Error("An unexpected error occurred")
            }
        }
    }
    
    private fun confirmLoginOnNewDevice(email: String, password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = AuthUiState.Loading
                
                // Re-authenticate the user
                val credential = EmailAuthProvider.getCredential(email, password)
                val user = FirebaseAuth.getInstance().currentUser
                user?.reauthenticate(credential)?.await()
                
                // If re-authentication is successful, update the session
                user?.let { firebaseUser ->
                    val deviceId = Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ) ?: UUID.randomUUID().toString()
                    
                    firestoreService.updateUserSession(firebaseUser.uid, deviceId)
                    
                    // Get the updated user data
                    val userDoc = firestoreService.getUserDocument(firebaseUser.uid).get().await()
                    val userData = userDoc.toObject(com.bntsoft.toastmasters.data.model.User::class.java)
                    
                    userData?.let { user ->
                        // Get the highest role if multiple exist, default to MEMBER
                        val userRole = user.roles.firstOrNull() ?: UserRole.MEMBER
                        
                        val domainUser = User(
                            id = user.id ?: "",
                            email = user.email ?: "",
                            name = user.name ?: "",
                            phoneNumber = user.phoneNumber ?: "",
                            role = userRole,
                            isApproved = user.status == com.bntsoft.toastmasters.data.model.User.Status.APPROVED
                        )
                        _uiState.value = AuthUiState.Success(domainUser, domainUser.role)
                    } ?: run {
                        _uiState.value = AuthUiState.Error("Failed to load user data")
                        authRepository.logout()
                    }
                } ?: run {
                    _uiState.value = AuthUiState.Error("User not found")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Authentication failed: ${e.message}")
                authRepository.logout()
            }
        }
    }

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
                        val userRole =
                            if (signedUpUser.isVpEducation) UserRole.VP_EDUCATION else UserRole.MEMBER
                        _uiState.value = SignUpSuccess(
                            user = signedUpUser,
                            userRole = userRole,
                            requiresApproval = signupResult.requiresApproval
                        )
                    }

                    is AuthResult.Error -> {
                        _uiState.value = Error(result.message)
                    }

                    AuthResult.Loading -> {
                        // Loading state is already set
                    }

                    is AuthResult.DeviceConflict<*> -> {
                        _uiState.value = AuthUiState.DeviceConflict(
                            message = result.message,
                            email = result.email,
                            onContinue = { confirmLoginOnNewDevice(result.email, result.password) },
                            onCancel = { resetState() }
                        )
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

    fun sendPasswordResetEmail(email: String, onComplete: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            try {
                val success = authRepository.sendPasswordResetEmail(email)
                if (success) {
                    onComplete(Result.success(true))
                } else {
                    onComplete(Result.failure(Exception("Failed to send password reset email")))
                }
            } catch (e: Exception) {
                onComplete(Result.failure(e))
            }
        }
    }

    sealed class AuthUiState {
        object Initial : AuthUiState()
        object Loading : AuthUiState()
        data class Error(val message: String) : AuthUiState()
        data class Success(val user: User, val userRole: UserRole) : AuthUiState()
        data class SignUpSuccess(
            val user: User,
            val userRole: UserRole,
            val requiresApproval: Boolean
        ) : AuthUiState()
        data class DeviceConflict(
            val message: String,
            val email: String,
            val onContinue: () -> Unit,
            val onCancel: () -> Unit
        ) : AuthUiState()
    }
}
