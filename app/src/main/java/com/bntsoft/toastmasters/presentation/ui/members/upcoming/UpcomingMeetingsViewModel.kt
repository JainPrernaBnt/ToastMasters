package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingAvailability
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class UpcomingMeetingsListViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val userRepository: UserRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableLiveData<UpcomingMeetingsUiState>()
    val uiState: LiveData<UpcomingMeetingsUiState> = _uiState

    private val _showSuccessDialog = MutableLiveData<Boolean>()
    val showSuccessDialog: LiveData<Boolean> = _showSuccessDialog

    private val db = Firebase.firestore
    private val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    private val _meetingAvailability = MutableLiveData<MeetingAvailability>()
    val meetingAvailability: LiveData<MeetingAvailability> = _meetingAvailability
    
    private val _meetings = MutableLiveData<List<Meeting>>()
    val meetings: LiveData<List<Meeting>> = _meetings

    init {
        fetchUpcomingMeetings()
    }

    fun saveMeetingAvailability(
        meetingId: String,
        isAvailable: Boolean,
        preferredRoles: List<String>
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = UpcomingMeetingsUiState.Loading

                val meetingAvailability = MeetingAvailability(
                    userId = currentUserId,
                    meetingId = meetingId,
                    isAvailable = isAvailable,
                    preferredRoles = if (isAvailable) preferredRoles else emptyList(),
                    timestamp = System.currentTimeMillis()
                )

                db.collection("meetings")
                    .document(meetingId)
                    .collection("availability")
                    .document(currentUserId) // each user has their own doc
                    .set(meetingAvailability)
                    .await()

                _showSuccessDialog.value = true
                _uiState.value = UpcomingMeetingsUiState.Success()
            } catch (e: Exception) {
                _uiState.value = UpcomingMeetingsUiState.Error(
                    e.message ?: application.getString(R.string.error_saving_availability)
                )
            }
        }
    }

    fun onDialogDismissed() {
        _showSuccessDialog.value = false
    }

    fun checkMeetingAvailability(meetingId: String) {
        viewModelScope.launch {
            try {
                val availability = db.collection("meetings")
                    .document(meetingId)
                    .collection("availability")
                    .document(currentUserId)
                    .get()
                    .await()
                    .toObject(MeetingAvailability::class.java)
                
                availability?.let { 
                    _meetingAvailability.value = it
                    // Update the meetings list with the new availability
                    _meetings.value = _meetings.value?.map { meeting ->
                        if (meeting.id == meetingId) {
                            meeting.copy(availability = it)
                        } else {
                            meeting
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UpcomingMeetingsUiState.Error(
                    e.message ?: "Error checking availability"
                )
            }
        }
    }

    fun fetchUpcomingMeetings() {
        viewModelScope.launch {
            _uiState.value = UpcomingMeetingsUiState.Loading
            try {
                meetingRepository.getUpcomingMeetings(LocalDate.now()).collect { meetings ->
                    _meetings.value = meetings
                    _uiState.value = UpcomingMeetingsUiState.Success(meetings)
                    // Check availability for each meeting
                    meetings.forEach { meeting ->
                        checkMeetingAvailability(meeting.id)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UpcomingMeetingsUiState.Error(
                    e.message ?: "Error fetching meetings"
                )
            }
        }
    }
}

sealed class UpcomingMeetingsUiState {
    object Loading : UpcomingMeetingsUiState()
    data class Success(val meetings: List<Meeting> = emptyList()) : UpcomingMeetingsUiState()
    data class Error(val message: String) : UpcomingMeetingsUiState()
}
