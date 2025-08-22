package com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates

import com.bntsoft.toastmasters.domain.model.Meeting

sealed class UpcomingMeetingsState {
    object Loading : UpcomingMeetingsState()
    object Empty : UpcomingMeetingsState()
    data class Error(val message: String) : UpcomingMeetingsState()
    data class Success(val meetings: List<Meeting>) : UpcomingMeetingsState()
}