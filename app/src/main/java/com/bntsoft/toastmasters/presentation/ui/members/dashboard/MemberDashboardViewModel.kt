package com.bntsoft.toastmasters.presentation.ui.members.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.MeetingWithRole
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MemberDashboardViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val TAG = "MemberDashboardVM"

    private val _uiState = MutableStateFlow<MemberDashboardUiState>(MemberDashboardUiState.Loading)
    val uiState: StateFlow<MemberDashboardUiState> = _uiState.asStateFlow()

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
                            val roles = meetingRepository.getAssignedRoles(meeting.id, currentUser.uid)
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
                    Log.d(TAG, "Successfully loaded ${sortedMeetings.size} meetings with assigned roles")
                    _uiState.value = MemberDashboardUiState.Success(sortedMeetings)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading assigned meetings: ${e.message}", e)
                _uiState.value = MemberDashboardUiState.Error("Failed to load meetings: ${e.message}")
            }
        }
    }
}

sealed class MemberDashboardUiState {
    object Loading : MemberDashboardUiState()
    object Empty : MemberDashboardUiState()
    data class Success(val meetings: List<MeetingWithRole>) : MemberDashboardUiState()
    data class Error(val message: String) : MemberDashboardUiState()
}
