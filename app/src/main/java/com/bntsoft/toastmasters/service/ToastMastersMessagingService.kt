package com.bntsoft.toastmasters.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bntsoft.toastmasters.MainActivity
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.bntsoft.toastmasters.utils.FcmTokenManager
import com.bntsoft.toastmasters.utils.NotificationHelper
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// Timber import removed, using Android Log instead
import javax.inject.Inject


@AndroidEntryPoint
class ToastMastersMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var prefsManager: PreferenceManager

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val TAG = "ToastMastersMsgService"
        const val CHANNEL_ID_MEETINGS = "meetings_channel"

        // Notification types
        private const val TYPE_MEETING_CREATED = "MEETING_CREATED"
        private const val TYPE_MEETING_UPDATED = "MEETING_UPDATED"
        private const val TYPE_MEETING_REMINDER = "MEETING_REMINDER"
        private const val TYPE_NEW_MEMBER_SIGNUP = "new_member_signup"
        private const val TYPE_MEMBER_BACKOUT = "member_backout"
        private const val TYPE_REQUEST_APPROVED = "request_approved"
        private const val TYPE_REQUEST_REJECTED = "request_rejected"

        // Intent extras
        private const val EXTRA_MEETING_ID = "meetingId"
        private const val EXTRA_FRAGMENT = "fragment"
        private const val FRAGMENT_MEETING_DETAILS = "meeting_details"

        const val ACTION_TOKEN_REFRESH = "com.bntsoft.toastmasters.ACTION_TOKEN_REFRESH"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ToastMastersMsgSvc", "ToastMastersMessagingService created")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Broadcast the token refresh to update it in the repository
        val intent = Intent(ACTION_TOKEN_REFRESH).apply {
            putExtra("token", token)
        }
        sendBroadcast(intent)

        // Update the token in the repository using a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fcmTokenManager.updateToken(token)
            } catch (e: Exception) {
                Log.e("ToastMastersMsgSvc", "Failed to update FCM token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("ToastMastersMsgSvc", "From: ${remoteMessage.from}")

        try {
            // Check if message contains a data payload
            if (remoteMessage.data.isNotEmpty()) {
                Log.d("ToastMastersMsgSvc", "Message data: ${remoteMessage.data}")

                // Handle different types of notifications
                when (remoteMessage.data["type"]) {
                    TYPE_MEETING_CREATED -> handleMeetingCreated(remoteMessage)
                    TYPE_MEETING_UPDATED -> handleMeetingUpdated(remoteMessage)
                    TYPE_MEETING_REMINDER -> handleMeetingReminder(remoteMessage)
                    TYPE_NEW_MEMBER_SIGNUP -> handleNewMemberSignup(remoteMessage)
                    TYPE_MEMBER_BACKOUT -> handleMemberBackout(remoteMessage)
                    TYPE_REQUEST_APPROVED -> handleRequestApproved(remoteMessage)
                    TYPE_REQUEST_REJECTED -> handleRequestRejected(remoteMessage)
                    else -> handleMessageInBackground(remoteMessage)
                }
            }

            // Check if message contains a notification payload
            remoteMessage.notification?.let { notification ->
                Log.d("ToastMastersMsgSvc", "Message notification body: ${remoteMessage.notification?.body}")

                // Show notification if app is in background or if it's a high priority message
                if (!isAppInForeground() || remoteMessage.priority == RemoteMessage.PRIORITY_HIGH) {
                    showNotification(notification, remoteMessage.data)
                }
            }
        } catch (e: Exception) {
            Log.e("ToastMastersMsgSvc", "Error handling message", e)
        }
    }

    private fun handleMeetingCreated(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data
            val meetingId = data["meetingId"] ?: return
            val title = data["title"] ?: "New Meeting Scheduled"
            val body = data["body"] ?: "A new meeting has been scheduled."
            val date = data["date"]
            val location = data["location"]

            // Delegate to NotificationHelper
            notificationHelper.showMeetingNotification(
                meetingId = meetingId,
                title = title,
                message = body,
                date = date,
                location = location,
                isReminder = false,
                data = data
            )

            Log.d("ToastMastersMsgSvc", "Meeting created notification shown for meeting: $meetingId")
        } catch (e: Exception) {
            Log.e("ToastMastersMsgSvc", "Failed to handle meeting created notification", e)
        }
    }

    private fun createMeetingPendingIntent(meetingId: String): PendingIntent {
        return notificationHelper.createMeetingPendingIntent(meetingId)
    }

    private fun handleMessageInBackground(remoteMessage: RemoteMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Process the message data
                processMessageData(remoteMessage.data)

                // Show the notification
                notificationHelper.handleRemoteMessage(remoteMessage)

            } catch (e: Exception) {
                Log.e("ToastMastersMsgSvc", "Error processing FCM message", e)
            }
        }
    }

    private fun processMessageData(data: Map<String, String>) {
        // Extract data from the message
        val type = data["type"]
        val title = data["title"] ?: getString(R.string.app_name)
        val message = data["message"] ?: ""

        Log.d("ToastMastersMsgSvc", "Processing message - Type: $type, Title: $title, Message: $message")

        // Handle different message types
        when (type) {
            NotificationHelper.TYPE_MEMBER_APPROVAL -> {
                // Handle member approval notification
                val userId = data["user_id"]
                userId?.let {
                    // Update the local database or refresh the UI
                    Log.d("ToastMastersMsgSvc", "Member approved: $userId")
                }
            }

            NotificationHelper.TYPE_MENTOR_ASSIGNMENT -> {
                // Handle mentor assignment notification
                val userId = data["user_id"]
                val mentorNames = data["mentor_names"]
                Log.d("ToastMastersMsgSvc", "Mentor assigned - User: $userId, Mentor: $mentorNames")
            }

            NotificationHelper.TYPE_MEETING_CREATED -> {
                // Handle meeting created notification
                val meetingId = data["meeting_id"]
                meetingId?.let {
                    // TODO: Refresh the meetings list
                    Log.d("ToastMastersMsgSvc", "New meeting created: $meetingId")
                }
            }

            NotificationHelper.TYPE_NEW_MEMBER_SIGNUP -> {
                // Handle new member signup notification
                val userId = data["userId"]
                val userName = data["userName"]
                Log.d("ToastMastersMsgSvc", "New member signup - User: $userName, ID: $userId")
            }

            NotificationHelper.TYPE_MEMBER_BACKOUT -> {
                // Handle member backout notification
                val meetingId = data["meetingId"]
                val memberName = data["memberName"]
                val roleName = data["roleName"]
                Log.d("ToastMastersMsgSvc", "Member backout - Meeting: $meetingId, Member: $memberName, Role: $roleName")
            }

            NotificationHelper.TYPE_REQUEST_APPROVED -> {
                // Handle request approved notification
                val requestType = data["requestType"]
                Log.d("ToastMastersMsgSvc", "Request approved - Type: $requestType")
            }

            NotificationHelper.TYPE_REQUEST_REJECTED -> {
                // Handle request rejected notification
                val requestType = data["requestType"]
                val reason = data["reason"]
                Log.d("ToastMastersMsgSvc", "Request rejected - Type: $requestType, Reason: $reason")
            }

            else -> {
                // Handle other notification types or unknown types
                Log.d("ToastMastersMsgSvc", "Received unknown message type: $type")
            }
        }
    }

    private fun isAppInForeground(): Boolean {
        val appProcessInfo = android.app.ActivityManager.RunningAppProcessInfo()
        android.app.ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                appProcessInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    private fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID_MEETINGS)
            channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            // For older versions, check if notifications are enabled in system settings
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }

    private fun showNotification(
        notification: RemoteMessage.Notification,
        data: Map<String, String>
    ) {
        try {
            val title = notification.title ?: getString(R.string.app_name)
            val body = notification.body ?: ""

            // Create pending intent if needed
            val pendingIntent = data["click_action"]?.let { action ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    data.forEach { (key, value) ->
                        putExtra(key, value)
                    }
                }
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            // Delegate to NotificationHelper
            notificationHelper.showNotification(
                title = title,
                message = body,
                channelId = NotificationHelper.CHANNEL_ID_DEFAULT,
                notificationId = System.currentTimeMillis().toInt(),
                priority = NotificationCompat.PRIORITY_DEFAULT,
                autoCancel = true,
                pendingIntent = pendingIntent,
                data = data
            )
        } catch (e: Exception) {
            Log.e("ToastMastersMsgSvc", "Failed to show notification", e)
        }
    }

    private fun handleMeetingUpdated(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data
            val meetingId = data["meetingId"] ?: return
            val title = data["title"] ?: "Meeting Updated"
            val body = data["body"] ?: "A meeting has been updated."
            val date = data["date"]
            val location = data["location"]

            notificationHelper.showMeetingNotification(
                meetingId = meetingId,
                title = title,
                message = body,
                date = date,
                location = location,
                isReminder = false,
                data = data
            )
        } catch (e: Exception) {
            Log.e("ToastMastersMsgSvc", "Failed to handle meeting updated notification", e)
        }
    }

    private fun handleMeetingReminder(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data
            val meetingId = data["meetingId"] ?: return
            val title = data["title"] ?: "Meeting Reminder"
            val body = data["body"] ?: "You have a meeting starting soon."
            val time = data["time"] ?: ""
            val location = data["location"]

            notificationHelper.showMeetingNotification(
                meetingId = meetingId,
                title = title,
                message = "$body\n\nTime: $time",
                location = location,
                isReminder = true,
                data = data
            )
        } catch (e: Exception) {
            Log.e("ToastMastersMsgSvc", "Failed to handle meeting reminder notification", e)
        }
    }

    private fun handleNewMemberSignup(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data
            val title = data["title"] ?: "New Member Signup"
            val body = data["message"] ?: "A new member has signed up."
            val userName = data["userName"] ?: "Unknown"
            val userEmail = data["userEmail"] ?: ""

            notificationHelper.showNotification(
                title = title,
                message = body,
                channelId = NotificationHelper.CHANNEL_ID_IMPORTANT,
                notificationId = NotificationHelper.NOTIFICATION_ID_NEW_MEMBER_SIGNUP,
                priority = NotificationCompat.PRIORITY_HIGH,
                data = data
            )

            Log.d("ToastMastersMsgSvc", "New member signup notification shown for: $userName")
        } catch (e: Exception) {
            Log.e("ToastMastersMsgSvc", "Failed to handle new member signup notification", e)
        }
    }

    private fun handleMemberBackout(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data
            val title = data["title"] ?: "Member Backed Out"
            val body = data["message"] ?: "A member has backed out from their role."
            val memberName = data["memberName"] ?: "Unknown"
            val roleName = data["roleName"] ?: "Unknown Role"

            notificationHelper.showNotification(
                title = title,
                message = body,
                channelId = NotificationHelper.CHANNEL_ID_IMPORTANT,
                notificationId = NotificationHelper.NOTIFICATION_ID_MEMBER_BACKOUT,
                priority = NotificationCompat.PRIORITY_HIGH,
                data = data
            )

            Log.d("ToastMastersMsgSvc", "Member backout notification shown for: $memberName from role: $roleName")
        } catch (e: Exception) {
            Log.e("ToastMastersMsgSvc", "Failed to handle member backout notification", e)
        }
    }

    private fun handleRequestApproved(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data
            val title = data["title"] ?: "Request Approved"
            val body = data["message"] ?: "Your request has been approved."
            val requestType = data["requestType"] ?: "request"

            notificationHelper.showNotification(
                title = title,
                message = body,
                channelId = NotificationHelper.CHANNEL_ID_IMPORTANT,
                notificationId = NotificationHelper.NOTIFICATION_ID_REQUEST_APPROVED,
                priority = NotificationCompat.PRIORITY_HIGH,
                data = data
            )

            Log.d("ToastMastersMsgSvc", "Request approved notification shown for: $requestType")
        } catch (e: Exception) {
            Log.e("ToastMastersMsgSvc", "Failed to handle request approved notification", e)
        }
    }

    private fun handleRequestRejected(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data
            val title = data["title"] ?: "Request Rejected"
            val body = data["message"] ?: "Your request has been rejected."
            val requestType = data["requestType"] ?: "request"
            val reason = data["reason"] ?: ""

            notificationHelper.showNotification(
                title = title,
                message = body,
                channelId = NotificationHelper.CHANNEL_ID_IMPORTANT,
                notificationId = NotificationHelper.NOTIFICATION_ID_REQUEST_REJECTED,
                priority = NotificationCompat.PRIORITY_DEFAULT,
                data = data
            )

            Log.d("ToastMastersMsgSvc", "Request rejected notification shown for: $requestType, reason: $reason")
        } catch (e: Exception) {
            Log.e("ToastMastersMsgSvc", "Failed to handle request rejected notification", e)
        }
    }
}
