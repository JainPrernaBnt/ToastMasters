package com.bntsoft.toastmasters.domain.model

data class RoleAssignmentItem(
    val userId: String,
    val memberName: String,
    val preferredRoles: List<String> = emptyList(),
    val recentRoles: List<String> = emptyList(),
    val assignableRoles: List<String> = emptyList(),
    val selectedRoles: MutableList<String> = mutableListOf(),
    var backupMemberId: String = "",
    var backupMemberName: String = ""
) {
    fun addSelectedRole(role: String) {
        if (!selectedRoles.contains(role)) {
            selectedRoles.add(role)
        }
    }
    
    fun removeSelectedRole(role: String) {
        selectedRoles.remove(role)
    }
    
    fun setBackupMember(id: String, name: String) {
        backupMemberId = id
        backupMemberName = name
    }
}