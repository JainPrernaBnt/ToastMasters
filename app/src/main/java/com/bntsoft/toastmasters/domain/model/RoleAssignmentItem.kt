package com.bntsoft.toastmasters.domain.model

data class RoleAssignmentItem(
    val userId: String,
    val memberName: String,
    val preferredRoles: List<String> = emptyList(),
    val recentRoles: List<String> = emptyList(),
    val assignableRoles: List<String> = emptyList(),
    val selectedRoles: MutableList<String> = mutableListOf(),
    val assignedRole: String = "",
    val backupMemberId: String = "",
    val backupMemberName: String = "",
    val isEditable: Boolean = false,
    val roleCounts: Map<String, Int> = emptyMap(),
    val assignedRoleCounts: Map<String, Int> = emptyMap()
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
    
    /**
     * Creates a copy of this item with the specified role removed
     */
    fun withRoleRemoved(role: String): RoleAssignmentItem {
        val updatedRoles = selectedRoles.toMutableList().apply { remove(role) }
        
        // Update assigned role counts
        val updatedAssignedCounts = assignedRoleCounts.toMutableMap()
        val currentCount = updatedAssignedCounts[role] ?: 0
        if (currentCount > 0) {
            updatedAssignedCounts[role] = currentCount - 1
            if (updatedAssignedCounts[role] == 0) {
                updatedAssignedCounts.remove(role)
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
    
    /**
     * Creates a copy of this item with the specified role added
     */
    fun withRoleAdded(role: String): RoleAssignmentItem {
        val updatedRoles = selectedRoles.toMutableList().apply { 
            if (!contains(role)) add(role) 
        }
        
        // Update assigned role counts
        val updatedAssignedCounts = assignedRoleCounts.toMutableMap()
        val currentCount = updatedAssignedCounts[role] ?: 0
        updatedAssignedCounts[role] = currentCount + 1
        
        return this.copy(
            selectedRoles = updatedRoles,
            assignedRole = if (assignedRole.isEmpty()) role else assignedRole,
            assignedRoleCounts = updatedAssignedCounts
        )
    }
    
    /**
     * Check if a role can be assigned based on available slots
     */
    fun canAssignRole(role: String): Boolean {
        val maxCount = roleCounts[role] ?: 1 // Default to 1 if no count specified
        val assignedCount = assignedRoleCounts[role] ?: 0
        return assignedCount < maxCount
    }
    
    /**
     * Get the remaining slots for a role
     */
    fun getRemainingSlots(role: String): Int {
        val maxCount = roleCounts[role] ?: 1 // Default to 1 if no count specified
        val assignedCount = assignedRoleCounts[role] ?: 0
        return maxOf(0, maxCount - assignedCount)
    }
    
    /**
     * Get display name for a role with count info (e.g., "Speaker (1/2)")
     */
    fun getRoleDisplayName(role: String): String {
        val maxCount = roleCounts[role] ?: return role
        val assignedCount = assignedRoleCounts[role] ?: 0
        return if (maxCount > 1) "$role (${assignedCount + 1}/$maxCount)" else role
    }
}