package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
// Timber import removed, using Android Log instead
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val memberResponseRepository: MemberResponseRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _upcomingMeetingsStateWithCounts =
        MutableStateFlow<UpcomingMeetingsStateWithCounts>(
            UpcomingMeetingsStateWithCounts.Loading
        )
    val upcomingMeetingsStateWithCounts: StateFlow<UpcomingMeetingsStateWithCounts> =
        _upcomingMeetingsStateWithCounts.asStateFlow()

    init {
        loadUpcomingMeetings()
    }

    fun loadUpcomingMeetings() {
        viewModelScope.launch {
            Log.d("DashboardDebug", "loadUpcomingMeetings: Starting to load meetings")
            _upcomingMeetingsStateWithCounts.value = UpcomingMeetingsStateWithCounts.Loading

            try {
                // Get all club members once (excluding VP Education)
                val allMembers = try {
                    val members = memberRepository.getAllMembers(includePending = false).first()
                        .filter { !it.isVpEducation }
                    Log.d("DashboardDebug", "Found ${members.size} active members (excluding VP Education)")
                    members
                } catch (e: Exception) {
                    Log.e("DashboardDebug", "Error fetching members", e)
                    _upcomingMeetingsStateWithCounts.value = 
                        UpcomingMeetingsStateWithCounts.Error("Failed to load members: ${e.message}")
                    return@launch
                }
                val totalMembers = allMembers.size

                // Combine the meetings flow with the responses flow
                Log.d("DashboardDebug", "Getting upcoming meetings from repository")
                meetingRepository.getUpcomingMeetings(LocalDate.now()).collectLatest { meetings ->
                    Log.d("DashboardDebug", "Retrieved ${meetings.size} meetings from repository")
                    if (meetings.isEmpty()) {
                        Log.d("DashboardDebug", "No meetings found in repository")
                    } else {
                        meetings.forEachIndexed { index, meeting ->
                            Log.d("DashboardDebug", "Meeting #${index + 1}: ${meeting.theme} (${meeting.id}) - " +
                                    "Status: ${meeting.status}, " +
                                    "Date: ${meeting.dateTime}")
                        }
                    }

                    if (meetings.isEmpty()) {
                        Log.d("DashboardViewModel", "No upcoming meetings found")
                        _upcomingMeetingsStateWithCounts.value =
                            UpcomingMeetingsStateWithCounts.Empty
                        return@collectLatest
                    }

                    // Create a flow for each meeting's responses and combine them
                    val meetingsFlow = meetings.map { meeting ->
                        memberResponseRepository.getResponsesForMeeting(meeting.id)
                            .map { responses ->
                                val availableCount =
                                    responses.count { it.availability == MemberResponse.AvailabilityStatus.AVAILABLE }
                                val notAvailableCount =
                                    responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_AVAILABLE }
                                val notConfirmedCount =
                                    responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_CONFIRMED }
                                val notRespondedCount =
                                    (totalMembers - responses.size).coerceAtLeast(0)

                                Log.d("DashboardViewModel", "Meeting ${meeting.id} - Available: $availableCount, Not Available: $notAvailableCount, Not Confirmed: $notConfirmedCount, Not Responded: $notRespondedCount (Total members: $totalMembers, Responses: ${responses.size})")

                                MeetingWithCounts(
                                    meeting = meeting,
                                    availableCount = availableCount,
                                    notAvailableCount = notAvailableCount,
                                    notConfirmedCount = notConfirmedCount,
                                    notResponded = notRespondedCount
                                )
                            }
                    }

                    // Combine all the flows and collect updates
                    combine(meetingsFlow) { meetingCounts ->
                        meetingCounts.toList()
                    }.collect { meetingsWithCounts ->
                        val sortedMeetings = meetingsWithCounts.sortedBy { it.meeting.dateTime }
                        Log.d("DashboardViewModel", "Successfully loaded ${meetingsWithCounts.size} upcoming meetings")
                        sortedMeetings.forEach { 
                            Log.d("DashboardViewModel", "Sending to UI: ${it.meeting.theme} - ${it.meeting.dateTime} - Status: ${it.meeting.status}")
                        }
                        _upcomingMeetingsStateWithCounts.value =
                            UpcomingMeetingsStateWithCounts.Success(sortedMeetings)
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading meetings", e)
                _upcomingMeetingsStateWithCounts.value =
                    UpcomingMeetingsStateWithCounts.Error(e.message ?: "Failed to load meetings")
            }
        }
    }

    suspend fun deleteMeeting(meetingId: String) {
        try {
            meetingRepository.deleteMeeting(meetingId)
            // Refresh the list after deletion
            loadUpcomingMeetings()
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error deleting meeting $meetingId", e)
        }
    }

    suspend fun completeMeeting(meetingId: String) {
        try {
            val result = meetingRepository.completeMeeting(meetingId)
            if (result is Resource.Success) {
                // Refresh the list after completion
                loadUpcomingMeetings()
            } else if (result is Resource.Error) {
                Log.e("DashboardViewModel", "Error completing meeting: ${result.message}")
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error completing meeting $meetingId", e)
        }
    }

    sealed class UpcomingMeetingsStateWithCounts {
        object Loading : UpcomingMeetingsStateWithCounts()
        object Empty : UpcomingMeetingsStateWithCounts()
        data class Error(val message: String) : UpcomingMeetingsStateWithCounts()
        data class Success(val meetings: List<MeetingWithCounts>) :
            UpcomingMeetingsStateWithCounts()
    }
}
