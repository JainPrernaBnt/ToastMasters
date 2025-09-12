package com.bntsoft.toastmasters.data.model

data class TableTopicSpeaker(
    val id: String = "",
    val meetingId: String = "",
    val speakerName: String = "",
    val topic: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
