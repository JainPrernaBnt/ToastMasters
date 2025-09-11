package com.bntsoft.toastmasters.presentation.ui.members.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.MemberRole
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeetingsRoleViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<MemberRole>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<MemberRole>>> = _uiState.asStateFlow()

    fun loadMemberRoles(meetingId: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val memberRoles = meetingRepository.getMemberRolesForMeeting(meetingId)
                _uiState.value = if (memberRoles.isNotEmpty()) {
                    UiState.Success(memberRoles)
                } else {
                    UiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load member roles")
            }
        }
    }
}