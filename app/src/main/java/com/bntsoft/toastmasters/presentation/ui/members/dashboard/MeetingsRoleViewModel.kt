package com.bntsoft.toastmasters.presentation.ui.members.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.data.model.MeetingRoleItem
import com.bntsoft.toastmasters.data.model.MemberRole
import com.bntsoft.toastmasters.data.model.SpeakerDetails
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeetingsRoleViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<MeetingRoleItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<MeetingRoleItem>>> = _uiState.asStateFlow()

    fun loadMemberRoles(meetingId: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                // Fetch member roles
                val memberRolesDeferred = async { meetingRepository.getMemberRolesForMeeting(meetingId) }
                
                // Fetch grammarian and speaker details in parallel
                val grammarianDetailsFlow = meetingRepository.getGrammarianDetailsForMeeting(meetingId)
                val speakerDetailsFlow = meetingRepository.getSpeakerDetailsForMeeting(meetingId)

                // Wait for all requests to complete
                val memberRoles = memberRolesDeferred.await()
                
                // Collect grammarian details
                val grammarianDetails = mutableListOf<GrammarianDetails>()
                grammarianDetailsFlow.collect { detailsList ->
                    grammarianDetails.addAll(detailsList)
                }
                
                // Collect speaker details
                val speakerDetails = mutableListOf<SpeakerDetails>()
                speakerDetailsFlow.collect { detailsList ->
                    speakerDetails.addAll(detailsList)
                }

                // Combine all items
                val items = mutableListOf<MeetingRoleItem>()
                
                // Add member roles
                items.addAll(memberRoles.map { MeetingRoleItem.MemberRoleItem(it) })
                
                // Add grammarian details
                grammarianDetails.forEach { details ->
                    if (details.wordOfTheDay.isNotBlank() || details.idiomOfTheDay.isNotBlank()) {
                        items.add(MeetingRoleItem.GrammarianDetails(
                            wordOfTheDay = details.wordOfTheDay,
                            wordMeaning = details.wordMeaning,
                            wordExamples = details.wordExamples,
                            idiomOfTheDay = details.idiomOfTheDay,
                            idiomMeaning = details.idiomMeaning,
                            idiomExamples = details.idiomExamples
                        ))
                    }
                }
                
                // Add speaker details
                speakerDetails.forEach { details ->
                    if (details.speechTitle.isNotBlank() || details.projectTitle.isNotBlank()) {
                        items.add(MeetingRoleItem.SpeakerDetails(
                            level = details.level,
                            pathwaysTrack = details.pathwaysTrack,
                            projectNumber = details.projectNumber,
                            projectTitle = details.projectTitle,
                            speechObjectives = details.speechObjectives,
                            speechTime = details.speechTime,
                            speechTitle = details.speechTitle
                        ))
                    }
                }

                _uiState.value = if (items.isNotEmpty()) {
                    UiState.Success(items)
                } else {
                    UiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load meeting details")
            }
        }
    }
}