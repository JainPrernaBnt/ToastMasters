package com.bntsoft.toastmasters.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bntsoft.toastmasters.domain.models.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREFS_NAME = "ToastMastersPrefs"

        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_PENDING_TOKEN_UPDATE = "pending_token_update"
        private const val KEY_DEVICE_ID = "device_id"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_AUTH_TOKEN, value) }

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit { putString(KEY_USER_ID, value) }

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit { putString(KEY_USER_EMAIL, value) }

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var fcmToken: String?
        get() = prefs.getString(KEY_FCM_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_FCM_TOKEN, value) }

    var areNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, value) }

    var pendingTokenUpdate: String?
        get() = prefs.getString(KEY_PENDING_TOKEN_UPDATE, null)
        set(value) = prefs.edit { putString(KEY_PENDING_TOKEN_UPDATE, value) }

    var isLoggedIn: Boolean
        get() {
            val value = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            android.util.Log.d("PreferenceManager", "isLoggedIn getter called - returning: $value")
            return value
        }
        set(value) {
            android.util.Log.d("PreferenceManager", "isLoggedIn setter called - setting to: $value")
            prefs.edit { putBoolean(KEY_IS_LOGGED_IN, value) }
        }

    fun saveUserRole(role: UserRole) {
        prefs.edit { putString(KEY_USER_ROLE, role.name) }
    }

    fun getUserRole(): UserRole? {
        val roleName = prefs.getString(KEY_USER_ROLE, null)
        return roleName?.let {
            try {
                UserRole.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    fun clearUserData() {
        android.util.Log.d("PreferenceManager", "Clearing user data")
        clearAll()
    }

    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit { putString(KEY_DEVICE_ID, value) }

    fun clearAll() {
        android.util.Log.d("PreferenceManager", "clearAll() called - before clear: isLoggedIn=${isLoggedIn}, userId=${userId}")
        val deviceId = prefs.getString(KEY_DEVICE_ID, null)
        prefs.edit().clear().apply()
        // Preserve the device ID across logouts
        if (deviceId != null) {
            prefs.edit { putString(KEY_DEVICE_ID, deviceId) }
        }
        android.util.Log.d("PreferenceManager", "clearAll() completed - after clear: isLoggedIn=${isLoggedIn}, userId=${userId}")
    }
}
