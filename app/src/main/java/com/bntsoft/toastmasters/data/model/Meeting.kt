package com.bntsoft.toastmasters.data.model

import com.google.firebase.firestore.Exclude
import java.util.*

data class Meeting(
    val id: String = "",
    val theme: String = "",
    val date: Date = Date(),
    val startTime: String = "",
    val endTime: String = "",
    val venue: String = "",
    val officers: Map<String, String> = emptyMap(),
    @get:Exclude
    val isLoading: Boolean = false,
    @get:Exclude
    val error: String? = null
) {
    companion object {
        val DEFAULT_OFFICER_ROLES = listOf(
            "President",
            "VP Education",
            "VP Membership",
            "VP Public Relations",
            "SAA",
            "Secretary",
            "Treasurer",
            "Immediate Past President"
        )
    }

    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "theme" to theme,
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime,
            "venue" to venue,
            "officers" to officers
        )
    }
}
