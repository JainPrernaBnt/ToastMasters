package com.bntsoft.toastmasters.domain.model

data class MeetingWithCounts(
    val meeting: Meeting,
    val availableCount: Int,
    val notAvailableCount: Int,
    val notConfirmedCount: Int,
    val notResponded: Int
)
