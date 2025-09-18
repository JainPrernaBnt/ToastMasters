package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseMessaging: FirebaseMessaging
) : NotificationRepository {

    companion object {
        private const val COLLECTION_NOTIFICATIONS = "notifications"
        private const val COLLECTION_USERS = "users"
        private const val FIELD_IS_READ = "isRead"
        private const val FIELD_READ_AT = "readAt"
        private const val FIELD_RECEIVER_ID = "receiverId"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_FCM_TOKEN = "fcmToken"
    }

    override suspend fun sendNotificationToUser(
        userId: String,
        notification: NotificationData
    ): Boolean {
        return try {
            // Add the notification to the user's notifications collection
            val notificationRef = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_NOTIFICATIONS)
                .document(notification.id)

            notificationRef.set(notification.toMap()).await()
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error sending notification to user $userId", e)
            false
        }
    }

    override suspend fun sendNotificationToTopic(
        topic: String,
        notification: NotificationData
    ): Boolean {
        return try {
            // In a real app, you would use Firebase Cloud Functions to send to topics
            // For now, we'll just log it
            Log.d("NotifRepo", "Sending notification to topic $topic: ${notification.title} - ${notification.message}")
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error sending notification to topic $topic", e)
            false
        }
    }

    override suspend fun sendNotificationToRole(
        role: String,
        notification: NotificationData
    ): Boolean {
        return try {
            // In a real app, you would query users with the given role and send to each
            // For now, we'll just log it
            Log.d("NotifRepo", "Sending notification to role $role: ${notification.title} - ${notification.message}")
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error sending notification to role $role", e)
            false
        }
    }

    override fun getUserNotifications(limit: Int): Flow<List<NotificationData>> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .orderBy(FIELD_CREATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotifRepo", "Error getting notifications", error)
                    trySend(emptyList()) // safe send
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { document ->
                    try {
                        NotificationData.fromMap(document.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        Log.e("NotifRepo", "Error mapping notification document", e)
                        null
                    }
                } ?: emptyList()

                trySend(notifications) // safe send
            }

        awaitClose {
            registration.remove()
        }
    }


    override suspend fun markNotificationAsRead(notificationId: String): Boolean {
        return try {
            val userId = firebaseAuth.currentUser?.uid ?: return false

            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update(
                    FIELD_IS_READ, true,
                    FIELD_READ_AT, Timestamp.now()
                ).await()

            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error marking notification as read", e)
            false
        }
    }

    override suspend fun markAllNotificationsAsRead(): Boolean {
        return try {
            val userId = firebaseAuth.currentUser?.uid ?: return false

            // Get all unread notifications
            val querySnapshot = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_IS_READ, false)
                .get()
                .await()

            // Update all to read
            val batch = firestore.batch()
            for (document in querySnapshot) {
                val notificationRef = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_NOTIFICATIONS)
                    .document(document.id)

                batch.update(
                    notificationRef,
                    FIELD_IS_READ, true,
                    FIELD_READ_AT, Timestamp.now()
                )
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error marking all notifications as read", e)
            false
        }
    }

    override suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            val userId = firebaseAuth.currentUser?.uid ?: return false

            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .delete()
                .await()

            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error deleting notification $notificationId", e)
            false
        }
    }

    override suspend fun deleteAllNotifications(): Boolean {
        return try {
            val userId = firebaseAuth.currentUser?.uid ?: return false

            // Get all notifications
            val querySnapshot = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_NOTIFICATIONS)
                .get()
                .await()

            // Delete all notifications
            val batch = firestore.batch()
            for (document in querySnapshot) {
                val notificationRef = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_NOTIFICATIONS)
                    .document(document.id)

                batch.delete(notificationRef)
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error deleting all notifications", e)
            false
        }
    }

    override suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            firebaseMessaging.subscribeToTopic(topic).await()
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error subscribing to topic $topic", e)
            false
        }
    }

    override suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            firebaseMessaging.unsubscribeFromTopic(topic).await()
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error unsubscribing from topic $topic", e)
            false
        }
    }

    override suspend fun updateFcmToken(userId: String, token: String): Boolean {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update(FIELD_FCM_TOKEN, token)
                .await()
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error updating FCM token for user $userId", e)
            false
        }
    }

    override suspend fun removeFcmToken(userId: String): Boolean {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update(FIELD_FCM_TOKEN, null)
                .await()
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error removing FCM token for user $userId", e)
            false
        }
    }
}
