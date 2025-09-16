package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.MemberResponseUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class MemberResponseViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val memberResponseRepository: MemberResponseRepository,
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemberResponseUiState>(MemberResponseUiState.Loading)
    val uiState: StateFlow<MemberResponseUiState> = _uiState.asStateFlow()

    private var meetingId: String = ""

    fun loadMemberResponses(meetingId: String) {
        Log.d("MemberResponseVM", "Loading responses for meeting: $meetingId")
        this.meetingId = meetingId

        viewModelScope.launch {
            try {
                _uiState.value = MemberResponseUiState.Loading

                val meeting = meetingRepository.getMeetingById(meetingId)
                val meetingDateTime = meeting?.dateTime
                val cutoffDateMillis = meetingDateTime?.let { calculateCutoffDate(it) }

                Log.d(
                    "MemberResponseVM",
                    "Meeting date: $meetingDateTime, Cutoff date (ms): $cutoffDateMillis"
                )

                // 2️⃣ Fetch members, responses, and backouts in parallel
                val membersDeferred = async {
                    memberRepository.getAllMembers(includePending = false)
                        .first()
                        .filter { !it.isVpEducation }
                }

                val responsesDeferred = async {
                    try {
                        memberResponseRepository.getResponsesForMeeting(meetingId).first()
                    } catch (e: Exception) {
                        Log.e("MemberResponseVM", "Error getting responses: ${e.message}", e)
                        emptyList()
                    }
                }

                val backoutMembersDeferred = async {
                    try {
                        val allBackouts = memberResponseRepository.getBackoutMembers(meetingId)
                        Log.d("MemberResponseVM", "Fetched ${allBackouts.size} raw backout entries")

                        val filteredBackouts = allBackouts.filter { (memberId, timestamp) ->
                            val pass = cutoffDateMillis == null || timestamp >= cutoffDateMillis
                            Log.d(
                                "MemberResponseVM",
                                "Backout check for member=$memberId timestamp=$timestamp cutoff=$cutoffDateMillis -> include=$pass"
                            )
                            pass
                        }

                        filteredBackouts
                    } catch (e: Exception) {
                        Log.e("MemberResponseVM", "Error getting backout members: ${e.message}", e)
                        emptyList()
                    }
                }

                // 3️⃣ Await results
                val members = membersDeferred.await()
                val responses = responsesDeferred.await()
                val backoutMembers = backoutMembersDeferred.await()

                Log.d(
                    "MemberResponseVM",
                    "Fetched ${members.size} members, ${responses.size} responses, ${backoutMembers.size} backouts after filtering"
                )

                // 4️⃣ Process into UI state
                val uiState = processMemberResponses(members, responses, backoutMembers)
                _uiState.value = uiState

                // 5️⃣ Log summary
                if (uiState is MemberResponseUiState.Success) {
                    Log.d(
                        "MemberResponseVM",
                        "UI State -> Available=${uiState.availableMembers.size}, " +
                                "Not Available=${uiState.notAvailableMembers.size}, " +
                                "Not Confirmed=${uiState.notConfirmedMembers.size}, " +
                                "Not Responded=${uiState.notRespondedMembers.size}, " +
                                "Backout=${uiState.backoutMembers.size}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value =
                    MemberResponseUiState.Error("Failed to load member responses: ${e.message}")
            }
        }
    }

    private fun calculateCutoffDate(meetingDateTime: LocalDateTime): Long {
        // Go to Monday of the meeting week, 00:00:00
        val cutoffDateTime = meetingDateTime
            .with(java.time.DayOfWeek.MONDAY)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val cutoffMillis = cutoffDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        Log.d("MemberResponseVM", "Cutoff calculated: $cutoffDateTime (millis=$cutoffMillis)")
        return cutoffMillis
    }

    private fun processMemberResponses(
        members: List<User>,
        responses: List<MemberResponse>,
        backoutMembers: List<Pair<String, Long>>
    ): MemberResponseUiState {
        Log.d(
            "MemberResponseVM",
            "Processing ${members.size} members, ${responses.size} responses, and ${backoutMembers.size} backout members"
        )

        return try {
            // 1️⃣ Create maps for quick lookups
            val responseMap = responses.associateBy { it.memberId }
            val backoutMemberMap = backoutMembers.associate { it.first to it.second }

            // 2️⃣ Build member UI models with correct backout logic
            val memberResponses = members.map { member ->
                val response = responseMap[member.id]
                val backoutTimestamp = backoutMemberMap[member.id]

                // ✅ Only treat as backout if:
                // - There is a backout timestamp
                // - Response exists
                // - Status is NOT Available (means they backed out)
                val isBackout = backoutTimestamp != null &&
                        response != null &&
                        response.availability != MemberResponse.AvailabilityStatus.AVAILABLE

                val responseWithBackout = if (isBackout) {
                    response!!.copy(backoutTimestamp = backoutTimestamp)
                } else {
                    response
                }

                MemberResponseUiModel.fromUserAndResponse(member, responseWithBackout)
            }

            // 3️⃣ Group by availability status
            val availableMembers = memberResponses.filter {
                it.availability == MemberResponse.AvailabilityStatus.AVAILABLE &&
                        it.response?.backoutTimestamp == null
            }
            val notAvailableMembers = memberResponses.filter {
                it.availability == MemberResponse.AvailabilityStatus.NOT_AVAILABLE &&
                        it.response?.backoutTimestamp == null
            }
            val notConfirmedMembers = memberResponses.filter {
                it.availability == MemberResponse.AvailabilityStatus.NOT_CONFIRMED &&
                        it.response?.backoutTimestamp == null
            }
            val notRespondedMembers = memberResponses.filter {
                it.availability == MemberResponse.AvailabilityStatus.NOT_RESPONDED &&
                        it.response?.backoutTimestamp == null
            }

            // Backout members = only those who actually changed status after cutoff
            val backoutMemberResponses = memberResponses.filter {
                it.response?.backoutTimestamp != null &&
                        it.availability != MemberResponse.AvailabilityStatus.AVAILABLE
            }.sortedByDescending { it.response?.backoutTimestamp }

            MemberResponseUiState.Success(
                availableMembers = availableMembers,
                notAvailableMembers = notAvailableMembers,
                notConfirmedMembers = notConfirmedMembers,
                notRespondedMembers = notRespondedMembers,
                backoutMembers = backoutMemberResponses
            )
        } catch (e: Exception) {
            MemberResponseUiState.Error("Error processing member responses: ${e.message}")
        }
    }


    sealed class MemberResponseUiState {
        object Loading : MemberResponseUiState()
        data class Success(
            val availableMembers: List<MemberResponseUiModel>,
            val notAvailableMembers: List<MemberResponseUiModel>,
            val notConfirmedMembers: List<MemberResponseUiModel>,
            val notRespondedMembers: List<MemberResponseUiModel>,
            val backoutMembers: List<MemberResponseUiModel> = emptyList()
        ) : MemberResponseUiState()

        data class Error(val message: String) : MemberResponseUiState()
    }
}
