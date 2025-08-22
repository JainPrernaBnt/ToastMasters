package com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates

import com.bntsoft.toastmasters.domain.model.Meeting

sealed class MeetingsUiState {
    object Loading : MeetingsUiState()
    object Empty : MeetingsUiState()
    data class Error(val message: String) : MeetingsUiState()
    data class Success(val meetings: List<Meeting>) : MeetingsUiState()
}
