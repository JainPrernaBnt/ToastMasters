package com.bntsoft.toastmasters.presentation.ui.vp.roles.model

import com.bntsoft.toastmasters.domain.model.Meeting
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

data class MeetingListItem(
    val id: String,
    val theme: String,
    val venue: String,
    val dateTime: String,
    val dayOfWeek: String,
    val formattedDate: String,
    val formattedTime: String,
    val meeting: Meeting
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        private val dayFormatter = DateTimeFormatter.ofPattern("EEEE")

        fun fromMeeting(meeting: Meeting): MeetingListItem {
            val dateTime = meeting.dateTime
            val endTime = meeting.endDateTime
            
            return MeetingListItem(
                id = meeting.id,
                theme = meeting.theme ?: "No Theme",
                venue = meeting.location ?: "Location TBD",
                dateTime = buildString {
                    append(dateTime.format(dayFormatter))
                    append(", ")
                    append(dateTime.format(dateFormatter))
                    append(" • ")
                    append(dateTime.format(timeFormatter))
                    if (endTime != null) {
                        append(" - ")
                        append(endTime.format(timeFormatter))
                    }
                },
                dayOfWeek = dateTime.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                formattedDate = dateTime.format(dateFormatter),
                formattedTime = buildString {
                    append(dateTime.format(timeFormatter))
                    if (endTime != null) {
                        append(" - ")
                        append(endTime.format(timeFormatter))
                    }
                },
                meeting = meeting
            )
        }
    }
}
