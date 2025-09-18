package com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates

import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.CreateMeetingFragment

sealed class CreateMeetingState {
    object Idle : CreateMeetingState()
    object Loading : CreateMeetingState()
    data class Success(val meeting: Meeting, val formData: CreateMeetingFragment.MeetingFormData) : CreateMeetingState()
    data class Error(val message: String) : CreateMeetingState()
    data class Duplicate(val meeting: Meeting) : CreateMeetingState()
}
