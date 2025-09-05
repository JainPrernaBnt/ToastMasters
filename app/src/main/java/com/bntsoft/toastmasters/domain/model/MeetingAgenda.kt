package com.bntsoft.toastmasters.domain.model

import android.os.Parcelable
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.time.ZoneOffset

@Parcelize
data class MeetingAgenda(
    @DocumentId
    val id: String = "",
    val meeting: Meeting,
    val meetingDate: Timestamp? = null,
    val startTime: String = "",
    val endTime: String = "",
    val officers: Map<String, String> = emptyMap(), // e.g., {"President":"TM John", "VP Education":"TM Jane"}
    val agendaStatus: AgendaStatus = AgendaStatus.DRAFT,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) : Parcelable {

    fun withStatus(newStatus: AgendaStatus): MeetingAgenda = copy(agendaStatus = newStatus)

    fun withMeetingDetails(
        theme: String = this.meeting.theme,
        date: Timestamp? = this.meetingDate,
        start: String = this.startTime,
        end: String = this.endTime,
        officers: Map<String, String> = this.officers
    ): MeetingAgenda = copy(
        meeting = meeting.copy(theme = theme),
        meetingDate = date,
        startTime = start,
        endTime = end,
        officers = officers,
        updatedAt = Timestamp.now()
    )

    fun withMeeting(meeting: Meeting): MeetingAgenda = copy(
        meeting = meeting,
        updatedAt = Timestamp.now()
    )

    companion object {
        fun default(meeting: Meeting, createdBy: String): MeetingAgenda = MeetingAgenda(
            id = meeting.id,
            meeting = meeting,
            meetingDate = Timestamp(meeting.dateTime.toEpochSecond(ZoneOffset.UTC), 0),
            startTime = meeting.dateTime.hour.toString().padStart(2, '0') + 
                       ":" + 
                       meeting.dateTime.minute.toString().padStart(2, '0'),
            endTime = meeting.endDateTime?.let { end ->
                end.hour.toString().padStart(2, '0') + ":" + 
                end.minute.toString().padStart(2, '0')
            } ?: "",
            agendaStatus = AgendaStatus.DRAFT,
            officers = emptyMap()
        )
    }
}
