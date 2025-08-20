package com.bntsoft.toastmasters.domain.model

import java.time.LocalDateTime

data class Meeting(
    val id: String = "",
    val dateTime: LocalDateTime,
    val endDateTime: LocalDateTime? = null,
    val location: String = "",
    val theme: String = "",
    val availableRoles: List<String> = emptyList(),
    val isRecurring: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // For backward compatibility
    val preferredRoles: List<String> get() = availableRoles
}
