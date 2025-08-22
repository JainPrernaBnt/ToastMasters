package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.viewmodel

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
import timber.log.Timber
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
            _upcomingMeetingsStateWithCounts.value = UpcomingMeetingsStateWithCounts.Loading
            Timber.d("Loading upcoming meetings...")

            try {
                // Get all club members once (excluding VP Education)
                val allMembers = try {
                    val members = memberRepository.getAllMembers(includePending = false).first()
                        .filter { !it.isVpEducation }
                    Timber.d("Found ${members.size} active members (excluding VP Education)")
                    members
                } catch (e: Exception) {
                    Timber.e(e, "Error fetching members")
                    emptyList()
                }
                val totalMembers = allMembers.size

                // Combine the meetings flow with the responses flow
                meetingRepository.getUpcomingMeetings(LocalDate.now()).collectLatest { meetings ->
                    Timber.d("Retrieved ${meetings.size} meetings from repository")

                    if (meetings.isEmpty()) {
                        Timber.d("No upcoming meetings found")
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
                        _upcomingMeetingsStateWithCounts.value =
                            UpcomingMeetingsStateWithCounts.Success(meetingsWithCounts.sortedBy { it.meeting.dateTime })
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading upcoming meetings")
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
            Timber.e(e, "Error deleting meeting $meetingId")
        }
    }

    suspend fun completeMeeting(meetingId: String) {
        try {
            val result = meetingRepository.completeMeeting(meetingId)
            if (result is Resource.Success) {
                // Refresh the list after completion
                loadUpcomingMeetings()
            } else if (result is Resource.Error) {
                Timber.e("Error completing meeting: ${result.message}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error completing meeting $meetingId")
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
