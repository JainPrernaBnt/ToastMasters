package com.bntsoft.toastmasters.presentation.ui.vp.meetings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.CompleteMeetingState
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.CreateMeetingState
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.MeetingsUiState
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.UpcomingMeetingsState
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.UpcomingMeetingsStateWithCounts
import com.bntsoft.toastmasters.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MeetingsViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val memberResponseRepository: MemberResponseRepository,
    private val memberRepository: MemberRepository
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

    private val _completeMeetingState =
        MutableStateFlow<CompleteMeetingState>(CompleteMeetingState.Idle)
    val completeMeetingState: StateFlow<CompleteMeetingState> = _completeMeetingState.asStateFlow()

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
            Timber.d("Loading upcoming meetings...")

            // Get all club members once
            val allMembers = try {
                val members = memberRepository.getAllMembers(includePending = false).first()
                Timber.d("Fetched ${members.size} active members")
                members
            } catch (e: Exception) {
                Timber.e(e, "Error fetching club members")
                emptyList<User>()
            }
            val totalMembers = allMembers.size
            Timber.d("Total active members: $totalMembers")

            meetingRepository.getUpcomingMeetings(LocalDate.now()).collectLatest { meetings ->
                if (meetings.isEmpty()) {
                    Timber.d("No upcoming meetings found")
                    _upcomingMeetingsStateWithCounts.value = UpcomingMeetingsStateWithCounts.Empty
                    return@collectLatest
                }

                Timber.d("Found ${meetings.size} upcoming meetings")
                val flows = meetings.map { meeting ->
                    if (!meeting.id.isNullOrBlank()) {
                        val responseFlow =
                            memberResponseRepository.getResponsesForMeeting(meeting.id)
                        responseFlow.collect { responses ->
                            Timber.d("Meeting ${meeting.id} has ${responses.size} responses")
                            responses.take(3).forEach { response ->
                                Timber.d("Response: member=${response.memberId}, status=${response.availability}")
                            }
                        }
                        responseFlow
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

                        // Calculate not responded as total members minus those who have responded
                        val totalResponses = responses.size
                        val notRespondedCount = (totalMembers - totalResponses).coerceAtLeast(0)

                        // Debug logging
                        Timber.d(
                            "Meeting ${meeting.id} - Available: $availableCount, " +
                                    "Not Available: $notAvailableCount, " +
                                    "Not Confirmed: $notConfirmedCount, " +
                                    "Not Responded: $notRespondedCount (Total members: $totalMembers, Responses: $totalResponses)"
                        )

                        // Log the first few responses for debugging
                        responses.take(3).forEach { response ->
                            Timber.d("Response - Member: ${response.memberId}, Status: ${response.availability}")
                        }

                        MeetingWithCounts(
                            meeting = meeting,
                            availableCount = availableCount,
                            notAvailableCount = notAvailableCount,
                            notConfirmedCount = notConfirmedCount,
                            notResponded = notRespondedCount
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

    fun completeMeeting(meetingId: String) {
        viewModelScope.launch {
            _completeMeetingState.value = CompleteMeetingState.Loading
            when (val result = meetingRepository.completeMeeting(meetingId)) {
                is Resource.Success -> {
                    _completeMeetingState.value = CompleteMeetingState.Success
                    // Refresh the meetings list to reflect the change
                    loadUpcomingMeetings()
                }

                is Resource.Error -> {
                    _completeMeetingState.value =
                        CompleteMeetingState.Error(result.message ?: "Failed to complete meeting")
                }

                is Resource.Loading -> _completeMeetingState.value = CompleteMeetingState.Loading
            }
        }
    }

    fun resetCompleteMeetingState() {
        _completeMeetingState.value = CompleteMeetingState.Idle
    }
}
