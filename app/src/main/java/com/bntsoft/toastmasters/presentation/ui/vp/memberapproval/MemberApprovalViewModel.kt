package com.bntsoft.toastmasters.presentation.ui.vp.memberapproval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.models.MemberApprovalFilter
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.bntsoft.toastmasters.domain.model.User as DomainUser

@HiltViewModel
class MemberApprovalViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberApprovalUiState())
    val uiState: StateFlow<MemberApprovalUiState> = _uiState.asStateFlow()

    private val _members = MutableStateFlow<List<DomainUser>>(emptyList())
    val members = _members.asStateFlow()

    private val _filteredMembers = MutableStateFlow<List<DomainUser>>(emptyList())
    val filteredMembers = _filteredMembers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessages = MutableStateFlow("")
    val errorMessages = _errorMessages.asStateFlow()

    private val _successMessages = MutableStateFlow("")
    val successMessages = _successMessages.asStateFlow()

    private val _mentors = MutableStateFlow<List<DomainUser>>(emptyList())
    val mentors = _mentors.asStateFlow()

    var currentFilter: MemberApprovalFilter = MemberApprovalFilter.PENDING_APPROVAL
        private set

    init {
        loadMembers()
        loadMentors()
    }

    fun loadMembers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                memberRepository.getPendingApprovals().collectLatest { users ->
                    _members.value = users
                    applyFilter(currentFilter, users)
                }
            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to load members"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadMembers()
    }

    private fun loadMentors() {
        viewModelScope.launch {
            try {
                memberRepository.getMentors().collectLatest { users ->
                    _mentors.value = users
                }
            } catch (e: Exception) {
                // non-fatal; just log to error state optionally
            }
        }
    }

    fun approveMember(member: DomainUser) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ensure we have the latest mentor names from the UI
                val mentorNames = member.mentorNames

                // First update the member with the mentor names
                if (mentorNames.isNotEmpty()) {
                    val updatedMember = member.copy(mentorNames = mentorNames)
                    memberRepository.updateMember(updatedMember)
                }

                // Then approve the member
                val isApproved = memberRepository.approveMember(member.id, mentorNames)

                if (isApproved) {
                    _successMessages.value = "${member.name} has been approved"

                    // Update the local list
                    val updatedList = _members.value.toMutableList().apply {
                        removeAll { it.id == member.id }
                    }
                    _members.value = updatedList
                    applyFilter(currentFilter, updatedList)
                } else {
                    _errorMessages.value = "Failed to approve member. Please try again."
                }

            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to approve member"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectMember(member: DomainUser, reason: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = memberRepository.rejectMember(member.id, reason)
                
                if (success) {
                    _successMessages.value = "${member.name} has been rejected and moved to rejected members"

                    // Update the local list instead of reloading
                    val updatedList = _members.value.toMutableList().apply {
                        removeAll { it.id == member.id }
                    }
                    _members.value = updatedList
                    applyFilter(currentFilter, updatedList)
                } else {
                    _errorMessages.value = "Failed to reject member. Please try again."
                }

            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to reject member"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignMentor(member: DomainUser, mentorNames: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedMentorNames = member.mentorNames.toMutableList().apply {
                    mentorNames.forEach { name ->
                        if (!contains(name)) add(name) // avoid duplicates
                    }
                }
                val updatedMember = member.copy(mentorNames = updatedMentorNames)
                memberRepository.updateMember(updatedMember)  // yahi firebase me save karega
                _successMessages.value = "Mentor(s) assigned successfully"
                loadMembers() // Refresh UI
            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to assign mentor"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignVp(member: DomainUser) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val isPromoting = member.role != com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION
                val newRole = if (isPromoting) {
                    com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION
                } else {
                    com.bntsoft.toastmasters.domain.models.UserRole.MEMBER
                }
                
                // Create updated member with new role
                val updatedMember = member.copy(role = newRole)
                
                // Save to Firebase
                val updateSuccess = memberRepository.updateMember(updatedMember)
                
                if (updateSuccess) {
                    // Refresh the member data from Firebase to ensure consistency
                    val refreshedMember = memberRepository.getMemberById(member.id) ?: member
                    
                    _successMessages.value = if (isPromoting) {
                        "${member.name} is now VP Education"
                    } else {
                        "${member.name} is now a regular member"
                    }
                    
                    // Update local list with refreshed data
                    val updatedList = _members.value.toMutableList().map { 
                        if (it.id == member.id) refreshedMember else it 
                    }
                    _members.value = updatedList
                    applyFilter(currentFilter, updatedList)
                } else {
                    _errorMessages.value = "Failed to update member role. Please try again."
                }
            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to update member role"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyFilter(filter: MemberApprovalFilter, members: List<DomainUser>) {
        val filtered = when (filter) {
            MemberApprovalFilter.ALL -> members
            MemberApprovalFilter.PENDING_APPROVAL -> members.filter { !it.isApproved }
        }
        _filteredMembers.value = filtered
    }

    fun clearError() {
        _errorMessages.value = ""
    }

    fun clearSuccessMessage() {
        _successMessages.value = ""
    }
}

data class MemberApprovalUiState(
    val filteredMembers: List<DomainUser> = emptyList()
)
