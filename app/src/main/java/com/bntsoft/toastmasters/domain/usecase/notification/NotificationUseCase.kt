package com.bntsoft.toastmasters.domain.usecase.notification

import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.bntsoft.toastmasters.utils.NotificationHelper
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import java.lang.Exception

class NotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) {

    suspend fun sendNotificationToUser(
        userId: String,
        title: String,
        message: String,
        type: String = NotificationHelper.TYPE_MEMBER_APPROVAL,
        data: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return Result.runCatching {
            val notification = NotificationData(
                title = title,
                message = message,
                type = type,
                receiverId = userId,
                data = data
            )
            
            val success = notificationRepository.sendNotificationToUser(userId, notification)
            if (!success) {
                throw Exception("Failed to send notification to user")
            }
        }
    }

    suspend fun sendNotificationToRole(
        role: String,
        title: String,
        message: String,
        type: String = NotificationHelper.TYPE_MEMBER_APPROVAL,
        data: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return Result.runCatching {
            val notification = NotificationData(
                title = title,
                message = message,
                type = type,
                data = data
            )
            
            val success = notificationRepository.sendNotificationToRole(role, notification)
            if (!success) {
                throw Exception("Failed to send notification to role")
            }
        }
    }

    suspend fun sendNotificationToTopic(
        topic: String,
        title: String,
        message: String,
        type: String = NotificationHelper.TYPE_MEMBER_APPROVAL,
        data: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return Result.runCatching {
            val notification = NotificationData(
                title = title,
                message = message,
                type = type,
                topic = topic,
                data = data
            )
            
            val success = notificationRepository.sendNotificationToTopic(topic, notification)
            if (!success) {
                throw Exception("Failed to send notification to topic")
            }
        }
    }

    fun getUserNotifications(limit: Int = 20): Flow<List<NotificationData>> {
        return notificationRepository.getUserNotifications(limit)
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return Result.runCatching {
            val success = notificationRepository.markNotificationAsRead(notificationId)
            if (!success) {
                throw Exception("Failed to mark notification as read")
            }
        }
    }

    suspend fun markAllNotificationsAsRead(): Result<Unit> {
        return Result.runCatching {
            val success = notificationRepository.markAllNotificationsAsRead()
            if (!success) {
                throw Exception("Failed to mark all notifications as read")
            }
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return Result.runCatching {
            val success = notificationRepository.deleteNotification(notificationId)
            if (!success) {
                throw Exception("Failed to delete notification")
            }
        }
    }

    suspend fun deleteAllNotifications(): Result<Unit> {
        return Result.runCatching {
            val success = notificationRepository.deleteAllNotifications()
            if (!success) {
                throw Exception("Failed to delete all notifications")
            }
        }
    }
    suspend fun subscribeToTopic(topic: String): Result<Unit> {
        return Result.runCatching {
            val success = notificationRepository.subscribeToTopic(topic)
            if (!success) {
                throw Exception("Failed to subscribe to topic")
            }
        }
    }
    

    suspend fun unsubscribeFromTopic(topic: String): Result<Unit> {
        return Result.runCatching {
            val success = notificationRepository.unsubscribeFromTopic(topic)
            if (!success) {
                throw Exception("Failed to unsubscribe from topic")
            }
        }
    }

    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return Result.runCatching {
            val success = notificationRepository.updateFcmToken(userId, token)
            if (!success) {
                throw Exception("Failed to update FCM token")
            }
        }
    }

    suspend fun removeFcmToken(userId: String): Result<Unit> {
        return Result.runCatching {
            val success = notificationRepository.removeFcmToken(userId)
            if (!success) {
                throw Exception("Failed to remove FCM token")
            }
        }
    }
}
