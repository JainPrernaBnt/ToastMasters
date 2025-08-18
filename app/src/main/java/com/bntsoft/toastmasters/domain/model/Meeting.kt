package com.bntsoft.toastmasters.domain.model

import java.time.LocalDateTime

data class Meeting(
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val dateTime: LocalDateTime,
    val endDateTime: LocalDateTime? = null,
    val location: String = "",
    val agenda: String = "",
    val isRecurring: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
