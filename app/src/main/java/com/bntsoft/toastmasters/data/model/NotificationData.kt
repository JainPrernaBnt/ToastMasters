package com.bntsoft.toastmasters.data.model

import android.os.Parcelable
import com.bntsoft.toastmasters.utils.NotificationHelper
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.UUID

@Parcelize
data class NotificationData(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: String = NotificationHelper.TYPE_MEMBER_APPROVAL,
    val senderId: String = "",
    val receiverId: String = "",
    val topic: String? = null,
    val data: Map<String, String> = emptyMap(),
    val isRead: Boolean = false,
    val createdAt: Date = Date(),
    val readAt: Date? = null
) : Parcelable {

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "message" to message,
            "type" to type,
            "senderId" to senderId,
            "receiverId" to receiverId,
            "topic" to topic,
            "data" to data,
            "isRead" to isRead,
            "createdAt" to createdAt,
            "readAt" to readAt
        )
    }

    companion object {

        fun fromMap(map: Map<String, Any?>): NotificationData {
            return NotificationData(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                title = map["title"] as? String ?: "",
                message = map["message"] as? String ?: "",
                type = map["type"] as? String ?: NotificationHelper.TYPE_MEMBER_APPROVAL,
                senderId = map["senderId"] as? String ?: "",
                receiverId = map["receiverId"] as? String ?: "",
                topic = map["topic"] as? String,
                data = (map["data"] as? Map<String, Any?>)?.mapValues { it.value?.toString() ?: "" }
                    ?: emptyMap(),
                isRead = map["isRead"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                    ?: Date(),
                readAt = (map["readAt"] as? com.google.firebase.Timestamp)?.toDate()
            )
        }
    }

    fun markAsRead(): NotificationData {
        return this.copy(
            isRead = true,
            readAt = Date()
        )
    }

    fun markAsUnread(): NotificationData {
        return this.copy(
            isRead = false,
            readAt = null
        )
    }
}
