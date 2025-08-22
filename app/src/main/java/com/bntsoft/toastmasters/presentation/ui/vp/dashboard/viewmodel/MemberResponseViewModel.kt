package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.model.User
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
import javax.inject.Inject

@HiltViewModel
class MemberResponseViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val memberResponseRepository: MemberResponseRepository
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

                // Get all members (excluding VP Education) and responses in parallel
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

                val members = membersDeferred.await()
                val responses = responsesDeferred.await()

                Log.d(
                    "MemberResponseVM",
                    "Fetched ${members.size} members and ${responses.size} responses for meeting $meetingId"
                )

                // Process the data
                val uiState = processMemberResponses(members, responses)
                _uiState.value = uiState

                // Log the UI state
                if (uiState is MemberResponseUiState.Success) {
                    Log.d(
                        "MemberResponseVM",
                        "UI State - Available: ${uiState.availableMembers.size}, " +
                                "Not Available: ${uiState.notAvailableMembers.size}, " +
                                "Not Confirmed: ${uiState.notConfirmedMembers.size}, " +
                                "Not Responded: ${uiState.notRespondedMembers.size}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value =
                    MemberResponseUiState.Error("Failed to load member responses: ${e.message}")
            }
        }
    }

    private fun processMemberResponses(
        members: List<User>,
        responses: List<MemberResponse>
    ): MemberResponseUiState {
        Log.d(
            "MemberResponseVM",
            "Processing ${members.size} members and ${responses.size} responses"
        )
        return try {
            // Create a map of memberId to response for quick lookup
            val responseMap = responses.associateBy { it.memberId }

            // Create UI models for each member
            val memberResponses = members.map { member ->
                val response = responseMap[member.id]
                MemberResponseUiModel.fromUserAndResponse(member, response)
            }

            // Group by availability status
            val availableMembers = memberResponses.filter {
                it.availability == MemberResponse.AvailabilityStatus.AVAILABLE
            }
            val notAvailableMembers = memberResponses.filter {
                it.availability == MemberResponse.AvailabilityStatus.NOT_AVAILABLE
            }
            val notConfirmedMembers = memberResponses.filter {
                it.availability == MemberResponse.AvailabilityStatus.NOT_CONFIRMED
            }
            val notRespondedMembers = memberResponses.filter {
                it.availability == MemberResponse.AvailabilityStatus.NOT_RESPONDED
            }

            MemberResponseUiState.Success(
                availableMembers = availableMembers,
                notAvailableMembers = notAvailableMembers,
                notConfirmedMembers = notConfirmedMembers,
                notRespondedMembers = notRespondedMembers
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
            val notRespondedMembers: List<MemberResponseUiModel>
        ) : MemberResponseUiState()

        data class Error(val message: String) : MemberResponseUiState()
    }
}
