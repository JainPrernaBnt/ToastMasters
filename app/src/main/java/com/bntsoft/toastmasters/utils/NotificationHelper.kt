package com.bntsoft.toastmasters.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bntsoft.toastmasters.MainActivity
import com.bntsoft.toastmasters.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context,
    private val firebaseMessaging: FirebaseMessaging,
    private val prefsManager: PrefsManager
) {
    
    companion object {
        // Notification channel IDs
        const val CHANNEL_ID_DEFAULT = "default_channel"
        const val CHANNEL_ID_IMPORTANT = "important_channel"
        
        // Notification IDs
        const val NOTIFICATION_ID_MEMBER_APPROVED = 1001
        const val NOTIFICATION_ID_MENTOR_ASSIGNED = 1002
        const val NOTIFICATION_ID_MEETING_CREATED = 1003
        
        // Topic for all users
        private const val TOPIC_ALL = "all_users"
        
        // Topic for members
        private const val TOPIC_MEMBERS = "members"
        
        // Topic for VP Education
        private const val TOPIC_VP_EDUCATION = "vp_education"
        
        // Topic for club officers
        private const val TOPIC_OFFICERS = "officers"
        
        // Notification types
        const val TYPE_MEMBER_APPROVAL = "member_approval"
        const val TYPE_MENTOR_ASSIGNMENT = "mentor_assignment"
        const val TYPE_MEETING_CREATED = "meeting_created"
        const val TYPE_MEETING_REMINDER = "meeting_reminder"
        
        // Intent extras
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_MEETING_ID = "meeting_id"
        const val EXTRA_USER_ID = "user_id"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    
    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Default channel for general notifications
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                context.getString(R.string.notification_channel_default),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_default_description)
            }
            
            // Important channel for high-priority notifications
            val importantChannel = NotificationChannel(
                CHANNEL_ID_IMPORTANT,
                context.getString(R.string.notification_channel_important),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_important_description)
                enableVibration(true)
            }
            
            // Register the channels with the system
            notificationManager.createNotificationChannels(listOf(defaultChannel, importantChannel))
        }
    }

    suspend fun subscribeToTopics(role: String) {
        try {
            // All users are subscribed to the general topic
            firebaseMessaging.subscribeToTopic(TOPIC_ALL).await()
            
            // Subscribe to role-specific topics
            when (role.lowercase(Locale.getDefault())) {
                "member" -> {
                    firebaseMessaging.subscribeToTopic(TOPIC_MEMBERS).await()
                }
                "vp_education" -> {
                    firebaseMessaging.subscribeToTopic(TOPIC_VP_EDUCATION).await()
                    firebaseMessaging.subscribeToTopic(TOPIC_OFFICERS).await()
                }
                // Add other roles as needed
            }
            
            // Save the subscription state
            prefsManager.areNotificationsEnabled = true
        } catch (e: Exception) {
            // Handle error - could log to Crashlytics or similar
            e.printStackTrace()
        }
    }

    suspend fun unsubscribeFromAllTopics() {
        try {
            // Unsubscribe from all topics
            firebaseMessaging.unsubscribeFromTopic(TOPIC_ALL).await()
            firebaseMessaging.unsubscribeFromTopic(TOPIC_MEMBERS).await()
            firebaseMessaging.unsubscribeFromTopic(TOPIC_VP_EDUCATION).await()
            firebaseMessaging.unsubscribeFromTopic(TOPIC_OFFICERS).await()
            
            // Save the subscription state
            prefsManager.areNotificationsEnabled = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    

    fun getTopicForAudience(audience: NotificationAudience): String {
        return when (audience) {
            NotificationAudience.ALL -> TOPIC_ALL
            NotificationAudience.MEMBERS -> TOPIC_MEMBERS
            NotificationAudience.VP_EDUCATION -> TOPIC_VP_EDUCATION
            NotificationAudience.OFFICERS -> TOPIC_OFFICERS
        }
    }
    

    fun showNotification(
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt(),
        channelId: String = CHANNEL_ID_DEFAULT,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        autoCancel: Boolean = true,
        data: Map<String, String>? = null
    ) {
        // Create an explicit intent for the MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get the appropriate icon based on notification type
        val iconResId = when (data?.get("type")) {
            TYPE_MEETING_CREATED, TYPE_MEETING_REMINDER -> R.drawable.ic_notification_meeting
            TYPE_MENTOR_ASSIGNMENT -> R.drawable.ic_notification_mentor
            else -> R.drawable.ic_notification_default
        }
        
        // Build the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconResId)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(autoCancel)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        
        // Show the notification
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(notificationId, builder.build())
    }

    fun handleRemoteMessage(remoteMessage: RemoteMessage) {
        val notification = remoteMessage.notification
        val data = remoteMessage.data
        
        if (notification != null) {
            // Handle notification message
            showNotification(
                title = notification.title ?: context.getString(R.string.app_name),
                message = notification.body ?: "",
                data = data
            )
        } else if (data.isNotEmpty()) {
            // Handle data message
            val title = data["title"] ?: context.getString(R.string.app_name)
            val message = data["message"] ?: ""
            val type = data["type"]
            
            // Handle different notification types
            when (type) {
                TYPE_MEMBER_APPROVAL -> {
                    handleMemberApprovalNotification(title, message, data)
                }
                TYPE_MENTOR_ASSIGNMENT -> {
                    handleMentorAssignmentNotification(title, message, data)
                }
                TYPE_MEETING_CREATED -> {
                    handleMeetingCreatedNotification(title, message, data)
                }
                else -> {
                    // Default notification
                    showNotification(title, message, data = data)
                }
            }
        }
    }
    
    /**
     * Handle member approval notification
     */
    private fun handleMemberApprovalNotification(
        title: String,
        message: String,
        data: Map<String, String>
    ) {
        // Show a high-priority notification for member approval
        showNotification(
            title = title,
            message = message,
            notificationId = NOTIFICATION_ID_MEMBER_APPROVED,
            channelId = CHANNEL_ID_IMPORTANT,
            priority = NotificationCompat.PRIORITY_HIGH,
            data = data
        )
        
        // You can also update the UI or perform other actions here
        // For example, refresh the member list in the MemberApprovalFragment
    }
    
    /**
     * Handle mentor assignment notification
     */
    private fun handleMentorAssignmentNotification(
        title: String,
        message: String,
        data: Map<String, String>
    ) {
        // Show a notification for mentor assignment
        showNotification(
            title = title,
            message = message,
            notificationId = NOTIFICATION_ID_MENTOR_ASSIGNED,
            channelId = CHANNEL_ID_DEFAULT,
            data = data
        )
    }
    
    /**
     * Handle meeting created notification
     */
    private fun handleMeetingCreatedNotification(
        title: String,
        message: String,
        data: Map<String, String>
    ) {
        // Show a high-priority notification for new meetings
        showNotification(
            title = title,
            message = message,
            notificationId = NOTIFICATION_ID_MEETING_CREATED,
            channelId = CHANNEL_ID_IMPORTANT,
            priority = NotificationCompat.PRIORITY_HIGH,
            data = data
        )
    }
    
    /**
     * Cancel a specific notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}

/**
 * Represents the target audience for a notification
 */
enum class NotificationAudience {
    ALL,          // All users
    MEMBERS,      // Regular members
    VP_EDUCATION, // VP Education only
    OFFICERS      // All club officers
}
