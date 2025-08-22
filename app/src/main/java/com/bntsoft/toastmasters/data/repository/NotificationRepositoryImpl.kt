package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.data.remote.NotificationService
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationService: NotificationService
) : NotificationRepository {

    override suspend fun sendNotificationToUser(
        userId: String,
        notification: NotificationData
    ): Boolean {
        return try {
            notificationService.sendNotificationToUser(userId, notification)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendNotificationToTopic(
        topic: String,
        notification: NotificationData
    ): Boolean {
        return try {
            notificationService.sendNotificationToTopic(topic, notification)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendNotificationToRole(
        role: String,
        notification: NotificationData
    ): Boolean {
        return try {
            notificationService.sendNotificationToRole(role, notification)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getUserNotifications(limit: Int): Flow<List<NotificationData>> = flow {
        try {
            val notifications = notificationService.getUserNotifications(limit)
            emit(notifications)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun markNotificationAsRead(notificationId: String): Boolean {
        return try {
            notificationService.markNotificationAsRead(notificationId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun markAllNotificationsAsRead(): Boolean {
        return try {
            notificationService.markAllNotificationsAsRead()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            notificationService.deleteNotification(notificationId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteAllNotifications(): Boolean {
        return try {
            notificationService.deleteAllNotifications()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            notificationService.subscribeToTopic(topic)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            notificationService.unsubscribeFromTopic(topic)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateFcmToken(userId: String, token: String): Boolean {
        return try {
            notificationService.updateFcmToken(userId, token)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeFcmToken(userId: String): Boolean {
        return try {
            notificationService.removeFcmToken(userId)
            true
        } catch (e: Exception) {
            false
        }
    }
}
