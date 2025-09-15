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
import com.bntsoft.toastmasters.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

    private val _assignableRoles = MutableLiveData<Map<String, Int>>()
    val assignableRoles: LiveData<Map<String, Int>> = _assignableRoles

    // Track role counts and assignments
    private var roleCounts: Map<String, Int> = emptyMap()
    private var currentAssignments: Map<String, Int> = emptyMap()

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _availableMembers = MutableLiveData<List<Pair<String, String>>>()
    val availableMembers: LiveData<List<Pair<String, String>>> = _availableMembers

    private val _evaluatorAssigned =
        MutableLiveData<Pair<String, String>>() // speakerId to evaluatorId
    val evaluatorAssigned: LiveData<Pair<String, String>> = _evaluatorAssigned

    // Recent roles
    private val _recentRoles = MutableLiveData<Map<String, List<String>>>()
    val recentRoles: LiveData<Map<String, List<String>>> = _recentRoles

    private val meetingId: String = savedStateHandle["meeting_id"] ?: ""

    init {
        Log.d("MemberRoleAssignVM", "ViewModel initialized with meetingId: '$meetingId'")
        if (meetingId.isNotEmpty()) {
            Log.d("MemberRoleAssignVM", "Loading role assignments for meeting: $meetingId")
            loadRoleAssignments(meetingId)
            loadRecentRoles()
        } else {
            Log.e("MemberRoleAssignVM", "No meetingId provided in savedStateHandle")
            _roleAssignments.value = emptyList()
            _assignableRoles.value = emptyMap()
            _recentRoles.value = emptyMap()
        }
    }

    fun loadRoleAssignments(meetingId: String) {
        viewModelScope.launch {
            try {
                Log.d(
                    "MemberRoleAssignVM",
                    "[loadRoleAssignments] Starting for meeting: $meetingId"
                )
                val availableMembers = userRepository.getAvailableMembers(meetingId)
                _availableMembers.value = availableMembers.map { it.id to it.name }
                Log.d(
                    "MemberRoleAssignVM",
                    "[loadRoleAssignments] Found ${availableMembers.size} available members"
                )

                // Load assignable roles first
                // Load meeting roles with counts
                Log.d("MemberRoleAssignVM", "Fetching meeting roles...")
                val meeting = meetingRepository.getMeetingById(meetingId)
                val roles = meeting?.roleCounts ?: emptyMap()
                roleCounts = roles
                Log.d("MemberRoleAssignVM", "Found ${roles.size} meeting roles with counts")
                _assignableRoles.value = roles

                val allAssignedRoles = meetingRepository.getAllAssignedRoles(meetingId)

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

                    // Get assigned roles for this member if any
                    val selectedRoles = try {
                        Log.d(
                            "MemberRoleAssignVM",
                            "[loadRoleAssignments] Checking assigned roles for member: ${member.name} (${member.id})"
                        )
                        val roles = meetingRepository.getAssignedRoles(meetingId, member.id)
                        Log.d(
                            "MemberRoleAssignVM",
                            "[loadRoleAssignments] Assigned roles for ${member.name}: $roles"
                        )
                        roles.toMutableList()
                    } catch (e: Exception) {
                        Log.e(
                            "MemberRoleAssignVM",
                            "Error getting assigned roles for ${member.name}: ${e.message}"
                        )
                        mutableListOf<String>()
                    }

                    // The primary assigned role can be the first in the list, or empty if none are assigned
                    val assignedRole = selectedRoles.firstOrNull() ?: ""

                    // Set isEditable based on whether there are any assigned roles
                    val isEditable = selectedRoles.isEmpty()

                    // Calculate assigned role counts for this member's roles
                    val assignedRoleCounts = selectedRoles.groupingBy { it }.eachCount()

                    val roleItem = RoleAssignmentItem(
                        userId = member.id,
                        memberName = member.name,
                        preferredRoles = preferredRoles,
                        recentRoles = emptyList(),
                        assignableRoles = roles.keys.toList(),
                        selectedRoles = selectedRoles,
                        assignedRole = assignedRole,
                        isEditable = isEditable,
                        roleCounts = roles,
                        assignedRoleCounts = assignedRoleCounts,
                        allAssignedRoles = allAssignedRoles
                    )
                    Log.d(
                        "MemberRoleAssignVM",
                        "[loadRoleAssignments] Created RoleAssignmentItem for ${member.name}: " +
                                "assignedRole=$assignedRole, selectedRoles=$selectedRoles, isEditable=$isEditable"
                    )
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
                _assignableRoles.value = emptyMap()
            }
        }
    }

    fun getAvailableRoles(userId: String): List<RoleDisplayItem> {
        val currentAssignments = _roleAssignments.value ?: return emptyList()
        val userAssignment = currentAssignments.find { it.userId == userId } ?: return emptyList()

        val result = mutableListOf<RoleDisplayItem>()

        roleCounts.forEach { (baseRole, maxCount) ->
            // Get all instances of this role that are already assigned
            val assignedInstances = currentAssignments
                .flatMap { it.selectedRoles }
                .filter { it.startsWith(baseRole) }
                .toSet()

            // Get all possible instances of this role (e.g., "Speaker 1", "Speaker 2")
            val allInstances = (1..maxCount).map { "$baseRole $it" }

            // Find which instances are assigned to the current user
            val userAssignedInstances = userAssignment.selectedRoles
                .filter { it.startsWith(baseRole) }
                .toSet()

            // Check if this role is in the user's preferred roles
            val isPreferred = userAssignment.preferredRoles.any {
                it.equals(baseRole, ignoreCase = true) ||
                        it.startsWith(baseRole, ignoreCase = true)
            }

            if (!isPreferred) return@forEach

            // Add each role instance to the result
            allInstances.forEach { roleInstance ->
                val isAssigned = assignedInstances.contains(roleInstance)
                val isAssignedToUser = userAssignedInstances.contains(roleInstance)

                // Only show if it's available or assigned to this user
                if (!isAssigned || isAssignedToUser) {
                    val displayName = when {
                        isAssignedToUser -> "$roleInstance (Assigned to you)"
                        isAssigned -> "$roleInstance (Assigned to another member)"
                        else -> roleInstance
                    }

                    result.add(
                        RoleDisplayItem(
                            role = roleInstance,
                            displayName = displayName,
                            isAvailable = !isAssigned || isAssignedToUser,
                            isAssignedToUser = isAssignedToUser,
                            maxCount = maxCount,
                            remainingSlots = maxCount - assignedInstances.size
                        )
                    )
                }
            }
        }

        // If no roles are available but user has preferred roles, show them with 0 remaining slots
        if (result.isEmpty() && userAssignment.preferredRoles.isNotEmpty()) {
            userAssignment.preferredRoles.forEach { preferredRole ->
                val baseRole = preferredRole.split(" ")[0]
                val maxCount = roleCounts[baseRole] ?: 0

                result.add(
                    RoleDisplayItem(
                        role = preferredRole,
                        displayName = "$preferredRole (No slots available)",
                        isAvailable = false,
                        isAssignedToUser = false,
                        maxCount = maxCount,
                        remainingSlots = 0
                    )
                )
            }
        }

        // Sort with user's assigned roles first, then by role name
        return result.sortedWith(
            compareByDescending<RoleDisplayItem> { it.isAssignedToUser }
                .thenBy { it.role }
        )
    }

    fun isRoleAvailable(role: String, userId: String): Boolean {
        val currentAssignments = _roleAssignments.value ?: return false
        val maxCount = roleCounts[role] ?: 1
        val assignedCount = currentAssignments.flatMap { it.selectedRoles }.count { it == role }

        // Allow if there are available slots or the user already has this role
        return assignedCount < maxCount || currentAssignments.any {
            it.userId == userId && it.selectedRoles.contains(role)
        }
    }

    fun assignRole(userId: String, role: String) {
        Log.d("MemberRoleAssignVM", "Assigning role: $role to user: $userId")

        // Get current assignments to check role counts
        val currentAssignments = _roleAssignments.value ?: return
        val roleMaxCount = roleCounts[role] ?: 1
        val currentRoleCount = currentAssignments.flatMap { it.selectedRoles }.count { it == role }

        if (currentRoleCount > roleMaxCount) {
            _errorMessage.value = "All $role slots are already assigned"
            return
        }

        _roleAssignments.value = currentAssignments.map { assignment ->
            when {
                assignment.userId == userId -> {
                    val updated = assignment.withRoleAdded(role)
                    Log.d("MemberRoleAssignVM", "Role assigned. Updated assignment: $updated")

                    // If role is Speaker, emit event to show evaluator selection
                    if (role.startsWith("Speaker")) {
                        _evaluatorAssigned.postValue(userId to "")
                    }

                    updated
                }

                else -> {
                    // For other users, update their assigned role counts
                    val updatedAssignedCounts = assignment.assignedRoleCounts.toMutableMap()
                    val currentCount = updatedAssignedCounts[role] ?: 0
                    updatedAssignedCounts[role] = currentCount + 1
                    assignment.copy(assignedRoleCounts = updatedAssignedCounts)
                }
            }
        }
    }

    fun removeRole(userId: String, role: String) {
        Log.d("MemberRoleAssignVM", "Removing role: $role from user: $userId")

        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            when {
                assignment.userId == userId -> {
                    val updated = assignment.withRoleRemoved(role)
                    Log.d("MemberRoleAssignVM", "Role removed. Updated assignment: $updated")
                    updated
                }

                else -> {
                    // For other users, update their assigned role counts
                    val updatedAssignedCounts = assignment.assignedRoleCounts.toMutableMap()
                    val currentCount = updatedAssignedCounts[role] ?: 0
                    if (currentCount > 0) {
                        updatedAssignedCounts[role] = currentCount - 1
                        if (updatedAssignedCounts[role] == 0) {
                            updatedAssignedCounts.remove(role)
                        }
                    }
                    assignment.copy(assignedRoleCounts = updatedAssignedCounts)
                }
            }
        }
    }

    fun setBackupMember(userId: String, backupMemberId: String) {
        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            if (assignment.userId == userId) {
                // Find the backup member's name from available members
                val backupMemberName =
                    _availableMembers.value?.find { it.first == backupMemberId }?.second ?: ""
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
                    Log.d(
                        "MemberRoleAssignVM",
                        "Disabled edit mode for ${assignment.userId} as user $userId is now in edit mode"
                    )
                    updated
                } else {
                    assignment
                }
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

                when (val result =
                    meetingRepository.saveRoleAssignments(meetingId, validAssignments)) {
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
                        Log.e(
                            "MemberRoleAssignVM",
                            "Failed to save role assignments: ${result.exception?.message}"
                        )
                        // Optionally show error to user
                        _errorMessage.value =
                            "Failed to save role assignments: ${result.exception?.message}"
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

    fun assignEvaluator(speakerId: String, evaluatorId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Check if speaker already has an evaluator assigned
                val currentAssignments = _roleAssignments.value ?: emptyList()
                val speakerAssignment = currentAssignments.find { it.userId == speakerId }

                if (speakerAssignment?.evaluatorId == evaluatorId) {
                    Log.d(
                        "MemberRoleAssignVM",
                        "Speaker $speakerId already has evaluator $evaluatorId assigned"
                    )
                    return@launch
                }

                // Check if the evaluator already has an evaluator role
                val evaluatorAssignment = currentAssignments.find { it.userId == evaluatorId }
                val evaluatorName = availableMembers.value?.find { it.first == evaluatorId }?.second

                if (evaluatorName == null) {
                    val error = "Evaluator not found in available members. ID: $evaluatorId"
                    Log.e("MemberRoleAssignVM", error)
                    _errorMessage.value = error
                    return@launch
                }

                Log.d(
                    "MemberRoleAssignVM",
                    "Assigning evaluator: $evaluatorName (ID: $evaluatorId) to speaker: $speakerId"
                )

                // If evaluator doesn't have an evaluator role, assign one
                val evaluatorRole =
                    if (evaluatorAssignment?.selectedRoles?.any { it.startsWith("Evaluator") } != true) {
                        // Get the next available evaluator number
                        val nextNumber = (evaluatorAssignment?.selectedRoles
                            ?.mapNotNull { role ->
                                """Evaluator (\d+)""".toRegex().find(role)?.groupValues?.get(1)
                                    ?.toInt()
                            }?.maxOrNull() ?: 0) + 1
                        "Evaluator $nextNumber"
                    } else {
                        // Use existing evaluator role
                        evaluatorAssignment.selectedRoles.first { it.startsWith("Evaluator") }
                    }

                fun assignEvaluator(speakerId: String, evaluatorId: String) {
                    viewModelScope.launch {
                        try {
                            val result = meetingRepository.updateSpeakerEvaluator(
                                meetingId = meetingId,
                                speakerId = speakerId,
                                evaluatorName = "", // We'll update this after getting member name
                                evaluatorId = evaluatorId
                            )

                            when (result) {
                                is Result.Success -> {
                                    // Update the UI by finding the speaker and evaluator in the current assignments
                                    val currentAssignments = _roleAssignments.value ?: return@launch

                                    val updatedAssignments = currentAssignments.map { assignment ->
                                        when (assignment.userId) {
                                            speakerId -> assignment.copy(evaluatorId = evaluatorId)
                                            evaluatorId -> {
                                                // Add evaluator role if not already present
                                                if (!assignment.selectedRoles.any { it.startsWith("Evaluator") }) {
                                                    assignment.copy(
                                                        selectedRoles = (assignment.selectedRoles + "Evaluator").toMutableList(),
                                                        assignedRole = "Evaluator".takeIf { assignment.assignedRole.isEmpty() }
                                                            ?: assignment.assignedRole
                                                    )
                                                } else {
                                                    assignment
                                                }
                                            }

                                            else -> assignment
                                        }
                                    }

                                    _roleAssignments.value = updatedAssignments
                                    _evaluatorAssigned.value = speakerId to evaluatorId
                                }

                                is Result.Error -> {
                                    val error =
                                        "Failed to assign evaluator: ${result.exception?.message ?: "Unknown error"}"
                                    Log.e("MemberRoleAssignVM", error, result.exception)
                                    _errorMessage.value = error
                                }

                                is Result.Loading -> {
                                    // Loading state handled by UI if needed
                                }
                            }
                        } catch (e: Exception) {
                            val error = "Error assigning evaluator: ${e.message}"
                            Log.e("MemberRoleAssignVM", error, e)
                            _errorMessage.value = error
                        }
                    }
                }

                // Update the speaker's document with evaluator info
                when (val result = meetingRepository.updateSpeakerEvaluator(
                    meetingId = meetingId,
                    speakerId = speakerId,
                    evaluatorName = evaluatorName,
                    evaluatorId = evaluatorId
                )) {
                    is Result.Success -> {
                        Log.d(
                            "MemberRoleAssignVM",
                            "Successfully updated speaker evaluator in Firestore"
                        )

                        // Update the role assignments
                        val updatedAssignments = currentAssignments.map { assignment ->
                            when (assignment.userId) {
                                speakerId -> assignment.copy(evaluatorId = evaluatorId)
                                evaluatorId -> {
                                    // Add evaluator role if not already present
                                    if (!assignment.selectedRoles.any { it.startsWith("Evaluator") }) {
                                        assignment.copy(
                                            selectedRoles = (assignment.selectedRoles + evaluatorRole).toMutableList(),
                                            assignedRole = evaluatorRole.takeIf { assignment.assignedRole.isEmpty() }
                                                ?: assignment.assignedRole
                                        )
                                    } else {
                                        assignment
                                    }

                                }

                                else -> assignment
                            }
                        }

                        _roleAssignments.value = updatedAssignments
                        _evaluatorAssigned.value = speakerId to evaluatorId
                    }

                    is Result.Error -> {
                        val error =
                            "Failed to assign evaluator: ${result.exception?.message ?: "Unknown error"}"
                        Log.e("MemberRoleAssignVM", error, result.exception)
                        _errorMessage.value = error
                    }

                    is Result.Loading -> {
                        // Loading state handled by UI if needed
                    }
                }
            } catch (e: Exception) {
                val error = "Error assigning evaluator: ${e.message}"
                Log.e("MemberRoleAssignVM", error, e)
                _errorMessage.value = error
            }
        }
    }

    private fun loadRecentRoles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("MemberRoleAssignVM", "Loading recent roles...")
                val recentMeetings = meetingRepository.getRecentCompletedMeetings(3)
                Log.d(
                    "MemberRoleAssignVM",
                    "Found ${recentMeetings.size} recent completed meetings"
                )

                if (recentMeetings.isNotEmpty()) {
                    val meetingIds = recentMeetings.map { it.id }
                    val rolesByMeeting = meetingRepository.getRoleAssignmentsForMeetings(meetingIds)
                    // rolesByMeeting = Map<meetingId, Map<userId, List<role>>>

                    val recentRolesByUser = mutableMapOf<String, MutableList<String>>()

                    rolesByMeeting.forEach { (_, userRolesMap) ->
                        userRolesMap.forEach { (userId, rolesList) ->
                            rolesList.forEach { role ->
                                val cleanedRole = role.substringBeforeLast(" ")
                                recentRolesByUser.getOrPut(userId) { mutableListOf() }
                                    .add(cleanedRole)
                            }
                        }
                    }

                    // Make roles unique for each user
                    val distinctRolesByUser =
                        recentRolesByUser.mapValues { (_, roles) -> roles.distinct() }

                    _recentRoles.postValue(distinctRolesByUser)
                    Log.d(
                        "MemberRoleAssignVM",
                        "Loaded recent roles for ${distinctRolesByUser.size} users"
                    )
                } else {
                    _recentRoles.postValue(emptyMap())
                    Log.d("MemberRoleAssignVM", "No recent completed meetings found")
                }
            } catch (e: Exception) {
                Log.e("MemberRoleAssignVM", "Error loading recent roles: ${e.message}", e)
                _recentRoles.postValue(emptyMap())
            }
        }
    }


    fun getRecentRolesForUser(userId: String): List<String> {
        return _recentRoles.value?.get(userId) ?: emptyList()
    }

    data class RoleDisplayItem(
        val role: String,
        val displayName: String,
        val isAvailable: Boolean,
        val isAssignedToUser: Boolean,
        val maxCount: Int,
        val remainingSlots: Int
    )
}