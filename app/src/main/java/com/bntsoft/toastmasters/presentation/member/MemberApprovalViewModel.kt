package com.bntsoft.toastmasters.presentation.member

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
                memberRepository.approveMember(member.id, emptyList(), member.isNewMember)
                _successMessages.value = "${member.name} has been approved"
                loadMembers() // Refresh the list
            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to approve member"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectMember(member: DomainUser) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                memberRepository.rejectMember(member.id)
                _successMessages.value = "${member.name} has been rejected"
                loadMembers() // Refresh the list
            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to reject member"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveAll() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val pendingMembers = _members.value.filter { it.isApproved.not() }
                pendingMembers.forEach { member ->
                    memberRepository.approveMember(member.id, emptyList(), member.isNewMember)
                }
                _successMessages.value = "${pendingMembers.size} members approved"
                loadMembers() // Refresh the list
            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to approve all members"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignMentor(member: DomainUser, mentorName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedMentorNames = member.mentorNames.toMutableList().apply {
                    if (!contains(mentorName)) add(mentorName)
                }
                val updatedMember = member.copy(mentorNames = updatedMentorNames)
                memberRepository.updateMember(updatedMember)
                _successMessages.value = "Mentor assigned successfully"
                loadMembers() // Refresh the list
            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to assign mentor"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignMentors(member: DomainUser, mentorNames: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cleaned = mentorNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                val updatedMember = member.copy(mentorNames = cleaned)
                memberRepository.updateMember(updatedMember)
                _successMessages.value = "Mentors updated successfully"
                loadMembers()
            } catch (e: Exception) {
                _errorMessages.value = e.message ?: "Failed to update mentors"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterMembers(query: String) {
        val filtered = when (currentFilter) {
            MemberApprovalFilter.ALL -> _members.value
            MemberApprovalFilter.NEW_MEMBERS -> _members.value.filter { it.isNewMember }
            MemberApprovalFilter.PENDING_APPROVAL -> _members.value.filter { !it.isApproved }
        }

        val result = if (query.isBlank()) {
            filtered
        } else {
            val lowerCaseQuery = query.lowercase()
            filtered.filter {
                it.name.lowercase().contains(lowerCaseQuery) ||
                it.email.lowercase().contains(lowerCaseQuery) ||
                it.phoneNumber.lowercase().contains(lowerCaseQuery)
            }
        }

        _filteredMembers.value = result
    }

    fun setFilter(filter: MemberApprovalFilter) {
        currentFilter = filter
        applyFilter(filter, _members.value)
    }

    private fun applyFilter(filter: MemberApprovalFilter, members: List<DomainUser>) {
        val filtered = when (filter) {
            MemberApprovalFilter.ALL -> members
            MemberApprovalFilter.NEW_MEMBERS -> members.filter { it.isNewMember }
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
