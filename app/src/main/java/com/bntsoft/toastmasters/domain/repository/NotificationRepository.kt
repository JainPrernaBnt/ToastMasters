package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.data.model.NotificationData
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    suspend fun sendNotificationToUser(userId: String, notification: NotificationData): Boolean

    suspend fun sendNotificationToTopic(topic: String, notification: NotificationData): Boolean

    suspend fun sendNotificationToRole(role: String, notification: NotificationData): Boolean

    fun getUserNotifications(limit: Int = 20): Flow<List<NotificationData>>

    suspend fun markNotificationAsRead(notificationId: String): Boolean

    suspend fun markAllNotificationsAsRead(): Boolean

    suspend fun deleteNotification(notificationId: String): Boolean

    suspend fun deleteAllNotifications(): Boolean

    suspend fun subscribeToTopic(topic: String): Boolean

    suspend fun unsubscribeFromTopic(topic: String): Boolean

    suspend fun updateFcmToken(userId: String, token: String): Boolean

    suspend fun removeFcmToken(userId: String): Boolean
}
