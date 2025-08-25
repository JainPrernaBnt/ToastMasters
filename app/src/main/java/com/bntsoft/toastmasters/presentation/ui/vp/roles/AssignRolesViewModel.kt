package com.bntsoft.toastmasters.presentation.ui.vp.roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.presentation.ui.vp.roles.model.MeetingListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AssignRolesViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {
    
    private val _meetings = MutableStateFlow<List<MeetingListItem>>(emptyList())
    val meetings = _meetings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadMeetings()
    }

    fun loadMeetings() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                meetingRepository.getUpcomingMeetings(LocalDate.now())
                    .collectLatest { meetings ->
                        _meetings.value = meetings
                            .filter { it.status == MeetingStatus.NOT_COMPLETED }
                            .map { MeetingListItem.fromMeeting(it) }
                            .sortedBy { it.meeting.dateTime }

                        // Stop loading once data is received
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while loading meetings"
                _isLoading.value = false
            }
        }
    }

}
