package com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates

import com.bntsoft.toastmasters.domain.model.Meeting

sealed class CreateMeetingState {
    object Idle : CreateMeetingState()
    object Loading : CreateMeetingState()
    data class Success(val meeting: Meeting) : CreateMeetingState()
    data class Error(val message: String) : CreateMeetingState()
    data class Duplicate(val meeting: Meeting) : CreateMeetingState()
}
