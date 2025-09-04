package com.bntsoft.toastmasters.presentation.ui.members.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.SpeakerDetails
import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.domain.model.MeetingWithRole
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.bntsoft.toastmasters.utils.Result
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MemberDashboardViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val TAG = "MemberDashboardVM"

    val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _uiState = MutableStateFlow<MemberDashboardUiState>(MemberDashboardUiState.Loading)
    val uiState: StateFlow<MemberDashboardUiState> = _uiState.asStateFlow()

    private val _speakerDetailsState = MutableStateFlow(SpeakerDetailsState())
    val speakerDetailsState = _speakerDetailsState.asStateFlow()

    data class SpeakerDetailsState(
        val speakerDetails: Map<String, SpeakerDetails> = emptyMap(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _grammarianDetailsState = MutableStateFlow(GrammarianDetailsState())
    val grammarianDetailsState = _grammarianDetailsState.asStateFlow()

    data class GrammarianDetailsState(
        val grammarianDetails: Map<String, GrammarianDetails> = emptyMap(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    init {
        Log.d(TAG, "ViewModel initialized")
        loadAssignedMeetings()
    }

    fun loadAssignedMeetings() {
        Log.d(TAG, "Loading assigned meetings")
        viewModelScope.launch {
            _uiState.value = MemberDashboardUiState.Loading
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Log.e(TAG, "User not authenticated")
                    _uiState.value = MemberDashboardUiState.Error("User not authenticated")
                    return@launch
                }

                val meetingsWithRoles = mutableListOf<MeetingWithRole>()
                val meetings = meetingRepository.getUpcomingMeetings(
                    LocalDate.now().minusDays(1)
                ) // Include today's meetings
                    .first()

                if (meetings.isEmpty()) {
                    Log.d(TAG, "No upcoming meetings found")
                    _uiState.value = MemberDashboardUiState.Empty
                    return@launch
                }
                Log.d(TAG, "Found ${meetings.size} upcoming meetings")

                // Fetch all roles for all meetings in parallel
                val meetingsWithRolesResult = meetings.map { meeting ->
                    async {
                        try {
                            val roles =
                                meetingRepository.getAssignedRoles(meeting.id, currentUser.uid)
                            if (roles.isNotEmpty()) {
                                MeetingWithRole(
                                    meeting = meeting,
                                    assignedRole = roles.first(), // For backward compatibility
                                    assignedRoles = roles
                                )
                            } else null
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting roles for meeting ${meeting.id}", e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                if (meetingsWithRolesResult.isEmpty()) {
                    Log.d(TAG, "No roles assigned in any meetings")
                    _uiState.value = MemberDashboardUiState.Empty
                } else {
                    val sortedMeetings = meetingsWithRolesResult.sortedBy { it.meeting.dateTime }
                    Log.d(
                        TAG,
                        "Successfully loaded ${sortedMeetings.size} meetings with assigned roles"
                    )
                    _uiState.value = MemberDashboardUiState.Success(sortedMeetings)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading assigned meetings: ${e.message}", e)
                _uiState.value =
                    MemberDashboardUiState.Error("Failed to load meetings: ${e.message}")
            }
        }
    }

    fun saveSpeakerDetails(meetingId: String, userId: String, speakerDetails: SpeakerDetails) {
        viewModelScope.launch {
            _speakerDetailsState.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result =
                    meetingRepository.saveSpeakerDetails(meetingId, userId, speakerDetails)) {
                    is Result.Success -> {
                        // Update the local state with the new details
                        val currentDetails =
                            _speakerDetailsState.value.speakerDetails.toMutableMap()
                        currentDetails[userId] = speakerDetails
                        _speakerDetailsState.update {
                            it.copy(
                                isLoading = false,
                                speakerDetails = currentDetails
                            )
                        }
                        // Also update the UI state if needed
                        updateUiStateWithSpeakerDetails(currentDetails)
                    }

                    is Result.Error -> {
                        _speakerDetailsState.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Failed to save speaker details"
                            )
                        }
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                _speakerDetailsState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    fun loadSpeakerDetails(meetingId: String, userId: String) {
        viewModelScope.launch {
            _speakerDetailsState.update { currentState ->
                currentState.copy(isLoading = true, error = null)
            }
            try {
                val details = meetingRepository.getSpeakerDetails(meetingId, userId)
                if (details != null) {
                    val currentDetails = _speakerDetailsState.value.speakerDetails.toMutableMap<String, SpeakerDetails>()
                    currentDetails[userId] = details
                    val newState = _speakerDetailsState.value.copy(
                        isLoading = false,
                        speakerDetails = currentDetails
                    )
                    _speakerDetailsState.value = newState
                    updateUiStateWithSpeakerDetails(currentDetails)
                } else {
                    _speakerDetailsState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _speakerDetailsState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load speaker details"
                    )
                }
            }
        }
    }

    fun loadAllSpeakerDetailsForMeeting(meetingId: String) {
        viewModelScope.launch {
            _speakerDetailsState.update { currentState ->
                currentState.copy(isLoading = true, error = null)
            }
            try {
                meetingRepository.getSpeakerDetailsForMeeting(meetingId).collect { detailsList ->
                    val detailsMap = detailsList.associateBy { it.userId }
                    val newState = _speakerDetailsState.value.copy(
                        isLoading = false,
                        speakerDetails = detailsMap
                    )
                    _speakerDetailsState.value = newState
                    updateUiStateWithSpeakerDetails(detailsMap)
                }
            } catch (e: Exception) {
                _speakerDetailsState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load speaker details"
                    )
                }
            }
        }
    }

    fun clearError() {
        _speakerDetailsState.update { it.copy(error = null) }
        _grammarianDetailsState.update { it.copy(error = null) }
    }

    suspend fun getSpeakerDetails(meetingId: String, userId: String): SpeakerDetails? {
        return try {
            meetingRepository.getSpeakerDetails(meetingId, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting speaker details", e)
            null
        }
    }

    private fun updateUiStateWithSpeakerDetails(details: Map<String, SpeakerDetails>) {
        _uiState.update { currentState ->
            if (currentState is MemberDashboardUiState.Success) {
                // Create a new Success state with updated speaker details
                MemberDashboardUiState.Success(
                    meetings = currentState.meetings,
                    speakerDetails = details,
                    grammarianDetails = if (currentState.grammarianDetails.isNotEmpty()) currentState.grammarianDetails else emptyMap()
                )
            } else {
                currentState
            }
        }
    }

    private fun updateUiStateWithGrammarianDetails(details: Map<String, GrammarianDetails>) {
        _uiState.update { currentState ->
            if (currentState is MemberDashboardUiState.Success) {
                MemberDashboardUiState.Success(
                    meetings = currentState.meetings,
                    speakerDetails = currentState.speakerDetails,
                    grammarianDetails = details
                )
            } else {
                currentState
            }
        }
    }

    fun saveGrammarianDetails(meetingId: String, userId: String, grammarianDetails: GrammarianDetails) {
        viewModelScope.launch {
            _grammarianDetailsState.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result = meetingRepository.saveGrammarianDetails(meetingId, userId, grammarianDetails)) {
                    is Result.Success -> {
                        val currentDetails = _grammarianDetailsState.value.grammarianDetails.toMutableMap()
                        currentDetails[userId] = grammarianDetails
                        _grammarianDetailsState.update {
                            it.copy(
                                isLoading = false,
                                grammarianDetails = currentDetails
                            )
                        }
                        updateUiStateWithGrammarianDetails(currentDetails)
                    }
                    is Result.Error -> {
                        _grammarianDetailsState.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Failed to save grammarian details"
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _grammarianDetailsState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    fun loadGrammarianDetails(meetingId: String, userId: String) {
        viewModelScope.launch {
            _grammarianDetailsState.update { currentState -> currentState.copy(isLoading = true, error = null) }
            try {
                val details = meetingRepository.getGrammarianDetails(meetingId, userId)
                if (details != null) {
                    val current = _grammarianDetailsState.value.grammarianDetails.toMutableMap<String, GrammarianDetails>()
                    current[userId] = details
                    val newState = _grammarianDetailsState.value.copy(
                        isLoading = false,
                        grammarianDetails = current
                    )
                    _grammarianDetailsState.value = newState
                    updateUiStateWithGrammarianDetails(current)
                } else {
                    _grammarianDetailsState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _grammarianDetailsState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load grammarian details"
                    )
                }
            }
        }
    }

    fun loadAllGrammarianDetailsForMeeting(meetingId: String) {
        viewModelScope.launch {
            _grammarianDetailsState.update { currentState -> currentState.copy(isLoading = true, error = null) }
            try {
                meetingRepository.getGrammarianDetailsForMeeting(meetingId).collect { detailsList ->
                    val detailsMap = detailsList.associateBy { it.userId }
                    val newState = _grammarianDetailsState.value.copy(
                        isLoading = false,
                        grammarianDetails = detailsMap
                    )
                    _grammarianDetailsState.value = newState
                    updateUiStateWithGrammarianDetails(detailsMap)
                }
            } catch (e: Exception) {
                _grammarianDetailsState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load grammarian details"
                    )
                }
            }
        }
    }

    sealed class MemberDashboardUiState {
        object Loading : MemberDashboardUiState()
        object Empty : MemberDashboardUiState()
        data class Success(
            val meetings: List<MeetingWithRole>,
            val speakerDetails: Map<String, SpeakerDetails> = emptyMap(),
            val grammarianDetails: Map<String, GrammarianDetails> = emptyMap()
        ) : MemberDashboardUiState()
        data class Error(val message: String) : MemberDashboardUiState()
    }
}
