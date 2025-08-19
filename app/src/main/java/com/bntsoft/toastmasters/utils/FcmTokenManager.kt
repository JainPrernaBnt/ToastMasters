package com.bntsoft.toastmasters.utils

import android.content.Context
import android.util.Log
import com.bntsoft.toastmasters.R
import com.google.firebase.auth.FirebaseAuth
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val prefsManager: PrefsManager,
    private val userRepository: UserRepository
) {
    private val TAG = "FcmTokenManager"
    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        // Get and update the FCM token when the app starts
        getFcmToken()
    }

    fun getFcmToken() {
        ioScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM Token: $token")
                
                // Save token to shared preferences
                prefsManager.fcmToken = token
                
                // If user is logged in, update the token on the server
                firebaseAuth.currentUser?.let { user ->
                    updateFcmTokenOnServer(user.uid, token)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FCM token", e)
            }
        }
    }

    private suspend fun updateFcmTokenOnServer(userId: String, token: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating FCM token for user $userId")
                
                // Update FCM token using UserRepository
                when (val result = userRepository.updateFcmToken(userId, token)) {
                    is Result.Success -> {
                        Log.d(TAG, "FCM token updated successfully for user $userId")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Failed to update FCM token: ${result.exception.message}")
                    }
                    else -> {}
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token on server", e)
            }
        }
    }

    fun deleteFcmToken() {
        ioScope.launch {
            try {
                // Delete the token from Firebase
                FirebaseMessaging.getInstance().deleteToken().await()
                
                // Clear from shared preferences
                prefsManager.fcmToken = ""
                
                // If user is logged in, remove the token from the server
                firebaseAuth.currentUser?.let { user ->
                    removeFcmTokenFromServer(user.uid)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete FCM token", e)
            }
        }
    }

    private suspend fun removeFcmTokenFromServer(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Remove FCM token using UserRepository
                when (val result = userRepository.removeFcmToken(userId)) {
                    is Result.Success -> {
                        Log.d(TAG, "FCM token removed successfully for user $userId")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Failed to remove FCM token: ${result.exception.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove FCM token from server", e)
            }
            try {
                // TODO: Implement API call to remove FCM token from your server
                // For now, we'll just log it
                Log.d(TAG, "Removing FCM token for user $userId")
                
                // Example: firebaseFirestore.collection("users").document(userId)
                //     .update("fcmToken", FieldValue.delete())
                //     .await()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove FCM token from server", e)
            }
        }
    }

    companion object {
        // Notification channel IDs
        const val CHANNEL_ID_DEFAULT = "default_channel"
        const val CHANNEL_ID_IMPORTANT = "important_channel"
        
        // Notification IDs
        const val NOTIFICATION_ID_MEMBER_APPROVED = 1001
        const val NOTIFICATION_ID_MENTOR_ASSIGNED = 1002
        const val NOTIFICATION_ID_MEETING_CREATED = 1003
    }
}
