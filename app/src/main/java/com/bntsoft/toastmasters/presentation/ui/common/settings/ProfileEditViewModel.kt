package com.bntsoft.toastmasters.presentation.ui.common.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.models.UserRole
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.bntsoft.toastmasters.domain.repository.ProfileRepository
import com.bntsoft.toastmasters.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val user: User? = null,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val selectedImageUri: Uri? = null
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val user = userRepository.getCurrentUser()
                if (user != null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            user = user
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "Unable to load user profile"
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to load user profile"
                    ) 
                }
            }
        }
    }

    fun setSelectedImage(imageUri: Uri) {
        _uiState.update { it.copy(selectedImageUri = imageUri) }
    }

    fun updateProfile(
        name: String,
        email: String,
        phoneNumber: String,
        address: String,
        toastmastersId: String,
        gender: String,
        level: String?,
        mentorNames: List<String>,
        role: UserRole
    ) {
        val currentUser = _uiState.value.user ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            // Only allow role changes if current user is VP_EDUCATION
            val finalRole = if (currentUser.role == UserRole.VP_EDUCATION) {
                role
            } else {
                currentUser.role // Keep existing role for MEMBER users
            }
            
            // Handle profile picture update if a new image was selected
            val selectedImageUri = _uiState.value.selectedImageUri
            var finalUpdatedUser = currentUser.copy(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                address = address,
                toastmastersId = toastmastersId,
                gender = gender,
                level = level,
                mentorNames = mentorNames,
                role = finalRole
            )

            // If there's a selected image, update profile picture first
            if (selectedImageUri != null) {
                try {
                    android.util.Log.d("ProfileEditViewModel", "Updating profile picture during save...")
                    val updateResult = profileRepository.updateProfilePicture(currentUser.id, selectedImageUri)
                    
                    if (updateResult.isSuccess) {
                        val newUrl = updateResult.getOrNull()
                        if (newUrl != null) {
                            finalUpdatedUser = finalUpdatedUser.copy(profilePictureUrl = newUrl)
                            android.util.Log.d("ProfileEditViewModel", "Profile picture updated successfully")
                        }
                    } else {
                        android.util.Log.e("ProfileEditViewModel", "Failed to update profile picture: ${updateResult.exceptionOrNull()?.message}")
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = updateResult.exceptionOrNull()?.message ?: "Failed to update profile picture"
                            )
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileEditViewModel", "Exception during profile picture update", e)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = e.message ?: "An error occurred while updating profile picture"
                        )
                    }
                    return@launch
                }
            }
            
            when (val result = userRepository.updateUser(finalUpdatedUser)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isSaving = false, 
                            saveSuccess = true,
                            user = finalUpdatedUser,
                            selectedImageUri = null // Clear selected image after save
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isSaving = false, 
                            error = result.exception.message ?: "Failed to update profile"
                        ) 
                    }
                }
                is Result.Loading -> {
                    _uiState.update {
                        it.copy(isSaving = true)
                    }
                }
            }
        }
    }



    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
