package com.bntsoft.toastmasters.service

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service to handle Firebase Cloud Messaging (FCM) messages.
 */
@AndroidEntryPoint
class ToastMastersMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    companion object {
        private const val TAG = "ToastMastersFCM"
        
        // Actions
        const val ACTION_TOKEN_REFRESH = "com.bntsoft.toastmasters.ACTION_TOKEN_REFRESH"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ToastMastersMessagingService created")
    }
    
    /**
     * Called when a new FCM token is generated.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        
        // Broadcast the token refresh to update it in the repository
        val intent = Intent(ACTION_TOKEN_REFRESH).apply {
            putExtra("token", token)
        }
        sendBroadcast(intent)
        
        // Update the token in the repository
        updateTokenInRepository(token)
    }
    
    /**
     * Called when a new FCM message is received.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            // Handle the message in the background
            handleMessageInBackground(remoteMessage)
        }
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            
            // If the app is in the foreground, we can show the notification directly
            if (!isAppInForeground()) {
                notificationHelper.handleRemoteMessage(remoteMessage)
            }
        }
    }
    
    /**
     * Handle the message in a background coroutine
     */
    private fun handleMessageInBackground(remoteMessage: RemoteMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Process the message data
                processMessageData(remoteMessage.data)
                
                // Show the notification
                notificationHelper.handleRemoteMessage(remoteMessage)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing FCM message", e)
            }
        }
    }
    
    /**
     * Process the data payload of the FCM message
     */
    private fun processMessageData(data: Map<String, String>) {
        // Extract data from the message
        val type = data["type"]
        val title = data["title"] ?: getString(R.string.app_name)
        val message = data["message"] ?: ""
        
        Log.d(TAG, "Processing message - Type: $type, Title: $title, Message: $message")
        
        // Handle different message types
        when (type) {
            NotificationHelper.TYPE_MEMBER_APPROVAL -> {
                // Handle member approval notification
                val userId = data["user_id"]
                userId?.let {
                    // Update the local database or refresh the UI
                    Log.d(TAG, "Member approved: $userId")
                }
            }
            NotificationHelper.TYPE_MENTOR_ASSIGNMENT -> {
                // Handle mentor assignment notification
                val userId = data["user_id"]
                val mentorId = data["mentor_id"]
                Log.d(TAG, "Mentor assigned - User: $userId, Mentor: $mentorId")
            }
            NotificationHelper.TYPE_MEETING_CREATED -> {
                // Handle meeting created notification
                val meetingId = data["meeting_id"]
                meetingId?.let {
                    // Refresh the meetings list
                    Log.d(TAG, "New meeting created: $meetingId")
                }
            }
            else -> {
                // Handle other notification types or unknown types
                Log.d(TAG, "Received unknown message type: $type")
            }
        }
    }
    
    /**
     * Check if the app is in the foreground
     */
    private fun isAppInForeground(): Boolean {
        val appProcessInfo = android.app.ActivityManager.RunningAppProcessInfo()
        android.app.ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
               appProcessInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }
    
    /**
     * Update the FCM token in the repository
     */
    private fun updateTokenInRepository(token: String) {
        // This will be handled by the FcmTokenManager
        // We just need to ensure the token is passed to the repository
        Log.d(TAG, "Updating FCM token in repository")
    }
    
    /**
     * Check if notifications are enabled for the app
     */
    private fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android O and above, check if the notification channel is enabled
            val manager = getSystemService(NotificationManager::class.java)
            val channel = manager.getNotificationChannel(NotificationHelper.CHANNEL_ID_DEFAULT)
            channel.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            // For older versions, check if notifications are enabled in system settings
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }
}
