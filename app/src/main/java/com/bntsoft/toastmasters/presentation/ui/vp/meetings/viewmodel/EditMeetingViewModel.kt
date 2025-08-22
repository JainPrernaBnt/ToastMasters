package com.bntsoft.toastmasters.presentation.ui.vp.meetings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class EditMeetingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditMeetingState>(EditMeetingState.Loading)
    val uiState: StateFlow<EditMeetingState> = _uiState.asStateFlow()

    fun loadMeeting(meetingId: String) {
        viewModelScope.launch {
            _uiState.value = EditMeetingState.Loading

            try {
                val meeting = meetingRepository.getMeetingById(meetingId)

                meeting?.let {
                    _uiState.value = EditMeetingState.Success(it)
                } ?: run {
                    _uiState.value = EditMeetingState.Error("Meeting not found")
                }
            } catch (e: Exception) {
                _uiState.value = EditMeetingState.Error(
                    getErrorMessage(e)
                )
            }
        }
    }

    fun updateMeeting(meeting: Meeting) {
        viewModelScope.launch {
            _uiState.value = EditMeetingState.Loading

            try {
                when (val result = meetingRepository.updateMeeting(meeting)) {
                    is Resource.Success -> {
                        _uiState.value = EditMeetingState.Updated(meeting)
                    }

                    is Resource.Error -> {
                        _uiState.value = EditMeetingState.Error(
                            result.message ?: "Failed to update meeting"
                        )
                    }

                    is Resource.Loading -> {
                        _uiState.value = EditMeetingState.Loading
                    }
                }
            } catch (e: Exception) {
                _uiState.value = EditMeetingState.Error(
                    getErrorMessage(e)
                )
            }
        }
    }

    fun deleteMeeting(meetingId: String) {
        viewModelScope.launch {
            _uiState.value = EditMeetingState.Loading

            try {
                when (val result = meetingRepository.deleteMeeting(meetingId)) {
                    is Resource.Success -> {
                        _uiState.value = EditMeetingState.Deleted
                    }

                    is Resource.Error -> {
                        _uiState.value = EditMeetingState.Error(
                            result.message ?: "Failed to delete meeting"
                        )
                    }

                    is Resource.Loading -> {
                        _uiState.value = EditMeetingState.Loading
                    }
                }
            } catch (e: Exception) {
                _uiState.value = EditMeetingState.Error(
                    getErrorMessage(e)
                )
            }
        }
    }

    private fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is UnknownHostException -> "No internet connection. Please check your network and try again."
            else -> exception.message ?: "An unexpected error occurred"
        }
    }

    sealed class EditMeetingState {
        object Loading : EditMeetingState()
        data class Success(val meeting: Meeting) : EditMeetingState()
        data class Updated(val meeting: Meeting) : EditMeetingState()
        object Deleted : EditMeetingState()
        data class Error(val message: String) : EditMeetingState()
    }
}
