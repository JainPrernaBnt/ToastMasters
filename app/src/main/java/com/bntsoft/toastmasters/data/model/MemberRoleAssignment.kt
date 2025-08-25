package com.bntsoft.toastmasters.data.model

data class MemberRoleAssignment(
    val userId: String = "",
    val memberName: String = "",
    val preferredRoles: List<String> = emptyList(),
    val recentRoles: List<String> = emptyList(),
    val assignedRole: String = ""
)
