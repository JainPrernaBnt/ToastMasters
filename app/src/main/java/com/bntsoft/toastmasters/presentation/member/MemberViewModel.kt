package com.bntsoft.toastmasters.presentation.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemberUiState>(MemberUiState.Loading)
    val uiState: StateFlow<MemberUiState> = _uiState.asStateFlow()

    private val _pendingApprovals = MutableStateFlow<List<User>>(emptyList())
    val pendingApprovals: StateFlow<List<User>> = _pendingApprovals.asStateFlow()

    private val _mentors = MutableStateFlow<List<User>>(emptyList())
    val mentors: StateFlow<List<User>> = _mentors.asStateFlow()

    init {
        loadPendingApprovals()
        loadMentors()
    }

    fun loadPendingApprovals() {
        viewModelScope.launch {
            try {
                memberRepository.getPendingApprovals().collectLatest { users ->
                    _pendingApprovals.value = users
                    _uiState.value = if (users.isEmpty()) {
                        MemberUiState.Empty
                    } else {
                        MemberUiState.Success
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MemberUiState.Error("Failed to load pending approvals")
            }
        }
    }

    private fun loadMentors() {
        viewModelScope.launch {
            try {
                memberRepository.getMentors().collectLatest { mentorsList ->
                    _mentors.value = mentorsList
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun approveMember(
        userId: String,
        mentorNames: List<String>,
        onComplete: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = memberRepository.approveMember(userId, mentorNames)
                if (success) {
                    onComplete(Result.Success(true))
                } else {
                    onComplete(Result.Error(Exception("Failed to approve member")))
                }
            } catch (e: Exception) {
                onComplete(Result.Error(e))
            }
        }
    }

    fun rejectMember(
        userId: String,
        reason: String? = null,
        onComplete: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = memberRepository.rejectMember(userId, reason)
                if (success) {
                    onComplete(Result.Success(true))
                } else {
                    onComplete(Result.Error(Exception("Failed to reject member")))
                }
            } catch (e: Exception) {
                onComplete(Result.Error(e))
            }
        }
    }

    sealed class MemberUiState {
        object Loading : MemberUiState()
        object Success : MemberUiState()
        object Empty : MemberUiState()
        data class Error(val message: String) : MemberUiState()
    }
}
