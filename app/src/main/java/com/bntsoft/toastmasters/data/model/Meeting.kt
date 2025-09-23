package com.bntsoft.toastmasters.data.model

import com.bntsoft.toastmasters.data.local.entity.MeetingRoles
import com.google.firebase.firestore.Exclude
import java.util.*

data class Meeting(
    val id: String = "",
    val theme: String = "",
    val date: Date = Date(),
    val startTime: String = "",
    val endTime: String = "",
    val venue: String = "",
    val roleCounts: Map<String, Int> = emptyMap(),
    val assignedRoles: Map<String, String> = emptyMap(),
    val assignedCounts: Map<String, Int> = emptyMap(),
    @get:Exclude
    val isLoading: Boolean = false,
    @get:Exclude
    val error: String? = null
) {

    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "theme" to theme,
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime,
            "venue" to venue,
        )
    }
}
