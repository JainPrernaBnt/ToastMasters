package com.bntsoft.toastmasters.data.model.dto

data class MeetingRolesBackoutDto(
    val backoutRoleId: Int = 0,
    val meetingId: Int,
    val originalUserId: Int,
    val roleName: String,
    val backupUserId: Int?,
    val reassignedAt: Long?,
    val reassignedBy: String?
)
