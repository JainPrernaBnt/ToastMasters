package com.bntsoft.toastmasters.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MeetingsViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val memberResponseRepository: MemberResponseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MeetingsUiState>(MeetingsUiState.Loading)
    val uiState: StateFlow<MeetingsUiState> = _uiState.asStateFlow()

    private val _upcomingMeetingsStateWithCounts =
        MutableStateFlow<UpcomingMeetingsStateWithCounts>(UpcomingMeetingsStateWithCounts.Loading)
    val upcomingMeetingsStateWithCounts: StateFlow<UpcomingMeetingsStateWithCounts> =
        _upcomingMeetingsStateWithCounts.asStateFlow()

    private val _upcomingMeetingsState =
        MutableStateFlow<UpcomingMeetingsState>(UpcomingMeetingsState.Loading)
    val upcomingMeetingsState: StateFlow<UpcomingMeetingsState> =
        _upcomingMeetingsState.asStateFlow()

    private val _createMeetingState = MutableStateFlow<CreateMeetingState>(CreateMeetingState.Idle)
    val createMeetingState: StateFlow<CreateMeetingState> = _createMeetingState.asStateFlow()

    init {
        loadMeetings()
        loadUpcomingMeetings()
    }

    fun loadMeetings() {
        viewModelScope.launch {
            _uiState.value = MeetingsUiState.Loading

            meetingRepository.getAllMeetings()
                .catch { e ->
                    _uiState.value = MeetingsUiState.Error(e.message ?: "Unknown error occurred")
                }
                .collectLatest { meetings ->
                    if (meetings.isEmpty()) {
                        _uiState.value = MeetingsUiState.Empty
                    } else {
                        _uiState.value = MeetingsUiState.Success(meetings)
                    }
                }
        }
    }

    fun loadUpcomingMeetings() {
        viewModelScope.launch {
            _upcomingMeetingsStateWithCounts.value = UpcomingMeetingsStateWithCounts.Loading

            meetingRepository.getUpcomingMeetings(LocalDate.now()).collectLatest { meetings ->
                if (meetings.isEmpty()) {
                    _upcomingMeetingsStateWithCounts.value = UpcomingMeetingsStateWithCounts.Empty
                    return@collectLatest
                }

                val flows = meetings.map { meeting ->
                    val idInt = meeting.id?.toIntOrNull()
                    if (idInt != null) {
                        memberResponseRepository.getResponsesForMeeting(idInt)
                    } else {
                        flowOf(emptyList())
                    }
                }

                combine(flows) { responsesArray: Array<List<MemberResponse>> ->
                    meetings.mapIndexed { index, meeting ->
                        val responses = responsesArray[index]
                        val availableCount =
                            responses.count { it.availability == MemberResponse.AvailabilityStatus.AVAILABLE }
                        val notAvailableCount =
                            responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_AVAILABLE }
                        val notConfirmedCount =
                            responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_CONFIRMED }

                        MeetingWithCounts(
                            meeting = meeting,
                            availableCount = availableCount,
                            notAvailableCount = notAvailableCount,
                            notConfirmedCount = notConfirmedCount
                        )
                    }
                }.catch { e ->
                    _upcomingMeetingsStateWithCounts.value =
                        UpcomingMeetingsStateWithCounts.Error(e.message ?: "Unknown error occurred")
                }.collect { meetingsWithCounts ->
                    _upcomingMeetingsStateWithCounts.value =
                        UpcomingMeetingsStateWithCounts.Success(meetingsWithCounts)
                }
            }
        }
    }

    fun deleteMeeting(id: String) {
        viewModelScope.launch {
            when (val result = meetingRepository.deleteMeeting(id)) {
                is Resource.Success -> {
                    // Refresh lists after deletion
                    loadMeetings()
                    loadUpcomingMeetings()
                }
                is Resource.Error -> {
                    // Optionally, expose an error state if needed later
                    Timber.e("Failed to delete meeting: ${result.message}")
                }
                else -> {}
            }
        }
    }

    fun createMeeting(meeting: Meeting) {
        Timber.d("createMeeting called with: ${meeting}")
        viewModelScope.launch {
            _createMeetingState.value = CreateMeetingState.Loading
            Timber.d("CreateMeetingState set to Loading")

            when (val result = meetingRepository.createMeeting(meeting)) {
                is Resource.Success -> {
                    Timber.d("Meeting creation successful: ${result.data}")
                    _createMeetingState.value = CreateMeetingState.Success(result.data!!)
                    loadMeetings() // Refresh the meetings list
                    loadUpcomingMeetings() // Refresh upcoming meetings
                }

                is Resource.Error -> {
                    Timber.e("Meeting creation failed: ${result.message}")
                    _createMeetingState.value =
                        CreateMeetingState.Error(result.message ?: "Failed to create meeting")
                }

                else -> {}
            }
        }
    }

    fun syncMeetings() {
        viewModelScope.launch {
            _uiState.value = MeetingsUiState.Loading

            when (val result = meetingRepository.syncMeetings()) {
                is Resource.Success -> {
                    // The flow will automatically update the UI when the local DB is updated
                }

                is Resource.Error -> {
                    _uiState.value =
                        MeetingsUiState.Error(result.message ?: "Failed to sync meetings")
                }

                is Resource.Loading -> TODO()
            }
        }
    }

    fun resetCreateMeetingState() {
        _createMeetingState.value = CreateMeetingState.Idle
    }
}

// UI States
sealed class MeetingsUiState {
    object Loading : MeetingsUiState()
    object Empty : MeetingsUiState()
    data class Error(val message: String) : MeetingsUiState()
    data class Success(val meetings: List<Meeting>) : MeetingsUiState()
}

sealed class UpcomingMeetingsState {
    object Loading : UpcomingMeetingsState()
    object Empty : UpcomingMeetingsState()
    data class Error(val message: String) : UpcomingMeetingsState()
    data class Success(val meetings: List<Meeting>) : UpcomingMeetingsState()
}

sealed class UpcomingMeetingsStateWithCounts {
    object Loading : UpcomingMeetingsStateWithCounts()
    object Empty : UpcomingMeetingsStateWithCounts()
    data class Error(val message: String) : UpcomingMeetingsStateWithCounts()
    data class Success(val meetings: List<MeetingWithCounts>) : UpcomingMeetingsStateWithCounts()
}

sealed class CreateMeetingState {
    object Idle : CreateMeetingState()
    object Loading : CreateMeetingState()
    data class Success(val meeting: Meeting) : CreateMeetingState()
    data class Error(val message: String) : CreateMeetingState()
}
