package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingAvailability
import com.bntsoft.toastmasters.domain.models.AvailabilityStatus
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    private val _meetingAvailability = MutableLiveData<MeetingAvailability?>()
    val meetingAvailability: LiveData<MeetingAvailability> =
        _meetingAvailability as LiveData<MeetingAvailability>

    private val _meetings = MutableLiveData<List<Meeting>>()
    val meetings: LiveData<List<Meeting>> = _meetings

    fun toggleEditMode(meeting: Meeting) {
        val currentList = _meetings.value?.toMutableList() ?: return
        val position = currentList.indexOfFirst { it.id == meeting.id }
        if (position != -1) {
            // Toggle edit mode for the selected meeting and ensure others are not in edit mode
            currentList[position] = currentList[position].toggleEditMode()
            currentList.forEachIndexed { index, item ->
                if (index != position && item.isEditMode) {
                    currentList[index] = item.copy(isEditMode = false)
                }
            }
            _meetings.value = currentList
        }
    }

    init {
        fetchUpcomingMeetings()
    }

    fun saveMeetingAvailability(
        meetingId: String,
        status: AvailabilityStatus,
        preferredRoles: List<String>,
        isBackout: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = UpcomingMeetingsUiState.Loading

                // Only keep preferred roles if member is Available
                val finalPreferredRoles =
                    if (status == AvailabilityStatus.AVAILABLE) preferredRoles else emptyList()

                val meetingAvailability = MeetingAvailability(
                    userId = currentUserId,
                    meetingId = meetingId,
                    status = if (isBackout) AvailabilityStatus.NOT_AVAILABLE else status,
                    preferredRoles = finalPreferredRoles,
                    timestamp = System.currentTimeMillis(),
                    isBackout = isBackout
                )

                val batch = db.batch()
                
                // Update availability
                val availabilityRef = db.collection("meetings")
                    .document(meetingId)
                    .collection("availability")
                    .document(currentUserId)
                
                batch.set(availabilityRef, meetingAvailability)
                
                // If this is a backout, clear any assigned roles
                if (isBackout) {
                    val roleRef = db.collection("meetings")
                        .document(meetingId)
                        .collection("assignedRole")
                        .document(currentUserId)
                    
                    batch.delete(roleRef)
                    
                    // Add to backout members collection
                    val backoutRef = db.collection("meetings")
                        .document(meetingId)
                        .collection("backoutMembers")
                        .document(currentUserId)
                    
                    val backoutData = hashMapOf(
                        "userId" to currentUserId,
                        "timestamp" to System.currentTimeMillis(),
                        "meetingId" to meetingId
                    )
                    
                    batch.set(backoutRef, backoutData)
                }
                
                batch.commit().await()

                // Update the meetings list directly to avoid a full refresh
                _meetings.value = _meetings.value?.map { meeting ->
                    if (meeting.id == meetingId) {
                        meeting.copy(
                            availability = meetingAvailability,
                            isEditMode = false // Exit edit mode after saving
                        )
                    } else {
                        meeting
                    }
                } ?: emptyList()

                _showSuccessDialog.value = true
                _uiState.value = UpcomingMeetingsUiState.Success(_meetings.value ?: emptyList())
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

                // Update the meetings list with the availability (or null if none found)
                _meetings.value = _meetings.value?.map { meeting ->
                    if (meeting.id == meetingId) {
                        meeting.copy(availability = availability)
                    } else {
                        meeting
                    }
                } ?: emptyList()

                // Update the single availability LiveData
                _meetingAvailability.value = availability

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
                    // Initialize meetings with no availability and not in edit mode
                    val meetingsWithDefaultState = meetings.map { meeting ->
                        meeting.copy(
                            availability = null,
                            isEditMode = false
                        )
                    }

                    _meetings.value = meetingsWithDefaultState
                    _uiState.value = UpcomingMeetingsUiState.Success(meetingsWithDefaultState)

                    // Check availability for each meeting
                    meetingsWithDefaultState.forEach { meeting ->
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
