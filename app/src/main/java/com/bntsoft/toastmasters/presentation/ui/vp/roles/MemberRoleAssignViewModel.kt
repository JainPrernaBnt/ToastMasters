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
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MemberRoleAssignViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val meetingRepository: MeetingRepository,
    private val savedStateHandle: SavedStateHandle,
    private val firestore: FirebaseFirestore
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

                // Load speaker to evaluators mapping
                val speakerEvaluatorsMap = loadSpeakerEvaluators(meetingId)
                Log.d("MemberRoleAssignVM", "Speaker to evaluators mapping: $speakerEvaluatorsMap")
                
                Log.d("MemberRoleAssignVM", "Loaded evaluator assignments: $speakerEvaluatorsMap")

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
                    
                    // Get evaluator IDs for this speaker if they are a speaker
                    val evaluatorIds = if (selectedRoles.any { it.startsWith("Speaker") }) {
                        speakerEvaluatorsMap[member.id] ?: emptyList()
                    } else {
                        emptyList()
                    }
                    
                    Log.d(
                        "MemberRoleAssignVM",
                        "[loadRoleAssignments] Evaluators for speaker ${member.name}: $evaluatorIds"
                    )

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
                        allAssignedRoles = allAssignedRoles,
                        evaluatorIds = evaluatorIds
                    )
                    Log.d(
                        "MemberRoleAssignVM",
                        "[loadRoleAssignments] Created RoleAssignmentItem for ${member.name}: " +
                                "assignedRole=$assignedRole, selectedRoles=$selectedRoles, isEditable=$isEditable, evaluatorIds=$evaluatorIds"
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

        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            when {
                assignment.userId == userId -> {
                    val updated = assignment.withRoleRemoved(role)
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
        _roleAssignments.value = _roleAssignments.value?.map { assignment ->
            if (assignment.userId == userId) {
                val updated = assignment.copyWithEditMode(isEditable)
                updated
            } else {
                // If we're enabling edit mode for one user, ensure others are not in edit mode
                if (isEditable && assignment.isEditable) {
                    val updated = assignment.copyWithEditMode(false)
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
                    return@launch
                }

                if (currentAssignments.isEmpty()) {
                    return@launch
                }
                // Filter out assignments without roles
                val validAssignments = currentAssignments.filter { it.assignedRole.isNotBlank() }

                if (validAssignments.isEmpty()) {
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
                _errorMessage.value = "Error saving role assignments: ${e.message}"
            }
        }
    }

    private suspend fun loadSpeakerEvaluators(meetingId: String): Map<String, List<String>> {
        return try {
            // First get all assigned roles
            val snapshot = firestore.collection("meetings")
                .document(meetingId)
                .collection("assignedRole")
                .get()
                .await()


            snapshot.associate { doc ->
                val userId = doc.id
                val roles = doc.get("roles") as? List<*> ?: emptyList<String>()
                val evaluatorIds = (doc.get("evaluatorIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                // Only include if this user has a speaker role
                val hasSpeakerRole = roles.any { it.toString().startsWith("Speaker") }

                if (hasSpeakerRole) {
                    userId to evaluatorIds
                } else {
                    userId to emptyList()
                }
            }.filterValues { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun assignEvaluator(speakerId: String, evaluatorId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val currentAssignments = _roleAssignments.value ?: emptyList()
                val speakerAssignment = currentAssignments.find { it.userId == speakerId } ?: return@launch
                
                // Check if this is a removal request
                val isRemoval = evaluatorId.endsWith(":remove")
                val actualEvaluatorId = if (isRemoval) evaluatorId.removeSuffix(":remove") else evaluatorId
                
                // Update the speaker's evaluator list
                val updatedEvaluatorIds = if (isRemoval) {
                    speakerAssignment.evaluatorIds.filter { it != actualEvaluatorId }
                } else {
                    if (speakerAssignment.evaluatorIds.contains(actualEvaluatorId)) {
                        // Already added, nothing to do
                        return@launch
                    }
                    speakerAssignment.evaluatorIds + actualEvaluatorId
                }
                
                // Find the evaluator's name
                val evaluatorName = availableMembers.value?.find { it.first == actualEvaluatorId }?.second
                if (evaluatorName == null && !isRemoval) {
                    val error = "Evaluator not found in available members. ID: $actualEvaluatorId"
                    Log.e("MemberRoleAssignVM", error)
                    _errorMessage.value = error
                    return@launch
                }
                
                Log.d(
                    "MemberRoleAssignVM",
                    if (isRemoval) 
                        "Removing evaluator: $evaluatorName (ID: $actualEvaluatorId) from speaker: $speakerId"
                    else 
                        "Assigning evaluator: $evaluatorName (ID: $actualEvaluatorId) to speaker: $speakerId"
                )
                
                // First, update the local state immediately for better UX
                val updatedAssignments = currentAssignments.map { assignment ->
                    when {
                        assignment.userId == speakerId -> 
                            assignment.copy(evaluatorIds = updatedEvaluatorIds)
                        
                        !isRemoval && assignment.userId == actualEvaluatorId -> {
                            // Add evaluator role if not already present
                            if (!assignment.selectedRoles.any { it.startsWith("Evaluator") }) {
                                // Get the next available evaluator number
                                val nextNumber = (currentAssignments
                                    .flatMap { it.selectedRoles }
                                    .mapNotNull { role ->
                                        """Evaluator (\d+)""".toRegex().find(role)?.groupValues?.get(1)?.toInt()
                                    }
                                    .maxOrNull() ?: 0) + 1
                                
                                val evaluatorRole = "Evaluator $nextNumber"
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
                
                // Update the UI immediately
                _roleAssignments.value = updatedAssignments
                _evaluatorAssigned.value = speakerId to actualEvaluatorId
                
                // Then update Firestore
                when (val result = meetingRepository.updateSpeakerEvaluators(
                    meetingId = meetingId,
                    speakerId = speakerId,
                    evaluatorIds = updatedEvaluatorIds
                )) {
                    is Result.Success -> {
                        Log.d(
                            "MemberRoleAssignVM",
                            "Successfully updated speaker evaluators in Firestore"
                        )
                    }
                    
                    is Result.Error -> {
                        // Revert the local state if the Firestore update fails
                        _roleAssignments.value = currentAssignments
                        val error = "Failed to ${if (isRemoval) "remove" else "assign"} evaluator: ${result.exception?.message ?: "Unknown error"}"
                        Log.e("MemberRoleAssignVM", error, result.exception)
                        _errorMessage.value = error
                    }
                    
                    is Result.Loading -> {
                        // Loading state handled by UI if needed
                    }
                }
            } catch (e: Exception) {
                val error = "Error ${if (evaluatorId.endsWith(":remove")) "removing" else "assigning"} evaluator: ${e.message}"
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

    data class RoleDisplayItem(
        val role: String,
        val displayName: String,
        val isAvailable: Boolean,
        val isAssignedToUser: Boolean,
        val maxCount: Int,
        val remainingSlots: Int
    )
}