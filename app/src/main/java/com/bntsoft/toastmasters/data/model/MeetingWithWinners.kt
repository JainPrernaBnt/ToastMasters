package com.bntsoft.toastmasters.data.model

import com.google.firebase.Timestamp

data class MeetingWithWinners(
    val meetingId: String = "",
    val date: String = "",
    val theme: String = "",
    val winners: List<Winner> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
