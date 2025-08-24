package com.bntsoft.toastmasters.presentation.ui.vp.roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.model.role.AssignRoleRequest
import com.bntsoft.toastmasters.domain.model.role.MemberRole
import com.bntsoft.toastmasters.domain.model.role.Role
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.RoleRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberRoleAssignmentViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val meetingRepository: MeetingRepository,
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentMeetingId: String = ""
    private var availableRoles: List<Role> = emptyList()
    private var currentAssignments: Map<String, MemberRole> = emptyMap()

    sealed class UiState {
        object Loading : UiState()
        data class Error(
            val message: String,
            val memberRoleItems: List<MemberRoleItem> = emptyList(),
            val availableRoles: List<Role> = emptyList(),
            val isLoading: Boolean = false
        ) : UiState()

        data class Success(
            val memberRoleItems: List<MemberRoleItem>,
            val availableRoles: List<Role>,
            val isLoading: Boolean = false
        ) : UiState()
    }

    data class MemberRoleItem(
        val userId: String,
        val userName: String,
        val currentRole: Role?,
        val preferredRoles: List<Role>,
        val pastRoles: List<Role>,
        val availableRoles: List<Role>
    )

    fun loadMembers(meeting: Meeting) {
        currentMeetingId = meeting.id
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                // Load all members and available roles in parallel
                val membersResult = userRepository.getAllMembers()
                    .map { users -> users.filter { it.isActive } }
                    .first()

                val rolesResult = roleRepository.getAllRoles()

                when (rolesResult) {
                    is Result.Success -> {
                        availableRoles = rolesResult.data
                        loadMemberAssignments(membersResult, meeting.id)
                    }

                    is Result.Error -> {
                        _uiState.value = UiState.Error(
                            message = rolesResult.message ?: "Failed to load roles",
                            availableRoles = availableRoles
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    message = e.message ?: "An error occurred while loading data",
                    availableRoles = availableRoles
                )
                Timber.e(e, "Error loading members and roles")
            }
        }
    }

    private suspend fun loadMemberAssignments(members: List<User>, meetingId: String) {
        try {
            val assignmentsResult = roleRepository.getMeetingRoleAssignments(meetingId)

            when (assignmentsResult) {
                is Result.Success -> {
                    currentAssignments = assignmentsResult.data.associateBy { it.memberId }
                    createMemberRoleItems(members, meetingId)
                }

                is Result.Error -> {
                    _uiState.value = UiState.Error(
                        message = assignmentsResult.message ?: "Failed to load role assignments",
                        availableRoles = availableRoles
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.value = UiState.Error(
                message = e.message ?: "An error occurred while loading role assignments",
                availableRoles = availableRoles
            )
            Timber.e(e, "Error loading role assignments")
        }
    }

    private suspend fun createMemberRoleItems(members: List<User>, meetingId: String) {
        try {
            // Load member preferences in parallel
            val memberPreferences = members.associate { member ->
                member.id to roleRepository.getMemberPreferences(member.id)
            }

            // Create member role items
            val memberRoleItems = members.map { member ->
                val preferences = (memberPreferences[member.id] as? Result.Success)?.data
                val currentRole = currentAssignments[member.id]?.let { assignment ->
                    availableRoles.find { it.id == assignment.roleId }
                }
                val preferredRoles = preferences?.preferredRoleIds?.mapNotNull { roleId ->
                    availableRoles.find { it.id == roleId }
                } ?: emptyList()

                // Get past roles (last 5)
                val pastRolesResult = roleRepository.getMemberRoleHistory(member.id, limit = 5)
                val pastRoles = when (pastRolesResult) {
                    is Result.Success -> pastRolesResult.data.mapNotNull { assignment ->
                        availableRoles.find { it.id == assignment.roleId }
                    }

                    else -> emptyList()
                }

                // Available roles are all roles not currently assigned to someone else
                val assignedRoleIds = currentAssignments.values
                    .filter { it.memberId != member.id }
                    .map { it.roleId }
                    .toSet()

                val availableRolesForMember = availableRoles.filter { role ->
                    role.id !in assignedRoleIds
                }

                MemberRoleItem(
                    userId = member.id,
                    userName = "${member.firstName} ${member.lastName}",
                    currentRole = currentRole,
                    preferredRoles = preferredRoles,
                    pastRoles = pastRoles,
                    availableRoles = availableRolesForMember
                )
            }

            _uiState.value = UiState.Success(
                memberRoleItems = memberRoleItems,
                availableRoles = availableRoles
            )

        } catch (e: Exception) {
            _uiState.value = UiState.Error(
                message = e.message ?: "Failed to create member role items",
                availableRoles = availableRoles
            )
            Timber.e(e, "Error creating member role items")
        }

        fun assignRole(memberId: String, role: Role?, assignedBy: String = "") {
            viewModelScope.launch {
                try {
                    val currentState = _uiState.value as? UiState.Success ?: return@launch

                    // Update UI optimistically
                    val updatedItems = currentState.memberRoleItems.map { item ->
                        if (item.userId == memberId) {
                            item.copy(currentRole = role)
                        } else if (role != null && item.currentRole?.id == role.id) {
                            // If this role is already assigned to someone else, clear it
                            item.copy(currentRole = null)
                        } else {
                            item
                        }
                    }

                    _uiState.value = currentState.copy(
                        memberRoleItems = updatedItems,
                        isLoading = true
                    )

                    // Save the assignment
                    if (role != null) {
                        val request = AssignRoleRequest(
                            meetingId = currentMeetingId,
                            memberId = memberId,
                            roleId = role.id,
                            assignedBy = assignedBy.ifEmpty { "system" },
                            notes = "Assigned by ${assignedBy.ifEmpty { "system" }}"
                        )

                        when (val result = meetingRepository.assignRole(request)) {
                            is Result.Success -> {
                                // Update current assignments with the new assignment
                                result.data.assignment?.let { assignment ->
                                    currentAssignments =
                                        currentAssignments.filterValues { it.memberId != memberId } +
                                                (memberId to assignment)
                                }

                                // Update the UI with the latest assignments
                                val successState =
                                    _uiState.value as? UiState.Success ?: return@launch
                                _uiState.value = successState.copy(
                                    memberRoleItems = updatedItems,
                                    isLoading = false
                                )
                            }

                            is Result.Error -> {
                                _uiState.value = UiState.Error(
                                    result.message ?: "Failed to assign role",
                                    currentState.memberRoleItems,
                                    currentState.availableRoles,
                                    isLoading = false
                                )
                                Timber.e("Error assigning role: ${result.message}")
                            }
                        }
                    } else {
                        // Handle role removal
                        val assignmentId = currentAssignments[memberId]?.id
                        if (assignmentId != null) {
                            when (meetingRepository.removeRoleAssignment(assignmentId)) {
                                is Result.Success -> {
                                    currentAssignments = currentAssignments - memberId
                                    // Update the UI with the latest assignments
                                    val successState =
                                        _uiState.value as? UiState.Success ?: return@launch
                                    _uiState.value = successState.copy(
                                        memberRoleItems = updatedItems,
                                        isLoading = false
                                    )
                                }

                                is Result.Error -> {
                                    _uiState.value = UiState.Error(
                                        "Failed to remove role assignment",
                                        currentState.memberRoleItems,
                                        currentState.availableRoles,
                                        isLoading = false
                                    )
                                    Timber.e("Error removing role assignment")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    val errorMessage = e.message ?: "An error occurred while assigning the role"
                    _uiState.value = (_uiState.value as? UiState.Success)?.let { successState ->
                        UiState.Error(
                            message = errorMessage,
                            memberRoleItems = successState.memberRoleItems,
                            availableRoles = successState.availableRoles,
                            isLoading = false
                        )
                    } ?: UiState.Error(errorMessage)

                    Timber.e(e, "Error in assignRole")
                }
            }
        }
    }
}
