package com.bntsoft.toastmasters.data.model

data class MeetingWithWinners(
    val meetingId: String = "",
    val date: String = "",
    val theme: String = "",
    val winners: List<Winner> = emptyList()
)
