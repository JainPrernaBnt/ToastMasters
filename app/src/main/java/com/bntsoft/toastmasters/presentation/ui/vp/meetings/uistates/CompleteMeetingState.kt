package com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates

sealed class CompleteMeetingState {
    object Idle : CompleteMeetingState()
    object Loading : CompleteMeetingState()
    object Success : CompleteMeetingState()
    data class Error(val message: String) : CompleteMeetingState()
}