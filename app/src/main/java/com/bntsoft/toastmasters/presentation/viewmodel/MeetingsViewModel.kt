package com.bntsoft.toastmasters.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MeetingsViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MeetingsUiState>(MeetingsUiState.Loading)
    val uiState: StateFlow<MeetingsUiState> = _uiState.asStateFlow()

    private val _upcomingMeetingsState = MutableStateFlow<UpcomingMeetingsState>(UpcomingMeetingsState.Loading)
    val upcomingMeetingsState: StateFlow<UpcomingMeetingsState> = _upcomingMeetingsState.asStateFlow()

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
            _upcomingMeetingsState.value = UpcomingMeetingsState.Loading
            
            meetingRepository.getUpcomingMeetings(LocalDate.now())
                .catch { e ->
                    _upcomingMeetingsState.value = UpcomingMeetingsState.Error(e.message ?: "Unknown error occurred")
                }
                .collectLatest { meetings ->
                    if (meetings.isEmpty()) {
                        _upcomingMeetingsState.value = UpcomingMeetingsState.Empty
                    } else {
                        _upcomingMeetingsState.value = UpcomingMeetingsState.Success(meetings)
                    }
                }
        }
    }

    fun createMeeting(meeting: Meeting) {
        viewModelScope.launch {
            _createMeetingState.value = CreateMeetingState.Loading
            
            when (val result = meetingRepository.createMeeting(meeting)) {
                is Resource.Success -> {
                    _createMeetingState.value = CreateMeetingState.Success
                    loadMeetings() // Refresh the meetings list
                    loadUpcomingMeetings() // Refresh upcoming meetings
                }
                is Resource.Error -> {
                    _createMeetingState.value = CreateMeetingState.Error(result.message ?: "Failed to create meeting")
                }

                is Resource.Loading -> TODO()
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
                    _uiState.value = MeetingsUiState.Error(result.message ?: "Failed to sync meetings")
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

sealed class CreateMeetingState {
    object Idle : CreateMeetingState()
    object Loading : CreateMeetingState()
    object Success : CreateMeetingState()
    data class Error(val message: String) : CreateMeetingState()
}
