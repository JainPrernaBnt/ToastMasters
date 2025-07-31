package com.bntsoft.toastmasters.data.model.dto

data class MeetingDto(
    val meetingID: Int,
    val title: String,
    val date: String,
    val time: String,
    val venue: String,
    val theme: String,
    val preferredRoles: List<String>,
    val createdAt: Long
)
