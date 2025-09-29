package com.bntsoft.toastmasters.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.utils.NotificationHelper
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListenerService : Service() {

    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var prefsManager: PreferenceManager
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    
    private val firestore = FirebaseFirestore.getInstance()
    private var notificationListener: ListenerRegistration? = null
    
    companion object {
        private const val TAG = "NotificationListener"
        private const val NOTIFICATIONS_COLLECTION = "notifications"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationListenerService created")
        startListeningForNotifications()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NotificationListenerService destroyed")
        notificationListener?.remove()
    }

    private fun startListeningForNotifications() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user, cannot listen for notifications")
            return
        }

        Log.d(TAG, "Starting to listen for notifications for user: ${currentUser.uid}")
        
        // Since we now create individual notifications for each user,
        // we only need to listen for notifications with the current user's receiverId
        
        // First, let's try a simpler query without orderBy to avoid index issues
        notificationListener = firestore.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("receiverId", currentUser.uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for notifications", error)
                    return@addSnapshotListener
                }

                Log.d(TAG, "Notification listener triggered. Documents count: ${snapshots?.documents?.size ?: 0}")
                
                snapshots?.documentChanges?.forEach { change ->
                    Log.d(TAG, "Document change type: ${change.type}, document ID: ${change.document.id}")
                    
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val notification = NotificationData.fromMap(change.document.data)
                        notification?.let {
                            Log.d(TAG, "New notification received: ${it.title} for user: ${it.receiverId}")
                            showLocalNotification(it)
                        } ?: run {
                            Log.w(TAG, "Failed to parse notification from document: ${change.document.id}")
                        }
                    }
                }
                
                // Also log existing notifications for debugging
                snapshots?.documents?.forEach { doc ->
                    val notification = NotificationData.fromMap(doc.data ?: emptyMap())
                    Log.d(TAG, "Existing notification: ${notification.title}, receiverId: ${notification.receiverId}, isRead: ${notification.isRead}")
                }
            }
    }

    private fun showLocalNotification(notification: NotificationData) {
        try {
            when (notification.type) {
                NotificationHelper.TYPE_MEETING_CREATED -> {
                    notificationHelper.showMeetingNotification(
                        meetingId = notification.data["meetingId"] ?: "",
                        title = notification.title,
                        message = notification.message,
                        date = notification.data["meetingDate"],
                        location = notification.data["meetingLocation"],
                        isReminder = false,
                        data = notification.data
                    )
                }
                
                NotificationHelper.TYPE_MEETING_UPDATED -> {
                    notificationHelper.showMeetingNotification(
                        meetingId = notification.data["meetingId"] ?: "",
                        title = notification.title,
                        message = notification.message,
                        date = notification.data["meetingDate"],
                        location = notification.data["meetingLocation"],
                        isReminder = false,
                        data = notification.data
                    )
                }
                
                else -> {
                    // Show generic notification
                    notificationHelper.showNotification(
                        title = notification.title,
                        message = notification.message,
                        channelId = NotificationHelper.CHANNEL_ID_DEFAULT,
                        data = notification.data
                    )
                }
            }
            
            Log.d(TAG, "Local notification shown for: ${notification.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing local notification", e)
        }
    }
}
