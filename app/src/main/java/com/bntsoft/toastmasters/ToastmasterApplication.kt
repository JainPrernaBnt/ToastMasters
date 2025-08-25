package com.bntsoft.toastmasters

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.bntsoft.toastmasters.service.ToastMastersMessagingService
import com.bntsoft.toastmasters.utils.FcmTokenManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ToastmasterApplication : Application() {

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
//        FirebaseFirestore.setLoggingEnabled(true)
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Create notification channels
        createNotificationChannels()

        // Initialize FCM token
        initializeFcmToken()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Meetings channel
            val meetingsChannel = NotificationChannel(
                ToastMastersMessagingService.CHANNEL_ID_MEETINGS,
                "Meetings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about upcoming meetings and schedule changes"
                enableVibration(true)
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(meetingsChannel)
        }
    }

    private fun initializeFcmToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the FCM token
                val token = FirebaseMessaging.getInstance().token.await()

                // Update the token in the repository
                fcmTokenManager.updateToken(token)

                // Subscribe to general topics
                FirebaseMessaging.getInstance().subscribeToTopic("all_members")

                Timber.d("FCM token initialized and updated: ${token.take(10)}...")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize FCM token")
            }
        }
    }
}