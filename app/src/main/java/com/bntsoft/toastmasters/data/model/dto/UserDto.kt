package com.bntsoft.toastmasters.data.model.dto

data class UserDto(
    val id: Int,
    val role: String,
    val name: String,
    val email: String,
    val number: Int,
    val address: String,
    val level: Int,
    val gender: String,
    val isApproved: Boolean = false,
    val hasVpPermission: Boolean = false,
    val clubId: Int,
    val toastmastersId: Int,
    val mentor: String,
    val mentorAssignedBy: String?,
    val mentorAssignedDate: Long?,
    val createdAt: Long = System.currentTimeMillis()
)
