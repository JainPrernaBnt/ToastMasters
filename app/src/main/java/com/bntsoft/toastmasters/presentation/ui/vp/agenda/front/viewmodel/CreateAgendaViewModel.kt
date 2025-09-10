package com.bntsoft.toastmasters.presentation.ui.vp.agenda.front.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import com.bntsoft.toastmasters.domain.repository.MeetingAgendaRepository
import com.bntsoft.toastmasters.domain.repository.AbbreviationRepository
import com.bntsoft.toastmasters.utils.Result
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            createdBy = "",
            clubName = "",
            clubNumber = "",
            area = "",
            district = "",
            mission = ""
        ),
        meetingDate = Timestamp.now(),
        startTime = "",
        endTime = "",
        officers = emptyMap(),
        agendaStatus = AgendaStatus.DRAFT,
        abbreviations = emptyMap()
    ),
    val grammarianDetails: GrammarianDetails = GrammarianDetails(),
    val isGrammarianDetailsLoading: Boolean = false,
    val isGrammarianDetailsSaving: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isClubInfoEditable: Boolean = false,
    val isOfficersEditable: Boolean = false,
// Add club info fields for editing
    val clubName: String = "",
    val clubNumber: String = "",
    val area: String = "",
    val district: String = "",
    val mission: String = "",
    // Abbreviations
    val newAbbreviation: String = "",
    val newMeaning: String = ""
)

@HiltViewModel
class CreateAgendaViewModel @Inject constructor(
    private val repository: MeetingAgendaRepository,
    private val abbreviationRepository: AbbreviationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAgendaUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _abbreviations = MutableStateFlow<Map<String, String>>(emptyMap())
    val abbreviations = _abbreviations.asStateFlow()

    fun loadAbbreviations(meetingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val abbreviations = abbreviationRepository.getAbbreviations(meetingId, meetingId) // Using meetingId as agendaId
                _abbreviations.value = abbreviations
                
                // Update the agenda with the loaded abbreviations
                _uiState.update { state ->
                    val updatedAgenda = state.agenda?.copy(abbreviations = abbreviations) ?: state.agenda
                    state.copy(agenda = updatedAgenda)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateNewAbbreviation(abbreviation: String) {
        _uiState.update { it.copy(newAbbreviation = abbreviation) }
    }

    fun updateNewMeaning(meaning: String) {
        _uiState.update { it.copy(newMeaning = meaning) }
    }

    fun addAbbreviation() {
        val state = _uiState.value
        val abbreviation = state.newAbbreviation.trim()
        val meaning = state.newMeaning.trim()

        if (abbreviation.isNotEmpty() && meaning.isNotEmpty()) {
            val currentAbbreviations = state.agenda.abbreviations.toMutableMap()
            currentAbbreviations[abbreviation] = meaning
            
            _uiState.update {
                it.copy(
                    agenda = it.agenda.copy(abbreviations = currentAbbreviations),
                    newAbbreviation = "",
                    newMeaning = ""
                )
            }
        }
    }

    fun removeAbbreviation(abbreviation: String) {
        val currentAbbreviations = _uiState.value.agenda.abbreviations.toMutableMap()
        currentAbbreviations.remove(abbreviation)
        
        _uiState.update {
            it.copy(agenda = it.agenda.copy(abbreviations = currentAbbreviations))
        }
    }

    suspend fun saveAbbreviations(meetingId: String, agendaId: String, abbreviations: Map<String, String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                abbreviationRepository.saveAbbreviations(meetingId, agendaId, abbreviations)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    fun loadMeeting(meetingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.getMeetingAgenda(meetingId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val agenda = result.data
                            _uiState.update { state ->
                                state.copy(
                                    agenda = agenda,
                                    // Initialize club info fields from the agenda
                                    clubName = agenda.meeting.clubName,
                                    clubNumber = agenda.meeting.clubNumber,
                                    district = agenda.meeting.district,
                                    area = agenda.meeting.area,
                                    mission = agenda.meeting.mission,
                                    isLoading = false,
                                    isSaved = false
                                )
                            }
                        }

                        is Result.Error -> {
                            _uiState.update { state ->
                                state.copy(
                                    error = result.exception?.message
                                        ?: "Failed to load meeting agenda",
                                    isLoading = false
                                )
                            }
                        }

                        Result.Loading -> {
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

    fun updateOfficer(role: String, name: String) {
        viewModelScope.launch(Dispatchers.Main) {
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

    fun clearSaveState() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun toggleClubInfoEdit() {
        _uiState.update { state ->
            if (!state.isClubInfoEditable) {
                // When entering edit mode, ensure we have the latest data from the agenda
                state.copy(
                    isClubInfoEditable = true,
                    clubName = state.agenda.meeting.clubName,
                    clubNumber = state.agenda.meeting.clubNumber,
                    district = state.agenda.meeting.district,
                    area = state.agenda.meeting.area,
                    mission = state.agenda.meeting.mission
                )
            } else {
                // When exiting edit mode without saving
                state.copy(isClubInfoEditable = false)
            }
        }
    }

    fun toggleOfficersEdit() {
        _uiState.update { it.copy(isOfficersEditable = !it.isOfficersEditable) }
    }

    fun updateClubName(name: String) {
        _uiState.update { state ->
            state.copy(
                clubName = name,
                isSaved = false
            )
        }
    }

    fun updateClubNumber(number: String) {
        _uiState.update { state ->
            state.copy(
                clubNumber = number,
                isSaved = false
            )
        }
    }

    fun updateArea(area: String) {
        _uiState.update { state ->
            state.copy(
                area = area,
                isSaved = false
            )
        }
    }

    fun updateDistrict(district: String) {
        _uiState.update { state ->
            state.copy(
                district = district,
                isSaved = false
            )
        }
    }

    fun updateClubMission(mission: String) {
        _uiState.update { state ->
            state.copy(
                mission = mission,
                isSaved = false
            )
        }
    }

    fun saveClubInfo() {
        viewModelScope.launch {
            try {
                // First, update the local state to ensure all fields are captured
                _uiState.update { state ->
                    val updatedAgenda = state.agenda.copy(
                        meeting = state.agenda.meeting.copy(
                            clubName = state.clubName,
                            clubNumber = state.clubNumber,
                            district = state.district,
                            area = state.area,
                            mission = state.mission
                        )
                    )
                    state.copy(
                        agenda = updatedAgenda,
                        isSaving = true,
                        error = null
                    )
                }

                // Get the latest state after updating
                val currentState = _uiState.value

                Log.d("SaveClubInfo", "Saving club info: ${currentState.agenda.meeting.clubName}, ${currentState.agenda.meeting.clubNumber}, ${currentState.agenda.meeting.district}, ${currentState.agenda.meeting.area}, ${currentState.agenda.meeting.mission}")

                val result = withContext(Dispatchers.IO) {
                    try {
                        repository.updateClubInfo(
                            meetingId = currentState.agenda.id,
                            clubName = currentState.agenda.meeting.clubName,
                            clubNumber = currentState.agenda.meeting.clubNumber,
                            district = currentState.agenda.meeting.district,
                            area = currentState.agenda.meeting.area,
                            mission = currentState.agenda.meeting.mission
                        )
                    } catch (e: Exception) {
                        Log.e("SaveClubInfo", "Error saving club info", e)
                        Result.Error(e)
                    }
                }

                _uiState.update { state ->
                    when (result) {
                        is Result.Success -> {
                            // Update the local agenda with the new club info
                            val updatedAgenda = state.agenda.copy(
                                meeting = state.agenda.meeting.copy(
                                    clubName = state.clubName,
                                    clubNumber = state.clubNumber,
                                    district = state.district,
                                    area = state.area,
                                    mission = state.mission
                                )
                            )
                            state.copy(
                                agenda = updatedAgenda,
                                isSaving = false,
                                isClubInfoEditable = false,
                                isSaved = true
                            )
                        }
                        is Result.Error -> state.copy(
                            isSaving = false,
                            error = result.exception?.message ?: "Failed to save club info"
                        )
                        Result.Loading -> state
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Error saving club info"
                    )
                }
            }
        }
    }


    fun saveOfficers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isSaving = true, error = null) }

                val currentAgenda = _uiState.value.agenda

                // Run Firebase updates in parallel if needed
                val result = try {
                    // Use coroutine scope for parallel updates
                    coroutineScope {
                        val agendaUpdate = async {
                            repository.updateOfficers(currentAgenda.id, currentAgenda.officers)
                        }
                        agendaUpdate.await() // suspend until finished
                    }
                } catch (e: Exception) {
                    Result.Error(e)
                }

                // Update UI
                _uiState.update {
                    when (result) {
                        is Result.Success -> it.copy(
                            isSaving = false,
                            isOfficersEditable = false,
                            isSaved = true
                        )

                        is Result.Error -> it.copy(
                            isSaving = false,
                            error = result.exception?.message ?: "Failed to save officers"
                        )

                        Result.Loading -> it.copy(isSaving = true)
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Error saving officers"
                    )
                }
            }
        }
    }


    fun loadGrammarianDetails(meetingId: String) {
        viewModelScope.launch {
            Log.d("GrammarianDetails", "Loading grammarian details for meeting: $meetingId")
            _uiState.update { it.copy(isGrammarianDetailsLoading = true, error = null) }
            try {
                val details = repository.getGrammarianDetails(meetingId)
                Log.d("GrammarianDetails", "Loaded grammarian details: $details")
                _uiState.update { state ->
                    state.copy(
                        grammarianDetails = details,
                        isGrammarianDetailsLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("GrammarianDetails", "Error loading grammarian details", e)
                val errorMessage =
                    "Failed to load grammarian details: ${e.message ?: "Unknown error"}"
                _uiState.update {
                    it.copy(
                        error = errorMessage,
                        isGrammarianDetailsLoading = false
                    )
                }
                // Clear the error after showing it
                viewModelScope.launch {
                    delay(3000)
                    _uiState.update { state ->
                        if (state.error == errorMessage) {
                            state.copy(error = null)
                        } else state
                    }
                }
            }
        }
    }

    fun updateGrammarianDetails(
        wordOfTheDay: String = _uiState.value.grammarianDetails.wordOfTheDay,
        wordMeaning: String = _uiState.value.grammarianDetails.wordMeaning.joinToString("\n"),
        wordExamples: String = _uiState.value.grammarianDetails.wordExamples.joinToString("\n"),
        idiomOfTheDay: String = _uiState.value.grammarianDetails.idiomOfTheDay,
        idiomMeaning: String = _uiState.value.grammarianDetails.idiomMeaning,
        idiomExamples: String = _uiState.value.grammarianDetails.idiomExamples.joinToString("\n")
    ) {
        _uiState.update { state ->
            state.copy(
                grammarianDetails = state.grammarianDetails.copy(
                    wordOfTheDay = wordOfTheDay,
                    wordMeaning = wordMeaning.split("\n").filter { it.isNotBlank() },
                    wordExamples = wordExamples.split("\n").filter { it.isNotBlank() },
                    idiomOfTheDay = idiomOfTheDay,
                    idiomMeaning = idiomMeaning,
                    idiomExamples = idiomExamples.split("\n").filter { it.isNotBlank() }
                )
            )
        }
    }

    fun saveGrammarianDetails() {
        viewModelScope.launch {
            val currentDetails = _uiState.value.grammarianDetails
            Log.d("GrammarianDetails", "Saving grammarian details: $currentDetails")
            _uiState.update { it.copy(isGrammarianDetailsSaving = true, error = null) }

            try {
                val result =
                    repository.saveGrammarianDetails(currentDetails.meetingID, currentDetails)
                Log.d("GrammarianDetails", "Save result: $result")

                if (result is Result.Success) {
                    // Update local state with the saved details
                    _uiState.update { state ->
                        state.copy(
                            grammarianDetails = currentDetails,
                            isGrammarianDetailsSaving = false
                        )
                    }
                    Log.d("GrammarianDetails", "Successfully saved grammarian details")
                    _uiState.update { it.copy(error = "Grammarian details saved successfully") }

                    // Clear success message after 3 seconds
                    viewModelScope.launch {
                        delay(3000)
                        _uiState.update { state ->
                            if (state.error == "Grammarian details saved successfully") {
                                state.copy(error = null)
                            } else state
                        }
                    }
                } else {
                    val error = (result as? Result.Error)?.exception?.message ?: "Unknown error"
                    val errorMessage = "Failed to save grammarian details: $error"
                    Log.e("GrammarianDetails", errorMessage)
                    _uiState.update {
                        it.copy(
                            error = errorMessage,
                            isGrammarianDetailsSaving = false
                        )
                    }

                    // Clear error after 5 seconds
                    viewModelScope.launch {
                        delay(5000)
                        _uiState.update { state ->
                            if (state.error == errorMessage) {
                                state.copy(error = null)
                            } else state
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMessage =
                    "Failed to save grammarian details: ${e.message ?: "Unknown error"}"
                Log.e("GrammarianDetails", errorMessage, e)
                _uiState.update {
                    it.copy(
                        error = errorMessage,
                        isGrammarianDetailsSaving = false
                    )
                }

                // Clear error after 5 seconds
                viewModelScope.launch {
                    delay(5000)
                    _uiState.update { state ->
                        if (state.error == errorMessage) {
                            state.copy(error = null)
                        } else state
                    }
                }
            }
        }
    }

}
