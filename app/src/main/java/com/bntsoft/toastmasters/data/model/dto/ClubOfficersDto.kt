package com.bntsoft.toastmasters.data.model.dto

data class ClubOfficersDto(
    val officersId: Int = 0,
    val meetingId: Int?,
    val userId: Int,
    val role: String,
    val termStart: String,
    val termEnd: String
)
