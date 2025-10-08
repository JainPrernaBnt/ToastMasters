package com.bntsoft.toastmasters.presentation.ui.common.leaderboard.external

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.ExternalClubActivity
import com.bntsoft.toastmasters.domain.repository.ExternalClubActivityRepository
import com.bntsoft.toastmasters.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddExternalActivityViewModel @Inject constructor(
    private val repository: ExternalClubActivityRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExternalActivityUiState())
    val uiState: StateFlow<AddExternalActivityUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(ExternalActivityFormState())
    val formState: StateFlow<ExternalActivityFormState> = _formState.asStateFlow()

    fun updateClubName(clubName: String) {
        _formState.value = _formState.value.copy(clubName = clubName)
        validateForm()
    }

    fun updateClubLocation(location: String) {
        _formState.value = _formState.value.copy(clubLocation = location)
    }

    fun updateMeetingDate(date: Date) {
        _formState.value = _formState.value.copy(meetingDate = date)
        validateForm()
    }

    fun updateRolePlayed(role: String) {
        _formState.value = _formState.value.copy(rolePlayed = role)
        validateForm()
    }

    fun updateNotes(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }

    private fun validateForm() {
        val form = _formState.value
        val isValid = form.clubName.isNotBlank() && 
                     form.rolePlayed.isNotBlank() &&
                     form.meetingDate != null
        
        _uiState.value = _uiState.value.copy(isFormValid = isValid)
    }

    fun submitActivity() {
        if (!_uiState.value.isFormValid) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                val form = _formState.value
                val activity = ExternalClubActivity(
                    userId = currentUser.id,
                    userName = currentUser.name,
                    userProfilePicture = currentUser.profilePictureUrl,
                    clubName = form.clubName.trim(),
                    clubLocation = form.clubLocation.takeIf { it.isNotBlank() }?.trim(),
                    meetingDate = form.meetingDate ?: Date(),
                    rolePlayed = form.rolePlayed.trim(),
                    notes = form.notes.takeIf { it.isNotBlank() }?.trim(),
                    timestamp = System.currentTimeMillis()
                )

                val result = repository.addActivity(activity)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to add activity"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun updateActivity(activityId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not found. Please log in again."
                    )
                    return@launch
                }

                val form = _formState.value
                val activity = ExternalClubActivity(
                    id = activityId,
                    userId = currentUser.id,
                    userName = currentUser.name,
                    userProfilePicture = currentUser.profilePictureUrl,
                    clubName = form.clubName.trim(),
                    clubLocation = form.clubLocation.takeIf { it.isNotBlank() }?.trim(),
                    meetingDate = form.meetingDate ?: Date(),
                    rolePlayed = form.rolePlayed.trim(),
                    notes = form.notes.takeIf { it.isNotBlank() }?.trim(),
                    timestamp = System.currentTimeMillis()
                )

                val result = repository.updateActivity(activity)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to update activity"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getRoleOptions(): List<String> {
        return listOf(
            "Toastmaster of the Day",
            "Speaker",
            "Evaluator", 
            "Timer",
            "Ah-Counter",
            "Grammarian",
            "Table Topics Master",
            "Table Topics Speaker",
            "Evaluator",
            "Sergeant at Arms",
            "Other"
        )
    }
}

data class AddExternalActivityUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isFormValid: Boolean = false,
    val error: String? = null
)

data class ExternalActivityFormState(
    val clubName: String = "",
    val clubLocation: String = "",
    val meetingDate: Date? = null,
    val rolePlayed: String = "",
    val notes: String = ""
)
