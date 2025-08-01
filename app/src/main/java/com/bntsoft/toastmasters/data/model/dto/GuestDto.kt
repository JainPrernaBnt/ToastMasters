package com.bntsoft.toastmasters.data.model.dto

data class GuestDto(
    val guestId: Int = 0,
    val meetingId: Int,
    val name: String,
    val phone: String,
    val email: String,
    val playedRole: Boolean = false,
    val rolePlayed: String? = null,
    val gender: String
)
