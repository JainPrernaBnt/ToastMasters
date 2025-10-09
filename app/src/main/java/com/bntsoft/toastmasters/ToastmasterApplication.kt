package com.bntsoft.toastmasters

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.bntsoft.toastmasters.service.ToastMastersMessagingService
import com.bntsoft.toastmasters.utils.FcmTokenManager
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import javax.inject.Inject

@HiltAndroidApp
class ToastmasterApplication : Application() {

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager
    
    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
//        FirebaseFirestore.setLoggingEnabled(true)
        // Initialize logging
        if (BuildConfig.DEBUG) {
            // Using Android Log instead of Timber
        }

        // Create notification channels
        createNotificationChannels()

        // Initialize FCM token
        initializeFcmToken()
        
        // Setup Firebase Auth persistence
        setupAuthStatePersistence()
    }
    
    private fun setupAuthStatePersistence() {
        val auth = FirebaseAuth.getInstance()
        
        // Add auth state listener to sync with preferences
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            val isLoggedInPrefs = preferenceManager.isLoggedIn
            
            Log.d("ToastmasterApp", "Auth state changed - Firebase user: ${firebaseUser != null}, Prefs logged in: $isLoggedInPrefs")
            
            // Only clear preferences if Firebase user is null AND we have a logged in state
            // This prevents clearing preferences during normal app startup
            if (firebaseUser == null && isLoggedInPrefs) {
                Log.d("ToastmasterApp", "No Firebase user but preferences indicate logged in - clearing preferences")
                preferenceManager.clearUserData()
            }
            
            // Don't automatically sign out Firebase user if preferences are cleared
            // Let the BaseActivity handle this to avoid race conditions
        }
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

                Log.d("ToastmasterApp", "FCM Token: $token")
            } catch (e: Exception) {
                Log.e("ToastmasterApp", "Error getting FCM token", e)
            }
        }
    }
}