package com.bntsoft.toastmasters.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Add notification-related methods here
    // Example:
    // fun sendNotification(notification: Notification) { ... }
    // fun getNotifications(userId: String): Flow<List<Notification>> { ... }
}
