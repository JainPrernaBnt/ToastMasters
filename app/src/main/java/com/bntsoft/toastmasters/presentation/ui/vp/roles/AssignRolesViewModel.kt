package com.bntsoft.toastmasters.presentation.ui.vp.roles

import android.util.Log
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
                val today = LocalDate.now()
                Log.d("AssignRolesVM", "Loading meetings from repository for date: $today")
                meetingRepository.getUpcomingMeetings(today)
                    .collectLatest { meetings ->
                        Log.d("AssignRolesVM", "Raw meetings from repository: ${meetings.size}")
                        meetings.forEach { 
                            Log.d("AssignRolesVM", "Meeting: ${it.id} - ${it.theme} - Status: ${it.status}")
                        }
                        
                        val filteredMeetings = meetings.filter { 
                            val isNotCompleted = it.status == MeetingStatus.NOT_COMPLETED
                            if (!isNotCompleted) {
                                Log.d("AssignRolesVM", "Filtering out completed meeting: ${it.id} - Status: ${it.status}")
                            }
                            isNotCompleted
                        }
                        
                        Log.d("AssignRolesVM", "After filtering: ${filteredMeetings.size} meetings")
                        
                        val meetingListItems = filteredMeetings.map { meeting ->
                            Log.d("AssignRolesVM", "Mapping meeting: ${meeting.id} - ${meeting.theme}")
                            MeetingListItem.fromMeeting(meeting).also {
                                Log.d("AssignRolesVM", "Mapped to MeetingListItem: ${it.theme} - ${it.dateTime}")
                            }
                        }.sortedBy { it.meeting.dateTime }
                        
                        Log.d("AssignRolesVM", "Setting ${meetingListItems.size} meetings to UI")
                        _meetings.value = meetingListItems

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
