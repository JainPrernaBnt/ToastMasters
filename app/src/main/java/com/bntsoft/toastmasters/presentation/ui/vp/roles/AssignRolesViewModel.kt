package com.bntsoft.toastmasters.presentation.ui.vp.roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.presentation.ui.vp.roles.model.MeetingListItem
import com.bntsoft.toastmasters.utils.Result.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
                when (val result = meetingRepository.getMeetingsByStatus(MeetingStatus.NOT_COMPLETED)) {
                    is Success -> {
                        _meetings.value = result.data?.map { MeetingListItem.fromMeeting(it) }?.sortedBy { it.date } ?: emptyList()
                    }
                    is Error -> {
                        _error.value = result.message ?: "Failed to load meetings"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while loading meetings"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
