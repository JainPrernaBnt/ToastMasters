package com.bntsoft.toastmasters.domain.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class AgendaItem(
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    val meetingId: String = "",
    val orderIndex: Int = 0,
    val time: String = "",
    val greenTime: Int = 0,
    val yellowTime: Int = 0,
    val redTime: Int = 0,
    val activity: String = "",
    val presenterId: String = "",
    val presenterName: String = "",
    val isSessionHeader: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) : Parcelable {

    fun withOrderIndex(newIndex: Int): AgendaItem = copy(orderIndex = newIndex)

    companion object {
        fun default(meetingId: String): AgendaItem = AgendaItem(
            meetingId = meetingId,
            orderIndex = 0,
            time = "",
            greenTime = 0,
            yellowTime = 0,
            redTime = 0,
            activity = "",
            presenterId = "",
            presenterName = "",
            isSessionHeader = false
        )
    }
}
