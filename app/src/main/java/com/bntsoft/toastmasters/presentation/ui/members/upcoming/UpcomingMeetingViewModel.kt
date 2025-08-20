package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.User
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.repository.AuthRepository
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UpcomingMeetingViewModel @Inject constructor(
    private val memberResponseRepository: MemberResponseRepository,
    private val meetingRepository: MeetingRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Meeting ID from navigation arguments
    private val meetingId: String = savedStateHandle.get<String>(MEETING_ID_ARG) ?: ""

    // Current user ID (to be replaced with actual user ID from authentication)
    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow<MemberResponseUiState>(MemberResponseUiState.Loading)
    val uiState: StateFlow<MemberResponseUiState> = _uiState.asStateFlow()

    // Meeting details
    private val _meeting = MutableStateFlow<Meeting?>(null)
    val meeting: StateFlow<Meeting?> = _meeting.asStateFlow()

    // Current response from the member
    private val _currentResponse = MutableStateFlow<MemberResponse?>(null)
    val currentResponse: StateFlow<MemberResponse?> = _currentResponse.asStateFlow()

    // Available roles for the meeting
    private val _availableRoles = MutableStateFlow<List<String>>(emptyList())
    val availableRoles: StateFlow<List<String>> = _availableRoles.asStateFlow()
    
    init {
        loadCurrentUser()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                authRepository.getCurrentUser()?.let { user ->
                    _currentUserId.value = user.id
                    loadMeetingAndResponse()
                } ?: run {
                    _uiState.value = MemberResponseUiState.Error("User not authenticated")
                }
            } catch (e: Exception) {
                _uiState.value = MemberResponseUiState.Error("Failed to load user: ${e.message}")
            }
        }
    }

    fun loadMeetingAndResponse() {
        viewModelScope.launch {
            try {
                _uiState.value = MemberResponseUiState.Loading

                // Load meeting details
                val meetingResult = meetingRepository.getMeetingById(meetingId)
                if (meetingResult != null) {
                    _meeting.value = meetingResult
                    // Update available roles from meeting
                    _availableRoles.value = meetingResult.availableRoles.ifEmpty {
                        listOf("Toastmaster", "Speaker", "Evaluator", "Table Topics Master") // Default roles
                    }
                    
                    // Load or create member response
                    memberResponseRepository.getMemberResponse(meetingId, _currentUserId.value)
                        .catch { e ->
                            _uiState.value = MemberResponseUiState.Error("Failed to load your response")
                            Timber.e(e, "Error loading member response")
                        }
                        .collect { response ->
                            _currentResponse.value = response ?: createDefaultResponse()
                            _uiState.value = MemberResponseUiState.Success
                        }
                } else {
                    _uiState.value = MemberResponseUiState.Error("Meeting not found")
                }
            } catch (e: Exception) {
                _uiState.value = MemberResponseUiState.Error("Failed to load meeting: ${e.message}")
                Timber.e(e, "Error in loadMeetingAndResponse")
            }
        }
    }

    private fun createDefaultResponse(): MemberResponse {
        return MemberResponse(
            meetingId = meetingId,
            memberId = currentUserId.value,
            availability = MemberResponse.AvailabilityStatus.NOT_CONFIRMED,
            preferredRoles = emptyList(),
            notes = ""
        )
    }

    fun updateAvailability(status: MemberResponse.AvailabilityStatus) {
        _currentResponse.value?.let { current ->
            _currentResponse.value = current.copy(availability = status)
        }
    }

    fun toggleRole(role: String) {
        _currentResponse.value?.let { current ->
            val newRoles = if (current.preferredRoles.contains(role)) {
                current.preferredRoles - role
            } else {
                current.preferredRoles + role
            }
            _currentResponse.value = current.copy(preferredRoles = newRoles)
        }
    }

    fun updateNotes(notes: String) {
        _currentResponse.value?.let { current ->
            _currentResponse.value = current.copy(notes = notes)
        }
    }

    fun submitResponse() {
        val response = _currentResponse.value ?: return

        viewModelScope.launch {
            _uiState.value = MemberResponseUiState.Saving

            try {
                when (val result = memberResponseRepository.saveResponse(response)) {
                    is com.bntsoft.toastmasters.utils.Result.Error -> _uiState.value =
                        MemberResponseUiState.Error("Failed to save your response")

                    com.bntsoft.toastmasters.utils.Result.Loading -> _uiState.value =
                        MemberResponseUiState.Loading

                    is com.bntsoft.toastmasters.utils.Result.Success -> _uiState.value =
                        MemberResponseUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = MemberResponseUiState.Error("An error occurred while saving")
                Timber.e(e, "Error submitting response")
            }
        }
    }

    sealed class MemberResponseUiState {
        object Loading : MemberResponseUiState()
        object Saving : MemberResponseUiState()
        object Success : MemberResponseUiState()
        data class Error(val message: String) : MemberResponseUiState()

        fun handle(
            onLoading: () -> Unit = {},
            onSaving: () -> Unit = {},
            onSuccess: () -> Unit = {},
            onError: (String) -> Unit = {}
        ) = when (this) {
            is Loading -> onLoading()
            is Saving -> onSaving()
            is Success -> onSuccess()
            is Error -> onError(message)
        }
    }

    companion object {
        const val MEETING_ID_ARG = "meeting_id"
    }
}
