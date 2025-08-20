package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.utils.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class UpcomingMeetingsListViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val memberResponseRepository: MemberResponseRepository,
    private val userManager: UserManager
) : ViewModel() {

    private val _uiState = MutableLiveData<UpcomingMeetingsState>()
    val uiState: LiveData<UpcomingMeetingsState> = _uiState

    private var currentUserId: String = ""

    fun loadUpcomingMeetings() {
        viewModelScope.launch {
            Timber.d("loadUpcomingMeetings: Starting to load upcoming meetings")
            _uiState.value = UpcomingMeetingsState.Loading
            try {
                currentUserId = userManager.getCurrentUserId() ?: run {
                    val error = "User not authenticated"
                    Timber.w(error)
                    _uiState.value = UpcomingMeetingsState.Error(error)
                    return@launch
                }

                Timber.d("Loading meetings for user: $currentUserId")
                val meetingsFlow = meetingRepository.getAllMeetings()
                val meetings = meetingsFlow.first()

                Timber.d("🔥 Fetched ${meetings.size} meetings from repository")
                meetings.forEach { meeting ->
                    Timber.d("📅 Meeting: ${meeting.theme} | ${meeting.dateTime} | ID: ${meeting.id}")
                }

                _uiState.value = if (meetings.isNotEmpty()) {
                    Timber.d("✅ Successfully loaded ${meetings.size} meetings")
                    UpcomingMeetingsState.Success(meetings)
                } else {
                    Timber.d("No upcoming meetings found")
                    UpcomingMeetingsState.Success(emptyList())
                }
            } catch (e: Exception) {
                val error = "Failed to load meetings: ${e.message}"
                Timber.e(e, error)
                _uiState.value = UpcomingMeetingsState.Error(error)
            }
        }
    }
    fun updateMemberAvailability(meetingId: String, availability: MemberResponse.AvailabilityStatus) {
        viewModelScope.launch {
            try {
                // Ensure we have a valid meeting ID and user ID
                if (meetingId.isBlank()) {
                    _uiState.value = UpcomingMeetingsState.Error("Invalid meeting ID")
                    return@launch
                }
                
                if (currentUserId.isEmpty()) {
                    currentUserId = userManager.getCurrentUserId() ?: run {
                        _uiState.value = UpcomingMeetingsState.Error("User not authenticated")
                        return@launch
                    }
                }
                
                // Check if response already exists
                val existingResponses = memberResponseRepository.getResponsesForMeeting(meetingId)
                    .first()
                    .filter { it.memberId == currentUserId }

                val response = if (existingResponses.isNotEmpty()) {
                    existingResponses.first().copy(availability = availability)
                } else {
                    MemberResponse(
                        meetingId = meetingId,
                        memberId = currentUserId,
                        availability = availability,
                        preferredRoles = emptyList(),
                        notes = ""
                    )
                }

                when (val result = memberResponseRepository.saveResponse(response)) {
                    is com.bntsoft.toastmasters.utils.Result.Success -> {
                        // Refresh the list to show updated status
                        loadUpcomingMeetings()
                    }
                    is com.bntsoft.toastmasters.utils.Result.Error -> {
                        _uiState.value = UpcomingMeetingsState.Error("Failed to save response: ${result.exception?.message ?: "Unknown error"}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = UpcomingMeetingsState.Error("Failed to update availability: ${e.message}")
            }
        }
    }
    
    fun updatePreferredRoles(meetingId: String, selectedRoles: List<String>) {
        viewModelScope.launch {
            try {
                // Ensure we have a valid meeting ID and user ID
                if (meetingId.isBlank()) {
                    _uiState.value = UpcomingMeetingsState.Error("Invalid meeting ID")
                    return@launch
                }
                
                if (currentUserId.isEmpty()) {
                    currentUserId = userManager.getCurrentUserId() ?: run {
                        _uiState.value = UpcomingMeetingsState.Error("User not authenticated")
                        return@launch
                    }
                }
                
                // Get existing response
                val existingResponses = memberResponseRepository.getResponsesForMeeting(meetingId)
                    .first()
                    .filter { it.memberId == currentUserId }
                    
                if (existingResponses.isNotEmpty()) {
                    val response = existingResponses.first().copy(preferredRoles = selectedRoles)
                    when (val result = memberResponseRepository.saveResponse(response)) {
                        is com.bntsoft.toastmasters.utils.Result.Success -> {
                            // Success - no need to update UI as the chip state is already updated
                        }
                        is com.bntsoft.toastmasters.utils.Result.Error -> {
                            _uiState.value = UpcomingMeetingsState.Error("Failed to save roles: ${result.exception?.message ?: "Unknown error"}")
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UpcomingMeetingsState.Error("Failed to update preferred roles: ${e.message}")
            }
        }
    }

    sealed class UpcomingMeetingsState {
        object Loading : UpcomingMeetingsState()
        data class Success(val meetings: List<Meeting>) : UpcomingMeetingsState()
        data class Error(val message: String) : UpcomingMeetingsState()
    }
}
