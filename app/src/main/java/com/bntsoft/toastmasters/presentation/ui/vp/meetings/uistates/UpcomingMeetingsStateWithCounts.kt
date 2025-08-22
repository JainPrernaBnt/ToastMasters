package com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates

import com.bntsoft.toastmasters.domain.model.MeetingWithCounts

sealed class UpcomingMeetingsStateWithCounts {
    object Loading : UpcomingMeetingsStateWithCounts()
    object Empty : UpcomingMeetingsStateWithCounts()
    data class Error(val message: String) : UpcomingMeetingsStateWithCounts()
    data class Success(val meetings: List<MeetingWithCounts>) : UpcomingMeetingsStateWithCounts()
}
