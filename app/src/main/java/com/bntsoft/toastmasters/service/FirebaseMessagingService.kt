package com.bntsoft.toastmasters.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bntsoft.toastmasters.MainActivity
import com.bntsoft.toastmasters.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "toastmasters_notifications"
        private const val CHANNEL_NAME = "ToastMasters Notifications"
        private const val CHANNEL_DESCRIPTION =
            "Notifications for upcoming meetings and important updates"
        private const val NOTIFICATION_ID = 1001

        // Notification actions
        private const val ACTION_MEETING_DETAILS = "meeting_details"
        private const val EXTRA_MEETING_ID = "meeting_id"
    }

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload of FCM messages
        remoteMessage.notification?.let { notification ->
            val meetingId = remoteMessage.data["meeting_id"]

            Log.d("FirebaseMsgService", "Message data payload: ${remoteMessage.data}")

            sendNotification(
                title = notification.title ?: getString(R.string.app_name),
                message = notification.body ?: "",
                meetingId = meetingId
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send the token to your server if needed
        // You can also subscribe to topics here if needed
        Log.d("FirebaseMsgService", "New FCM token: $token")
    }

    private fun sendNotification(title: String, message: String, meetingId: String? = null) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            // Set flags to create a new task and clear any existing ones
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Add meeting ID as an extra if available
            if (!meetingId.isNullOrEmpty()) {
                putExtra(EXTRA_MEETING_ID, meetingId)
                // Optionally, you can add a fragment tag or other identifier here
                // to help MainActivity navigate to the meeting detail
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon as fallback
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Add actions if needed (e.g., Join Meeting, View Details)
        if (!meetingId.isNullOrEmpty()) {
            notificationBuilder.addAction(
                android.R.drawable.ic_menu_info_details, // Using system icon as fallback
                "View Details",
                pendingIntent
            )
        }

        // Generate a unique notification ID for each notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
