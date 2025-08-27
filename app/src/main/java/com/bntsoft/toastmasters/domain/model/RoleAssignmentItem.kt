package com.bntsoft.toastmasters.domain.model

data class RoleAssignmentItem(
    val userId: String,
    val memberName: String,
    val preferredRoles: List<String> = emptyList(),
    val recentRoles: List<String> = emptyList(),
    val assignableRoles: List<String> = emptyList(),
    val selectedRoles: MutableList<String> = mutableListOf(),
    val assignedRole: String = "",
    var backupMemberId: String = "",
    var backupMemberName: String = "",
    var isEditable: Boolean = assignedRole.isBlank()
) {
    fun copyWithEditMode(editable: Boolean): RoleAssignmentItem {
        return this.copy(
            isEditable = editable,
            selectedRoles = selectedRoles.toMutableList()
        )
    }
}