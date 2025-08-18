package com.bntsoft.toastmasters.domain.models

import com.bntsoft.toastmasters.domain.model.Meeting

data class MeetingWithCounts(
    val meeting: Meeting,
    val availableCount: Int,
    val notAvailableCount: Int,
    val notConfirmedCount: Int
)
