package com.bntsoft.toastmasters.presentation.ui.vp.meetings.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.User
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.repository.MeetingAgendaRepository
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.CreateMeetingState
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.MeetingsUiState
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.UpcomingMeetingsState
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.UpcomingMeetingsStateWithCounts
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.UpdateMeetingState
import com.bntsoft.toastmasters.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import com.bntsoft.toastmasters.data.model.Meeting as DataMeeting
import com.bntsoft.toastmasters.domain.model.Meeting as DomainMeeting

@HiltViewModel
class MeetingsViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val meetingAgendaRepository: MeetingAgendaRepository,
    private val memberRepository: MemberRepository,
    private val memberResponseRepository: MemberResponseRepository,
    private val sharedPreferences: SharedPreferences,
    private val auth: FirebaseAuth
) : ViewModel() {

    companion object {
        private const val PREF_LATEST_OFFICERS = "latest_officers"
        private val DEFAULT_OFFICERS = mapOf(
            "President" to "TM Raghvendra Tiwari",
            "VPE" to "TM Sanchit Hundare",
            "VPM" to "TM Harshada Pandure",
            "Secretary" to "TM Manuel Tholath",
            "Treasurer" to "TM Sushant Ulavi",
            "SAA" to "TM Vaishali Menon",
            "IPP" to "TM Kiran Nawale",
            "VPPR" to "TM Suraj Krishnamoorthy"
        )
    }

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

    private val _updateMeetingState = MutableStateFlow<UpdateMeetingState>(UpdateMeetingState.Idle)
    val updateMeetingState: StateFlow<UpdateMeetingState> = _updateMeetingState.asStateFlow()

    // Store the latest officers to use for new meetings
    private var _latestOfficers = MutableStateFlow(loadLatestOfficers())
    val latestOfficers: StateFlow<Map<String, String>> = _latestOfficers.asStateFlow()

    private fun loadLatestOfficers(): Map<String, String> {
        return try {
            val json = sharedPreferences.getString(PREF_LATEST_OFFICERS, null)
            if (json.isNullOrEmpty()) {
                DEFAULT_OFFICERS
            } else {
                val type = object : TypeToken<Map<String, String>>() {}.type
                Gson().fromJson(json, type) ?: DEFAULT_OFFICERS
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading latest officers from SharedPreferences")
            DEFAULT_OFFICERS
        }
    }

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
            try {
                val result = meetingRepository.deleteMeeting(id)
                if (result is Resource.Success) {
                    // Refresh lists after successful deletion
                    loadMeetings()
                    loadUpcomingMeetings()
                } else if (result is Resource.Error) {
                    Timber.e("Failed to delete meeting: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting meeting")
            }
        }
    }

    fun updateMeeting(meeting: DataMeeting) = viewModelScope.launch {
        _updateMeetingState.value = UpdateMeetingState.Loading
        try {
            // Convert to domain model before updating
            val domainMeeting = meeting.toDomainModel()
            val result = meetingRepository.updateMeeting(domainMeeting)
            if (result is Resource.Success) {
                _updateMeetingState.value = UpdateMeetingState.Success(domainMeeting)
                loadMeetings()
                loadUpcomingMeetings()
            } else if (result is Resource.Error) {
                _updateMeetingState.value =
                    UpdateMeetingState.Error(result.message ?: "Failed to update meeting")
            }
        } catch (e: Exception) {
            _updateMeetingState.value = UpdateMeetingState.Error(e.message ?: "An error occurred")
        }
    }

    private suspend fun isDuplicateMeeting(meeting: DomainMeeting): Boolean {
        val existingMeetings = meetingRepository.getAllMeetings().first()
        return existingMeetings.any { existing ->
            existing.dateTime.toLocalDate() == meeting.dateTime.toLocalDate() &&
                    existing.dateTime.toLocalTime() == meeting.dateTime.toLocalTime() &&
                    existing.endDateTime?.toLocalTime() == meeting.endDateTime?.toLocalTime()
        }
    }

    private fun DataMeeting.toDomainModel(): DomainMeeting {
        // Convert data model to domain model
        val dateTime = this.date.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()

        return DomainMeeting(
            id = this.id,
            theme = this.theme,
            dateTime = dateTime,
            location = this.venue,
            officers = this.officers,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // Call this method whenever officers are updated in a meeting
    fun updateLatestOfficers(officers: Map<String, String>) {
        try {
            val json = Gson().toJson(officers)
            sharedPreferences.edit()
                .putString(PREF_LATEST_OFFICERS, json)
                .apply()
            _latestOfficers.value = officers
        } catch (e: Exception) {
            Timber.e(e, "Error saving latest officers to SharedPreferences")
        }
    }

    fun createMeeting(meeting: DataMeeting, forceCreate: Boolean = false) = viewModelScope.launch {
        _createMeetingState.value = CreateMeetingState.Loading

        try {
            // Convert to domain model for duplicate check
            val domainMeeting = meeting.toDomainModel()

            // Check for duplicate meetings if not forcing creation
            if (!forceCreate && isDuplicateMeeting(domainMeeting)) {
                _createMeetingState.value = CreateMeetingState.Duplicate(domainMeeting)
                return@launch
            }

            // Create the meeting with the latest officers
            val meetingWithOfficers = domainMeeting.copy(officers = _latestOfficers.value)
            val meetingResult = meetingRepository.createMeeting(meetingWithOfficers)

            if (meetingResult is Resource.Success) {
                val createdMeeting = meetingResult.data!!
                Timber.d("Meeting creation successful: $createdMeeting")

                // Create a default agenda for the meeting
                val defaultAgenda = MeetingAgenda(
                    id = createdMeeting.id,
                    meetingDate = Timestamp.now(),
                    startTime = createdMeeting.dateTime.toLocalTime().toString(),
                    endTime = createdMeeting.endDateTime?.toLocalTime()?.toString() ?: "",
                    officers = _latestOfficers.value,
                    agendaStatus = AgendaStatus.DRAFT,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                try {
                    val agendaResult = meetingAgendaRepository.saveMeetingAgenda(defaultAgenda)
                    if (agendaResult is com.bntsoft.toastmasters.utils.Result.Success<*>) {
                        Timber.d("Default agenda created successfully for meeting ${createdMeeting.id}")

                        // Also update the meeting with the agenda ID
                        meetingRepository.updateMeeting(
                            createdMeeting.copy(
                                agendaId = createdMeeting.id,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else if (agendaResult is com.bntsoft.toastmasters.utils.Result.Error) {
                        Timber.e("Failed to create default agenda: ${agendaResult.exception?.message}")
                        // Even if agenda creation fails, we still consider the meeting creation successful
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error saving default agenda")
                    // Even if agenda creation fails, we still consider the meeting creation successful
                }

                _createMeetingState.value = CreateMeetingState.Success(createdMeeting)
                loadMeetings()
                loadUpcomingMeetings()
            } else if (meetingResult is Resource.Error) {
                Timber.e("Meeting creation failed: ${meetingResult.message}")
                _createMeetingState.value = CreateMeetingState.Error(
                    meetingResult.message ?: "Failed to create meeting"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in createMeeting")
            _createMeetingState.value = CreateMeetingState.Error(
                e.message ?: "An unexpected error occurred"
            )
        }
    }

    suspend fun getLastMeetingDate(): java.util.Date? {
        val meetings = meetingRepository.getAllMeetings().first()
        return meetings.maxByOrNull { it.dateTime }?.dateTime?.let {
            java.util.Date.from(it.atZone(java.time.ZoneId.systemDefault()).toInstant())
        }
    }

    fun resetCreateMeetingState() {
        _createMeetingState.value = CreateMeetingState.Idle
    }
}
