package com.bntsoft.toastmasters.data.model.dto

data class WinnersDto(
    val id: Int = 0,
    val meetingId: Int,
    val category: String,
    val winnerUserId: Int? = null,
    val isGuest: Boolean,
    val guestName: String? = null
)

