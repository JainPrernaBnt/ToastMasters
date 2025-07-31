package com.bntsoft.toastmasters.data.model.dto

data class MeetingRolesDto(
    val roleId: Int = 0,
    val meetingId: Int,
    val userId: Int,
    val assignedRole: String,
    val isBackupMember: Boolean,
    val isCancelled: Boolean = false,
    val assignedAt: Long,
    val cancelledAt: Long? = null
)
