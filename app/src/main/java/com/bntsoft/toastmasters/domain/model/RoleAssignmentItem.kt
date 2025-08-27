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
    val isEditable: Boolean = false
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
        return this.copy(
            selectedRoles = updatedRoles,
            // If we removed the assigned role, clear it or assign the first available role
            assignedRole = when {
                assignedRole != role -> assignedRole
                updatedRoles.isNotEmpty() -> updatedRoles[0]
                else -> ""
            }
        )
    }
    
    /**
     * Creates a copy of this item with the specified role added
     */
    fun withRoleAdded(role: String): RoleAssignmentItem {
        val updatedRoles = selectedRoles.toMutableList().apply { 
            if (!contains(role)) add(role) 
        }
        return this.copy(
            selectedRoles = updatedRoles,
            // If no role is assigned yet, assign this one
            assignedRole = if (assignedRole.isEmpty()) role else assignedRole
        )
    }
}