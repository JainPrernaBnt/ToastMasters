package com.bntsoft.toastmasters.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bntsoft.toastmasters.data.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ToastmastersPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    var isLoggedIn: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()


    fun saveUserRole(role: UserRole) {
        sharedPreferences.edit {
            putString(KEY_USER_ROLE, role.name)
            apply()
        }
    }

    fun getUserRole(): UserRole? {
        val roleName = sharedPreferences.getString(KEY_USER_ROLE, null)
        return roleName?.let {
            try {
                UserRole.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    fun clearUserData() {
        sharedPreferences.edit {
            remove(KEY_USER_ROLE)
            remove(KEY_IS_LOGGED_IN)
            apply()
        }
    }
}
