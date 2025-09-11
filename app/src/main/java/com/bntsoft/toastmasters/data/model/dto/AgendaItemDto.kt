package com.bntsoft.toastmasters.data.model.dto

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class AgendaItemDto(
    @DocumentId
    val id: String = "",
    val meetingId: String = "",
    var orderIndex: Int = 0,
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
): Parcelable
