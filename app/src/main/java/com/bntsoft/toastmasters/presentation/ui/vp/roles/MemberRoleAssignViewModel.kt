package com.bntsoft.toastmasters.presentation.ui.vp.roles

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.RoleAssignmentItem
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberRoleAssignViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val meetingRepository: MeetingRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _roleAssignments = MutableLiveData<List<RoleAssignmentItem>>()
    val roleAssignments: LiveData<List<RoleAssignmentItem>> = _roleAssignments

    private val _assignableRoles = MutableLiveData<List<String>>()
    val assignableRoles: LiveData<List<String>> = _assignableRoles

    private val _availableMembers = MutableLiveData<List<Pair<String, String>>>()
    val availableMembers: LiveData<List<Pair<String, String>>> = _availableMembers

    private val meetingId: String = savedStateHandle["meeting_id"] ?: ""

    init {
        Log.d("MemberRoleAssignVM", "ViewModel initialized with meetingId: '$meetingId'")
        if (meetingId.isNotEmpty()) {
            Log.d("MemberRoleAssignVM", "Loading role assignments for meeting: $meetingId")
            loadRoleAssignments(meetingId)
        } else {
            Log.e("MemberRoleAssignVM", "No meetingId provided in savedStateHandle")
            _roleAssignments.value = emptyList()
            _assignableRoles.value = emptyList()
        }
    }

    fun loadRoleAssignments(meetingId: String) {
        viewModelScope.launch {
            try {
                Log.d("MemberRoleAssignVM", "Fetching available members for meeting: $meetingId")
                val availableMembers = userRepository.getAvailableMembers(meetingId)
                _availableMembers.value = availableMembers.map { it.id to it.name }
                Log.d("MemberRoleAssignVM", "Found ${availableMembers.size} available members")

                // Create role assignments for available members
                val assignments = availableMembers.map { member ->
                    // Get preferred roles for this member
                    val preferredRoles = try {
                        meetingRepository.getPreferredRoles(meetingId, member.id).also { roles ->
                            Log.d(
                                "MemberRoleAssignVM",
                                "Found ${roles.size} preferred roles for member: ${member.name}"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "MemberRoleAssignVM",
                            "Error getting preferred roles for ${member.name}: ${e.message}"
                        )
                        emptyList<String>()
                    }

                    RoleAssignmentItem(
                        userId = member.id,
                        memberName = member.name,
                        preferredRoles = preferredRoles,
                        recentRoles = emptyList(),
                        assignableRoles = emptyList(),
                        selectedRoles = mutableListOf()
                    )
                }

                Log.d("MemberRoleAssignVM", "Fetching meeting roles...")
                val roles = meetingRepository.getMeetingRoles(meetingId)
                Log.d("MemberRoleAssignVM", "Found ${roles.size} meeting roles")

                _roleAssignments.value = assignments
                _assignableRoles.value = roles

                // Log if either list is empty
                if (assignments.isEmpty()) {
                    Log.w("MemberRoleAssignVM", "No role assignments found for meeting: $meetingId")
                }
                if (roles.isEmpty()) {
                    Log.w("MemberRoleAssignVM", "No assignable roles found")
                }
            } catch (e: Exception) {
                Log.e("MemberRoleAssignVM", "Error loading role assignments: ${e.message}", e)
                _roleAssignments.value = emptyList()
                _assignableRoles.value = emptyList()
            }
        }
    }

    fun assignRole(userId: String, role: String) {
        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            if (assignment.userId == userId) {
                val updatedRoles = assignment.selectedRoles.toMutableList()
                if (!updatedRoles.contains(role)) {
                    updatedRoles.add(role)
                }
                assignment.copy(selectedRoles = updatedRoles)
            } else {
                assignment
            }
        }
    }

    fun removeRole(userId: String, role: String) {
        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            if (assignment.userId == userId) {
                val updatedRoles = assignment.selectedRoles.toMutableList()
                updatedRoles.remove(role)
                assignment.copy(selectedRoles = updatedRoles)
            } else {
                assignment
            }
        }
    }

    fun setBackupMember(userId: String, backupMemberId: String) {
        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            if (assignment.userId == userId) {
                // Find the backup member's name from available members
                val backupMemberName = _availableMembers.value?.find { it.first == backupMemberId }?.second ?: ""
                assignment.copy(
                    backupMemberId = backupMemberId,
                    backupMemberName = backupMemberName
                )
            } else {
                assignment
            }
        }
    }
    
    fun toggleEditMode(userId: String, isEditable: Boolean) {
        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            if (assignment.userId == userId) {
                assignment.copyWithEditMode(isEditable)
            } else {
                assignment
            }
        }
    }
    
    fun loadRoleAssignments() {
        viewModelScope.launch {
            try {
                // Load your assignments here
                // After loading, ensure all assignments are in read-only mode initially
                _roleAssignments.value = _roleAssignments.value?.map { 
                    it.copyWithEditMode(false)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun saveRoleAssignments() {
        viewModelScope.launch {
            try {
                val currentAssignments = _roleAssignments.value ?: run {
                    Log.e(
                        "MemberRoleAssignVM",
                        "No assignments to save - currentAssignments is null"
                    )
                    return@launch
                }

                if (currentAssignments.isEmpty()) {
                    Log.w(
                        "MemberRoleAssignVM",
                        "No role assignments to save - assignments list is empty"
                    )
                    return@launch
                }

                Log.d(
                    "MemberRoleAssignVM",
                    "Saving ${currentAssignments.size} role assignments for meeting: $meetingId"
                )
                currentAssignments.forEach { assignment ->
                    Log.d(
                        "MemberRoleAssignVM",
                        "User: ${assignment.memberName} (${assignment.userId}), " +
                                "Selected Roles: ${assignment.selectedRoles.joinToString()}, " +
                                "Backup Member ID: ${assignment.backupMemberId}"
                    )
                }

                when (val result =
                    meetingRepository.saveRoleAssignments(meetingId, currentAssignments)) {
                    is com.bntsoft.toastmasters.utils.Result.Error -> Log.e(
                        "MemberRoleAssignVM",
                        "Failed to save role assignments: ${result.exception?.message}"
                    )

                    com.bntsoft.toastmasters.utils.Result.Loading -> TODO()
                    is com.bntsoft.toastmasters.utils.Result.Success ->
                        Log.d(
                            "MemberRoleAssignVM",
                            "Successfully saved role assignments for meeting: $meetingId"
                        )

                }
            } catch (e: Exception) {
                Log.e("MemberRoleAssignVM", "Error saving role assignments: ${e.message}", e)
            }
        }
    }
}