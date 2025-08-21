package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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

    private val _upcomingMeetingsStateWithCounts = MutableStateFlow<UpcomingMeetingsStateWithCounts>(
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
                // Get all club members once
                val allMembers = try {
                    val members = memberRepository.getAllMembers(includePending = false).first()
                    Timber.d("Found ${members.size} active members")
                    members
                } catch (e: Exception) {
                    Timber.e(e, "Error fetching members")
                    emptyList()
                }
                val totalMembers = allMembers.size

                meetingRepository.getUpcomingMeetings(LocalDate.now()).collectLatest { meetings ->
                    Timber.d("Retrieved ${meetings.size} meetings from repository")
                    meetings.forEach { meeting ->
                        Timber.d("Meeting: ${meeting.theme} on ${meeting.dateTime}")
                    }

                    if (meetings.isEmpty()) {
                        Timber.d("No upcoming meetings found")
                        _upcomingMeetingsStateWithCounts.value = UpcomingMeetingsStateWithCounts.Empty
                        return@collectLatest
                    }

                    // For each meeting, get responses and create MeetingWithCounts
                    val meetingsWithCounts = meetings.map { meeting ->
                        val responses = if (!meeting.id.isNullOrBlank()) {
                            try {
                                memberResponseRepository.getResponsesForMeeting(meeting.id).first()
                            } catch (e: Exception) {
                                Timber.e(e, "Error getting responses for meeting ${meeting.id}")
                                emptyList()
                            }
                        } else {
                            emptyList()
                        }

                        val availableCount = responses.count { it.availability == MemberResponse.AvailabilityStatus.AVAILABLE }
                        val notAvailableCount = responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_AVAILABLE }
                        val notConfirmedCount = responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_CONFIRMED }
                        val notRespondedCount = (totalMembers - responses.size).coerceAtLeast(0)

                        MeetingWithCounts(
                            meeting = meeting,
                            availableCount = availableCount,
                            notAvailableCount = notAvailableCount,
                            notConfirmedCount = notConfirmedCount,
                            notResponded = notRespondedCount
                        )
                    }

                    _upcomingMeetingsStateWithCounts.value =
                        UpcomingMeetingsStateWithCounts.Success(meetingsWithCounts)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading upcoming meetings")
                _upcomingMeetingsStateWithCounts.value =
                    UpcomingMeetingsStateWithCounts.Error(e.message ?: "Failed to load meetings")
            }
        }
    }

    fun deleteMeeting(id: String) {
        viewModelScope.launch {
            when (val result = meetingRepository.deleteMeeting(id)) {
                is Resource.Success -> {
                    loadUpcomingMeetings()
                }
                is Resource.Error -> {
                    Timber.e("Failed to delete meeting: ${result.message}")
                }
                else -> {}
            }
        }
    }

    sealed class UpcomingMeetingsStateWithCounts {
        object Loading : UpcomingMeetingsStateWithCounts()
        object Empty : UpcomingMeetingsStateWithCounts()
        data class Error(val message: String) : UpcomingMeetingsStateWithCounts()
        data class Success(val meetings: List<MeetingWithCounts>) : UpcomingMeetingsStateWithCounts()
    }
}
