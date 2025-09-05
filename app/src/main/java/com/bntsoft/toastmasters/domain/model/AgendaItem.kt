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
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) : Parcelable {
    val totalTime: Int get() = greenTime + yellowTime + redTime

    fun withOrderIndex(newIndex: Int): AgendaItem = copy(orderIndex = newIndex)

    companion object {
        fun default(meetingId: String): AgendaItem = AgendaItem(
            meetingId = meetingId,
            orderIndex = 0,
            time = "",
            greenTime = 5,
            yellowTime = 1,
            redTime = 1,
            activity = "",
            presenterId = "",
            presenterName = ""
        )
    }
}
