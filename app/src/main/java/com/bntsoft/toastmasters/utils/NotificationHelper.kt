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
import com.bntsoft.toastmasters.domain.models.NotificationAudience
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context,
    private val firebaseMessaging: FirebaseMessaging,
    private val prefsManager: PreferenceManager
) {

    companion object {
        // Notification channel IDs
        const val CHANNEL_ID_DEFAULT = "default_channel"
        const val CHANNEL_ID_IMPORTANT = "important_channel"
        const val CHANNEL_ID_MEETINGS = "meetings_channel"

        // Notification IDs
        const val NOTIFICATION_ID_MEMBER_APPROVED = 1001
        const val NOTIFICATION_ID_MENTOR_ASSIGNED = 1002
        const val NOTIFICATION_ID_MEETING_CREATED = 1003
        const val NOTIFICATION_ID_MEETING_UPDATED = 1004
        const val NOTIFICATION_ID_MEETING_REMINDER = 1005

        // Notification types

        // Topic for all users
        private const val TOPIC_ALL = "all_users"

        // Topic for members
        private const val TOPIC_MEMBERS = "members"

        // Topic for VP Education
        private const val TOPIC_VP_EDUCATION = "vp_education"

        // Intent extras
        const val EXTRA_FRAGMENT = "fragment"
        const val FRAGMENT_MEETING_DETAILS = "meeting_details"

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

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                context.getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.default_notification_channel_description)
            }

            val importantChannel = NotificationChannel(
                CHANNEL_ID_IMPORTANT,
                context.getString(R.string.important_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.important_notification_channel_description)
                setShowBadge(true)
                enableVibration(true)
            }

            val meetingsChannel = NotificationChannel(
                CHANNEL_ID_MEETINGS,
                context.getString(R.string.meetings_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.meetings_notification_channel_description)
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
                lightColor = context.getColor(R.color.primary)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannels(
                listOf(defaultChannel, importantChannel, meetingsChannel)
            )
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
        }
    }

    fun showNotification(
        title: String,
        message: String,
        channelId: String = CHANNEL_ID_DEFAULT,
        notificationId: Int = System.currentTimeMillis().toInt(),
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        autoCancel: Boolean = true,
        pendingIntent: PendingIntent? = null,
        style: NotificationCompat.Style? = null,
        actions: List<NotificationCompat.Action> = emptyList(),
        data: Map<String, String> = emptyMap()
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(autoCancel)
            .apply {
                style?.let { setStyle(it) }
                pendingIntent?.let { setContentIntent(it) }
                actions.forEach { addAction(it) }
            }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
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

    }

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

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    fun showMeetingNotification(
        meetingId: String,
        title: String,
        message: String,
        date: String? = null,
        location: String? = null,
        isReminder: Boolean = false,
        data: Map<String, String> = emptyMap()
    ) {
        try {
            // Build the notification content
            val notificationId = System.currentTimeMillis().toInt()
            val channelId = if (isReminder) CHANNEL_ID_IMPORTANT else CHANNEL_ID_MEETINGS
            val priority =
                if (isReminder) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

            // Create the notification style
            val style = NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(buildMeetingNotificationText(message, date, location))

            // Create the pending intent for the notification
            val pendingIntent = createMeetingPendingIntent(meetingId)

            // Build and show the notification
            showNotification(
                title = title,
                message = message,
                channelId = channelId,
                notificationId = notificationId,
                priority = priority,
                pendingIntent = pendingIntent,
                style = style,
                actions = listOf(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_calendar,
                        context.getString(R.string.view_meeting),
                        pendingIntent
                    ).build()
                ),
                data = data
            )

            Log.d("NotificationHelper", "Meeting notification shown: $title")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to show meeting notification", e)
        }
    }

    fun createMeetingPendingIntent(meetingId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_MEETING_ID, meetingId)
            putExtra(EXTRA_FRAGMENT, FRAGMENT_MEETING_DETAILS)
        }
        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildMeetingNotificationText(
        message: String,
        date: String?,
        location: String?
    ): String {
        return buildString {
            append(message)
            date?.let { append("\n\nDate: $it") }
            location?.let { append("\nLocation: $it") }
        }
    }
}

