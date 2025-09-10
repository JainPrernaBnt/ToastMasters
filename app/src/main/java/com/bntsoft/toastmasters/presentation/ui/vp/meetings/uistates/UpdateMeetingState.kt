package com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates

import com.bntsoft.toastmasters.domain.model.Meeting

sealed class UpdateMeetingState {
    object Loading : UpdateMeetingState()
    data class Success(val meeting: Meeting) : UpdateMeetingState()
    data class Error(val message: String) : UpdateMeetingState()
    object Idle : UpdateMeetingState()
}
