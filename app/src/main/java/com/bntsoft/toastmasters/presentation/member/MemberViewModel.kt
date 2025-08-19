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

/**
 * ViewModel for member-related operations.
 */
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

    /**
     * Loads pending member approvals.
     */
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

    /**
     * Loads available mentors.
     */
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

    /**
     * Approves a member's registration.
     * @param userId ID of the member to approve
     * @param mentorNames List of mentor names to assign
     * @param isNewMember Whether this is a new member
     * @param onComplete Callback with the result
     */
    fun approveMember(
        userId: String,
        mentorNames: List<String>,
        isNewMember: Boolean,
        onComplete: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = memberRepository.approveMember(userId, mentorNames, isNewMember)
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

    /**
     * Rejects a member's registration.
     * @param userId ID of the member to reject
     * @param reason Optional reason for rejection
     * @param onComplete Callback with the result
     */
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

    /**
     * Sealed class representing the member UI state.
     */
    sealed class MemberUiState {
        object Loading : MemberUiState()
        object Success : MemberUiState()
        object Empty : MemberUiState()
        data class Error(val message: String) : MemberUiState()
    }
}
