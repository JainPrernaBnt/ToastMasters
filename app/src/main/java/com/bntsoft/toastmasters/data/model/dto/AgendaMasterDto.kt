package com.bntsoft.toastmasters.data.model.dto

data class AgendaMasterDto(
    val agendaId: Int = 0,
    val meetingId: Int,
    val createdBy: Int?,
    val createdAt: Long,
    val status: String,
    val meetingStartTime: String,
    val meetingEndTime: String
)
