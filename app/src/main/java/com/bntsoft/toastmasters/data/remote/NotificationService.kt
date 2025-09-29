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
            android.util.Log.d("NotificationService", "Sending notification to user: $userId")
            android.util.Log.d("NotificationService", "Original notification ID: ${notification.id}")
            
            // Create a unique notification ID for this user to avoid conflicts
            val uniqueNotificationId = "${notification.id}_$userId"
            val notificationWithReceiver = notification.copy(
                id = uniqueNotificationId,
                receiverId = userId
            )
            
            android.util.Log.d("NotificationService", "Saving notification with ID: $uniqueNotificationId for user: $userId")
            
            // Save notification to Firestore
            notificationsCollection.document(uniqueNotificationId).set(notificationWithReceiver.toMap())
                .await()

            android.util.Log.d("NotificationService", "Successfully saved notification to Firestore for user: $userId")

            // Get user's FCM token and send push notification
            val userDoc = usersCollection.document(userId).get().await()
            val fcmToken = userDoc.getString("fcmToken")

            fcmToken?.let {
                android.util.Log.d("NotificationService", "User $userId has FCM token: ${fcmToken.take(10)}...")
                // Here you would typically use Firebase Cloud Functions or a server to send FCM messages
                // For now, we'll just store the notification in Firestore
            } ?: run {
                android.util.Log.w("NotificationService", "User $userId has no FCM token")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Error sending notification to user: $userId", e)
            throw e
        }
    }

    suspend fun sendNotificationToTopic(topic: String, notification: NotificationData) {
        try {
            // Save the topic notification for reference
            val notificationWithTopic = notification.copy(topic = topic)
            notificationsCollection.document(notification.id).set(notificationWithTopic.toMap())
                .await()

            // Send individual notifications to each user in the topic
            sendFcmMessageToTopic(topic, notification)
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun sendFcmMessageToTopic(topic: String, notification: NotificationData) {
        try {
            // Get all users who should receive this topic notification
            val targetUsers = when (topic) {
                "all_users" -> {
                    // Get all users
                    android.util.Log.d("NotificationService", "Querying all users from collection: $USERS_COLLECTION")
                    val allUsers = usersCollection.get().await().documents
                    android.util.Log.d("NotificationService", "All users query returned ${allUsers.size} users")
                    
                    if (allUsers.isEmpty()) {
                        android.util.Log.w("NotificationService", "No users found in Firestore!")
                    } else {
                        allUsers.forEach { doc ->
                            val role = doc.getString("role")
                            val email = doc.getString("email")
                            android.util.Log.d("NotificationService", "User ${doc.id} (${email}) has role: $role")
                        }
                    }
                    allUsers
                }
                "members" -> {
                    // Get all members - query the single role field
                    val members = usersCollection.whereEqualTo("role", "MEMBER").get().await().documents
                    android.util.Log.d("NotificationService", "Members query returned ${members.size} users")
                    members
                }
                "vp_education" -> {
                    // Get VP Education users - query the single role field
                    val vpUsers = usersCollection.whereEqualTo("role", "VP_EDUCATION").get().await().documents
                    android.util.Log.d("NotificationService", "VP Education query returned ${vpUsers.size} users")
                    vpUsers
                }
                else -> emptyList()
            }

            // Create individual notification documents for each user
            android.util.Log.d("NotificationService", "Found ${targetUsers.size} users for topic: $topic")
            
            targetUsers.forEach { userDoc ->
                val userId = userDoc.id
                val fcmToken = userDoc.getString("fcmToken")
                
                android.util.Log.d("NotificationService", "Processing user: $userId, has FCM token: ${fcmToken != null}")
                
                // Create notification for all users, not just those with FCM tokens
                // FCM token is for push notifications, but we want local notifications too
                val userNotification = notification.copy(
                    id = "${notification.id}_$userId", // Unique ID for each user
                    receiverId = userId,
                    topic = topic // Keep the original topic for reference
                )
                
                // Save individual notification document
                notificationsCollection.document(userNotification.id)
                    .set(userNotification.toMap())
                    .await()
                    
                android.util.Log.d("NotificationService", "Created notification for user: $userId with ID: ${userNotification.id}")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun sendNotificationToRole(role: String, notification: NotificationData) {
        try {
            android.util.Log.d("NotificationService", "Querying users with role: $role")
            
            // Get all users with the specified role - query the single role field
            val usersWithRole = usersCollection
                .whereEqualTo("role", role)
                .get()
                .await()

            android.util.Log.d("NotificationService", "Found ${usersWithRole.documents.size} users with role: $role")

            if (usersWithRole.documents.isEmpty()) {
                android.util.Log.w("NotificationService", "No users found with role: $role")
                
                // Let's also check what roles exist in the system
                val allUsers = usersCollection.get().await().documents
                android.util.Log.d("NotificationService", "Total users in system: ${allUsers.size}")
                allUsers.forEach { doc ->
                    val userRole = doc.getString("role")
                    val userEmail = doc.getString("email")
                    android.util.Log.d("NotificationService", "User ${doc.id} (${userEmail}) has role: $userRole")
                }
            }

            // Send notification to each user with the role
            usersWithRole.documents.forEach { userDoc ->
                val userId = userDoc.id
                val userRole = userDoc.getString("role")
                val userEmail = userDoc.getString("email")
                android.util.Log.d("NotificationService", "Sending notification to user: $userId (${userEmail}) with role: $userRole")
                sendNotificationToUser(userId, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Error in sendNotificationToRole", e)
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
                batch.update(
                    document.reference, mapOf(
                        "isRead" to true,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                )
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
            fcmTokensCollection.document(userId).set(
                mapOf(
                    "token" to token,
                    "userId" to userId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
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
