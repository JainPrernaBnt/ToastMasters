package com.bntsoft.toastmasters.domain.model

data class RoleAssignmentItem(
    val userId: String,
    val memberName: String,
    val preferredRoles: List<String> = emptyList(),
    val recentRoles: List<String> = emptyList(),
    val assignableRoles: List<String> = emptyList(),
    var selectedRoles: MutableList<String> = mutableListOf(),
    val assignedRole: String = "",
    val backupMemberId: String = "",
    val backupMemberName: String = "",
    val isEditable: Boolean = false,
    val roleCounts: Map<String, Int> = emptyMap(),
    val assignedRoleCounts: Map<String, Int> = emptyMap(),
    val allAssignedRoles: Map<String, String> = emptyMap()
) {
    fun copyWithEditMode(editable: Boolean): RoleAssignmentItem {
        val newSelectedRoles = if (editable && selectedRoles.isEmpty() && assignedRole.isNotBlank()) {
            mutableListOf(assignedRole)
        } else {
            selectedRoles.toMutableList()
        }
        
        return this.copy(
            isEditable = editable,
            selectedRoles = newSelectedRoles
        )
    }

    fun withRoleRemoved(role: String): RoleAssignmentItem {
        val updatedRoles = selectedRoles.toMutableList().apply { remove(role) }
        
        // Get the base role name (e.g., "Speaker" from "Speaker 1")
        val baseRole = role.split(" ")[0]
        
        // Update assigned role counts using base role name
        val updatedAssignedCounts = assignedRoleCounts.toMutableMap()
        val currentCount = updatedAssignedCounts[baseRole] ?: 0
        if (currentCount > 0) {
            updatedAssignedCounts[baseRole] = currentCount - 1
            if (updatedAssignedCounts[baseRole] == 0) {
                updatedAssignedCounts.remove(baseRole)
            }
        }
        
        return this.copy(
            selectedRoles = updatedRoles,
            // If we removed the assigned role, clear it or assign the first available role
            assignedRole = when {
                assignedRole != role -> assignedRole
                updatedRoles.isNotEmpty() -> updatedRoles[0]
                else -> ""
            },
            assignedRoleCounts = updatedAssignedCounts
        )
    }

    fun withRoleAdded(role: String): RoleAssignmentItem {
        val updatedRoles = selectedRoles.toMutableList().apply { 
            if (!contains(role)) add(role) 
        }
        
        // Get the base role name (e.g., "Speaker" from "Speaker 1")
        val baseRole = role.split(" ")[0]
        
        // Update assigned role counts using base role name
        val updatedAssignedCounts = assignedRoleCounts.toMutableMap()
        val currentCount = updatedAssignedCounts[baseRole] ?: 0
        updatedAssignedCounts[baseRole] = currentCount + 1
        
        return this.copy(
            selectedRoles = updatedRoles,
            assignedRole = if (assignedRole.isEmpty()) role else assignedRole,
            assignedRoleCounts = updatedAssignedCounts
        )
    }

    fun canAssignRole(role: String): Boolean {
        val maxCount = roleCounts[role] ?: 1 // Default to 1 if no count specified
        val assignedCount = assignedRoleCounts[role] ?: 0
        return assignedCount < maxCount
    }

    fun getRemainingSlots(role: String): Int {
        val maxCount = roleCounts[role] ?: 1 // Default to 1 if no count specified
        val assignedCount = assignedRoleCounts[role] ?: 0
        return maxOf(0, maxCount - assignedCount)
    }

    fun getRoleDisplayName(role: String): String {
        // Get the base role name (e.g., "Speaker" from "Speaker 1")
        val baseRole = role.split(" ")[0]
        val maxCount = roleCounts[baseRole] ?: return role
        
        // Get the count of currently assigned instances of this role
        val assignedCount = assignedRoleCounts[baseRole] ?: 0
        return if (maxCount > 1) {
            "$role (${assignedCount}/$maxCount)"
        } else {
            role
        }
    }
}