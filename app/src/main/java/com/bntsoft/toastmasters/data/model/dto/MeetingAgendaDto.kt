package com.bntsoft.toastmasters.data.model.dto

import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class MeetingAgendaDto(
    @DocumentId
    val id: String = "",
    val meetingId: String = "",
    val meetingTitle: String = "",
    val meetingDate: Timestamp? = null,
    val startTime: String = "",
    val endTime: String = "",
    val meetingTheme: String = "",
    val venue: String = "",
    val createdBy: String = "",
    val officers: Map<String, String> = emptyMap(),
    val agendaStatus: String = AgendaStatus.DRAFT.name,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
