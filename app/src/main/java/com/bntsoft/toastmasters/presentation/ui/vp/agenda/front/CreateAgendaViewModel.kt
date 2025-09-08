package com.bntsoft.toastmasters.presentation.ui.vp.agenda.front

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import com.bntsoft.toastmasters.domain.repository.MeetingAgendaRepository
import com.bntsoft.toastmasters.utils.Result
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class CreateAgendaUiState(
    val agenda: MeetingAgenda = MeetingAgenda(
        id = "",
        meeting = Meeting(
            id = "",
            dateTime = LocalDateTime.now(),
            endDateTime = null,
            location = "",
            theme = "",
            status = MeetingStatus.NOT_COMPLETED,
            createdBy = ""
        ),
        meetingDate = Timestamp.now(),
        startTime = "",
        endTime = "",
        officers = emptyMap(),
        agendaStatus = AgendaStatus.DRAFT
    ),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isClubInfoEditable: Boolean = false,
    val isOfficersEditable: Boolean = false
)

@HiltViewModel
class CreateAgendaViewModel @Inject constructor(
    private val repository: MeetingAgendaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAgendaUiState())
    val uiState = _uiState.asStateFlow()

    fun loadMeeting(meetingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.getMeetingAgenda(meetingId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update { it.copy(agenda = result.data, isLoading = false) }
                        }

                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    error = result.exception?.message
                                        ?: "Failed to load meeting agenda",
                                    isLoading = false
                                )
                            }
                        }

                        is Result.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "An unexpected error occurred",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateTheme(theme: String) {
        _uiState.update { state ->
            state.copy(
                agenda = state.agenda.copy(
                    meeting = state.agenda.meeting.copy(theme = theme)
                ),
                isSaved = false
            )
        }
    }

    fun updateDate(date: Timestamp) {
        _uiState.update { state ->
            state.copy(
                agenda = state.agenda.copy(meetingDate = date),
                isSaved = false
            )
        }
    }

    fun updateStartTime(time: String) {
        _uiState.update { state ->
            state.copy(
                agenda = state.agenda.copy(startTime = time),
                isSaved = false
            )
        }
    }

    fun updateEndTime(time: String) {
        _uiState.update { state ->
            state.copy(
                agenda = state.agenda.copy(endTime = time),
                isSaved = false
            )
        }
    }

    fun updateVenue(venue: String) {
        _uiState.update { state ->
            state.copy(
                agenda = state.agenda.copy(
                    meeting = state.agenda.meeting.copy(location = venue)
                ),
                isSaved = false
            )
        }
    }

    fun updateOfficer(role: String, name: String) {
        _uiState.update { state ->
            val updatedOfficers = state.agenda.officers.toMutableMap().apply {
                this[role] = name
            }
            state.copy(
                agenda = state.agenda.copy(officers = updatedOfficers),
                isSaved = false
            )
        }
    }

    fun saveAgenda() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                when (val result = repository.saveMeetingAgenda(_uiState.value.agenda)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isSaving = false, isSaved = true) }
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                error = result.exception?.message ?: "Failed to save agenda",
                                isSaving = false
                            )
                        }
                    }

                    is Result.Loading -> {
                        _uiState.update { it.copy(isSaving = true) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "An unexpected error occurred",
                        isSaving = false
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun toggleClubInfoEdit() {
        _uiState.update { it.copy(isClubInfoEditable = !it.isClubInfoEditable) }
    }
    
    fun toggleOfficersEdit() {
        _uiState.update { it.copy(isOfficersEditable = !it.isOfficersEditable) }
    }
    
    fun updateClubName(name: String) {
        _uiState.update { state ->
            state.copy(
                agenda = state.agenda.copy(
                    meeting = state.agenda.meeting.copy(
                        location = name // Using location field to store club name for now
                    )
                ),
                isSaved = false
            )
        }
    }
    
    fun updateClubNumber(number: String) {
        // Store club number in a custom field or extend the model as needed
        _uiState.update { it.copy(isSaved = false) }
    }
    
    fun updateArea(area: String) {
        // Store area in a custom field or extend the model as needed
        _uiState.update { it.copy(isSaved = false) }
    }
    
    fun updateDistrict(district: String) {
        // Store district in a custom field or extend the model as needed
        _uiState.update { it.copy(isSaved = false) }
    }
    
    fun updateClubMission(mission: String) {
        // Store club mission in a custom field or extend the model as needed
        _uiState.update { it.copy(isSaved = false) }
    }

}
