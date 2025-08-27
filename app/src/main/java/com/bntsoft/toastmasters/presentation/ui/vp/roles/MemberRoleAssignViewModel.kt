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
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

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
                Log.d("MemberRoleAssignVM", "[loadRoleAssignments] Starting for meeting: $meetingId")
                val availableMembers = userRepository.getAvailableMembers(meetingId)
                _availableMembers.value = availableMembers.map { it.id to it.name }
                Log.d("MemberRoleAssignVM", "[loadRoleAssignments] Found ${availableMembers.size} available members")
                
                // Load assignable roles first
                Log.d("MemberRoleAssignVM", "Fetching meeting roles...")
                val roles = meetingRepository.getMeetingRoles(meetingId)
                Log.d("MemberRoleAssignVM", "Found ${roles.size} meeting roles")
                _assignableRoles.value = roles

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

                    // Get assigned role for this member if any
                    val assignedRole = try {
                        Log.d("MemberRoleAssignVM", "[loadRoleAssignments] Checking assigned role for member: ${member.name} (${member.id})")
                        val role = meetingRepository.getAssignedRole(meetingId, member.id)
                        Log.d("MemberRoleAssignVM", "[loadRoleAssignments] Assigned role for ${member.name}: $role")
                        role ?: ""
                    } catch (e: Exception) {
                        Log.e("MemberRoleAssignVM", "Error getting assigned role for ${member.name}: ${e.message}")
                        ""
                    }

                    val selectedRoles = if (assignedRole.isNotBlank()) {
                        Log.d("MemberRoleAssignVM", "[loadRoleAssignments] Member ${member.name} has role: $assignedRole")
                        mutableListOf(assignedRole)
                    } else {
                        Log.d("MemberRoleAssignVM", "[loadRoleAssignments] No role assigned for ${member.name}")
                        mutableListOf()
                    }
                    
                    // Set isEditable based on whether there's an assigned role
                    val isEditable = assignedRole.isBlank()

                    val roleItem = RoleAssignmentItem(
                        userId = member.id,
                        memberName = member.name,
                        preferredRoles = preferredRoles,
                        recentRoles = emptyList(),
                        assignableRoles = roles, // Use the loaded meeting roles
                        selectedRoles = selectedRoles,
                        assignedRole = assignedRole,
                        isEditable = isEditable // Set editable state based on assigned role
                    )
                    Log.d("MemberRoleAssignVM", "[loadRoleAssignments] Created RoleAssignmentItem for ${member.name}: " +
                            "assignedRole=$assignedRole, selectedRoles=$selectedRoles, isEditable=$isEditable")
                    roleItem
                }

                _roleAssignments.value = assignments

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
        Log.d("MemberRoleAssignVM", "Assigning role: $role to user: $userId")
        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            if (assignment.userId == userId) {
                val updated = assignment.withRoleAdded(role)
                Log.d("MemberRoleAssignVM", "Role assigned. Updated assignment: $updated")
                updated
            } else {
                // Just return the assignment as-is for other users
                // Multiple users can have the same role
                assignment
            }
        }
    }

    fun removeRole(userId: String, role: String) {
        Log.d("MemberRoleAssignVM", "Removing role: $role from user: $userId")
        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            if (assignment.userId == userId) {
                val updated = assignment.withRoleRemoved(role)
                Log.d("MemberRoleAssignVM", "Role removed. Updated assignment: $updated")
                updated
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
        Log.d("MemberRoleAssignVM", "Toggling edit mode for user: $userId, isEditable: $isEditable")
        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            if (assignment.userId == userId) {
                val updated = assignment.copyWithEditMode(isEditable)
                Log.d("MemberRoleAssignVM", "Updated assignment: $updated")
                updated
            } else {
                // If we're enabling edit mode for one user, ensure others are not in edit mode
                if (isEditable && assignment.isEditable) {
                    val updated = assignment.copyWithEditMode(false)
                    Log.d("MemberRoleAssignVM", "Disabled edit mode for ${assignment.userId} as user $userId is now in edit mode")
                    updated
                } else {
                    assignment
                }
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
                    Log.e("MemberRoleAssignVM", "No assignments to save - currentAssignments is null")
                    return@launch
                }

                if (currentAssignments.isEmpty()) {
                    Log.w("MemberRoleAssignVM", "No role assignments to save - assignments list is empty")
                    return@launch
                }

                Log.d("MemberRoleAssignVM", "Saving role assignments for meeting: $meetingId")
                
                // Filter out assignments without roles
                val validAssignments = currentAssignments.filter { it.assignedRole.isNotBlank() }
                
                if (validAssignments.isEmpty()) {
                    Log.w("MemberRoleAssignVM", "No valid role assignments to save")
                    return@launch
                }

                validAssignments.forEach { assignment ->
                    Log.d(
                        "MemberRoleAssignVM",
                        "Saving assignment - User: ${assignment.memberName} (${assignment.userId}), " +
                                "Role: ${assignment.assignedRole}, " +
                                "Backup Member ID: ${assignment.backupMemberId}"
                    )
                }

                when (val result = meetingRepository.saveRoleAssignments(meetingId, validAssignments)) {
                    is com.bntsoft.toastmasters.utils.Result.Success -> {
                        Log.d("MemberRoleAssignVM", "Successfully saved role assignments")
                        // Update the UI to reflect the saved state
                        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
                            if (validAssignments.any { it.userId == assignment.userId }) {
                                assignment.copy(isEditable = false)
                            } else {
                                assignment
                            }
                        }
                    }
                    is com.bntsoft.toastmasters.utils.Result.Error -> {
                        Log.e("MemberRoleAssignVM", "Failed to save role assignments: ${result.exception?.message}")
                        // Optionally show error to user
                        _errorMessage.value = "Failed to save role assignments: ${result.exception?.message}"
                    }
                    com.bntsoft.toastmasters.utils.Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                Log.e("MemberRoleAssignVM", "Error saving role assignments", e)
                _errorMessage.value = "Error saving role assignments: ${e.message}"
            }
        }
    }
}