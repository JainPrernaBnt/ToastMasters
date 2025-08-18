package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.model.NotificationData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val NOTIFICATIONS_COLLECTION = "notifications"
        private const val USERS_COLLECTION = "users"
        private const val FCM_TOKENS_COLLECTION = "fcm_tokens"
    }

    private val notificationsCollection = firestore.collection(NOTIFICATIONS_COLLECTION)
    private val usersCollection = firestore.collection(USERS_COLLECTION)
    private val fcmTokensCollection = firestore.collection(FCM_TOKENS_COLLECTION)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseMessaging = FirebaseMessaging.getInstance()

    suspend fun sendNotificationToUser(userId: String, notification: NotificationData) {
        try {
            // Save notification to Firestore
            val notificationWithReceiver = notification.copy(receiverId = userId)
            notificationsCollection.document(notification.id).set(notificationWithReceiver.toMap()).await()

            // Get user's FCM token and send push notification
            val userDoc = usersCollection.document(userId).get().await()
            val fcmToken = userDoc.getString("fcmToken")
            
            fcmToken?.let {
                // Here you would typically use Firebase Cloud Functions or a server to send FCM messages
                // For now, we'll just store the notification in Firestore
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun sendNotificationToTopic(topic: String, notification: NotificationData) {
        try {
            // Save notification to Firestore with topic
            val notificationWithTopic = notification.copy(topic = topic)
            notificationsCollection.document(notification.id).set(notificationWithTopic.toMap()).await()

            // Subscribe to topic and send notification
            firebaseMessaging.subscribeToTopic(topic).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun sendNotificationToRole(role: String, notification: NotificationData) {
        try {
            // Get all users with the specified role
            val usersWithRole = usersCollection
                .whereEqualTo("role", role)
                .get()
                .await()

            // Send notification to each user with the role
            usersWithRole.documents.forEach { userDoc ->
                val userId = userDoc.id
                sendNotificationToUser(userId, notification)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getUserNotifications(limit: Int): List<NotificationData> {
        return try {
            val currentUser = firebaseAuth.currentUser ?: return emptyList()
            
            val querySnapshot = notificationsCollection
                .whereEqualTo("receiverId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                NotificationData.fromMap(document.data ?: emptyMap())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            val updates = mapOf(
                "isRead" to true,
                "readAt" to FieldValue.serverTimestamp()
            )
            notificationsCollection.document(notificationId).update(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun markAllNotificationsAsRead() {
        try {
            val currentUser = firebaseAuth.currentUser ?: return
            
            val userNotifications = notificationsCollection
                .whereEqualTo("receiverId", currentUser.uid)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            userNotifications.documents.forEach { document ->
                batch.update(document.reference, mapOf(
                    "isRead" to true,
                    "readAt" to FieldValue.serverTimestamp()
                ))
            }
            batch.commit().await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteNotification(notificationId: String) {
        try {
            notificationsCollection.document(notificationId).delete().await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteAllNotifications() {
        try {
            val currentUser = firebaseAuth.currentUser ?: return
            
            val userNotifications = notificationsCollection
                .whereEqualTo("receiverId", currentUser.uid)
                .get()
                .await()

            val batch = firestore.batch()
            userNotifications.documents.forEach { document ->
                batch.delete(document.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun subscribeToTopic(topic: String) {
        try {
            firebaseMessaging.subscribeToTopic(topic).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun unsubscribeFromTopic(topic: String) {
        try {
            firebaseMessaging.unsubscribeFromTopic(topic).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateFcmToken(userId: String, token: String) {
        try {
            val updates = mapOf(
                "fcmToken" to token,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            usersCollection.document(userId).update(updates).await()
            
            // Also store in separate FCM tokens collection for easier management
            fcmTokensCollection.document(userId).set(mapOf(
                "token" to token,
                "userId" to userId,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun removeFcmToken(userId: String) {
        try {
            val updates = mapOf(
                "fcmToken" to FieldValue.delete(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            usersCollection.document(userId).update(updates).await()
            
            // Also remove from FCM tokens collection
            fcmTokensCollection.document(userId).delete().await()
        } catch (e: Exception) {
            throw e
        }
    }
}
