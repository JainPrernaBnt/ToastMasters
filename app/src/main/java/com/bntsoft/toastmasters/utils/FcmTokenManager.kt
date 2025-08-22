package com.bntsoft.toastmasters.utils

import android.content.Context
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class FcmTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val prefsManager: PreferenceManager,
    private val userRepository: UserRepository
) {
    companion object {
        private const val TAG = "FcmTokenManager"
        private const val MAX_RETRY_ATTEMPTS = 5
    }

    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        // Get and update the FCM token when the app starts
        getFcmToken()
    }

    /**
     * Get the current FCM token and update it on the server if needed
     */
    fun getFcmToken() {
        ioScope.launch {
            try {
                // Get the current token
                val token = FirebaseMessaging.getInstance().token.await()

                // Get the last saved token
                val savedToken = prefsManager.fcmToken

                // Check if token is valid and different from the saved one
                if (token.isNotBlank() && token != savedToken) {
                    Timber.d("New FCM token generated")
                    updateToken(token)
                } else if (savedToken.isNullOrBlank()) {
                    Timber.d("No saved token found, updating with new token")
                    updateToken(token)
                } else {
                    Timber.d("Using existing valid FCM token")
                    // Verify the token is still valid on the server
                    verifyTokenOnServer(savedToken)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get FCM token")
                // Retry after a delay if token fetch fails
                retryTokenFetch()
            }
        }
    }

    /**
     * Update the FCM token in local storage and on the server
     */
    suspend fun updateToken(token: String) {
        try {
            // Save to shared preferences
            prefsManager.fcmToken = token

            // Update on server if user is logged in
            firebaseAuth.currentUser?.uid?.let { userId ->
                updateFcmTokenOnServer(userId, token)
            }

            Timber.d("FCM token updated successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update FCM token")
            throw e
        }
    }

    private suspend fun updateFcmTokenOnServer(userId: String, token: String) {
        try {
            userRepository.updateFcmToken(userId, token)
            Timber.d("FCM token updated on server for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update FCM token on server")
            // Queue the token update for retry
            queueTokenForRetry(userId, token)
            throw e
        }
    }

    /**
     * Verify if the token is still valid on the server
     */
    private suspend fun verifyTokenOnServer(token: String) {
        try {
            firebaseAuth.currentUser?.uid?.let { userId ->
                // For now, just try to update the token
                // In a real app, you might want to implement actual token verification
                updateFcmTokenOnServer(userId, token)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify FCM token on server")
        }
    }

    /**
     * Queue a token update for retry
     */
    private fun queueTokenForRetry(userId: String, token: String) {
        // Store in SharedPreferences for retry
        prefsManager.pendingTokenUpdate = "$userId:$token"

        // Schedule a retry
        ioScope.launch {
            kotlinx.coroutines.delay(5 * 60 * 1000) // Retry after 5 minutes
            retryPendingTokenUpdate()
        }
    }

    /**
     * Retry any pending token updates
     */
    private suspend fun retryPendingTokenUpdate() {
        val pendingUpdate = prefsManager.pendingTokenUpdate ?: return
        val (userId, token) = pendingUpdate.split(":", limit = 2)

        try {
            userRepository.updateFcmToken(userId, token)
            prefsManager.pendingTokenUpdate = null
            Timber.d("Successfully retried pending FCM token update")
        } catch (e: Exception) {
            Timber.e(e, "Failed to retry pending FCM token update")
            // Will retry on next app launch
        }
    }

    /**
     * Retry token fetch with exponential backoff
     */
    private fun retryTokenFetch(attempt: Int = 1) {
        if (attempt > MAX_RETRY_ATTEMPTS) {
            Timber.w("Max retry attempts reached for FCM token fetch")
            return
        }

        val delayMs = (2.0.pow(attempt.toDouble()) * 1000).toLong()

        ioScope.launch {
            kotlinx.coroutines.delay(delayMs)
            Timber.d("Retrying FCM token fetch (attempt $attempt)")
            getFcmToken()
        }
    }

    /**
     * Delete the FCM token when user logs out
     */
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

                Timber.d("FCM token deleted successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete FCM token")
            }
        }
    }

    private suspend fun removeFcmTokenFromServer(userId: String) {
        try {
            userRepository.removeFcmToken(userId)
            Timber.d("FCM token removed from server for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove FCM token from server")
            throw e
        }
    }

}
