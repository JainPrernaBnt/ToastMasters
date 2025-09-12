package com.bntsoft.toastmasters.data.model

data class SpeakerDetails(
    val id: String = "",
    val meetingId: String = "",
    val userId: String = "",
    val name: String = "",
    val pathwaysTrack: String = "",
    val level: Int = 0,
    val projectNumber: String = "",
    val projectTitle: String = "",
    val speechTime: String = "",
    val speechTitle: String = "",
    val speechObjectives: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
