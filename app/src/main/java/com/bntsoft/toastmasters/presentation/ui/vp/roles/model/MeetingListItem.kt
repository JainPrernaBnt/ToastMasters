package com.bntsoft.toastmasters.presentation.ui.vp.roles.model

import com.bntsoft.toastmasters.domain.model.Meeting
import java.time.format.DateTimeFormatter

data class MeetingListItem(
    val id: String,
    val theme: String,
    val venue: String,
    val dateTime: String,
    val meeting: Meeting
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

        fun fromMeeting(meeting: Meeting): MeetingListItem {
            val dateStr = meeting.dateTime.format(dateFormatter)
            val startTime = meeting.dateTime.format(timeFormatter)
            val endTime = meeting.endDateTime?.format(timeFormatter) ?: ""
            
            return MeetingListItem(
                id = meeting.id,
                theme = meeting.theme,
                venue = meeting.location,
                dateTime = "$dateStr • $startTime - $endTime",
                meeting = meeting
            )
        }
    }
}
