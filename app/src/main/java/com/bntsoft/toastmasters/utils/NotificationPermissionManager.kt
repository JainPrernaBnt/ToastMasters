package com.bntsoft.toastmasters.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bntsoft.toastmasters.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPermissionManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "NotificationPermissionPrefs"
        private const val PREF_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        private const val PREF_NOTIFICATION_PERMISSION_DENIED_COUNT = "notification_permission_denied_count"
        private const val MAX_PERMISSION_DENIAL_COUNT = 2
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isNotificationPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissions not required for older versions
        }
    }

    fun shouldRequestPermission(context: Context): Boolean {
        if (!isNotificationPermissionRequired()) return false
        if (hasNotificationPermission(context)) return false
        
        val hasBeenRequested = prefs.getBoolean(PREF_NOTIFICATION_PERMISSION_REQUESTED, false)
        val denialCount = prefs.getInt(PREF_NOTIFICATION_PERMISSION_DENIED_COUNT, 0)
        
        return !hasBeenRequested || denialCount < MAX_PERMISSION_DENIAL_COUNT
    }

    fun shouldShowRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }

    fun requestNotificationPermission(
        activity: FragmentActivity,
        onPermissionResult: (Boolean) -> Unit
    ) {
        if (!isNotificationPermissionRequired()) {
            onPermissionResult(true)
            return
        }

        if (hasNotificationPermission(activity)) {
            onPermissionResult(true)
            return
        }

        val launcher = activity.activityResultRegistry.register(
            "notification_permission",
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            handlePermissionResult(isGranted, onPermissionResult)
        }

        if (shouldShowRationale(activity)) {
            showPermissionRationale(activity) {
                requestPermissionWithLauncher(launcher)
            }
        } else {
            requestPermissionWithLauncher(launcher)
        }
    }

    fun requestNotificationPermissionFromFragment(
        fragment: Fragment,
        launcher: ActivityResultLauncher<String>,
        onPermissionResult: (Boolean) -> Unit
    ) {
        if (!isNotificationPermissionRequired()) {
            onPermissionResult(true)
            return
        }

        val context = fragment.requireContext()
        if (hasNotificationPermission(context)) {
            onPermissionResult(true)
            return
        }

        if (shouldShowRationale(fragment.requireActivity())) {
            showPermissionRationale(fragment.requireContext()) {
                requestPermissionWithLauncher(launcher)
            }
        } else {
            requestPermissionWithLauncher(launcher)
        }
    }

    private fun requestPermissionWithLauncher(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handlePermissionResult(
        isGranted: Boolean,
        onPermissionResult: (Boolean) -> Unit
    ) {
        prefs.edit().putBoolean(PREF_NOTIFICATION_PERMISSION_REQUESTED, true).apply()
        
        if (isGranted) {
            prefs.edit().putInt(PREF_NOTIFICATION_PERMISSION_DENIED_COUNT, 0).apply()
            onPermissionResult(true)
        } else {
            val currentCount = prefs.getInt(PREF_NOTIFICATION_PERMISSION_DENIED_COUNT, 0)
            prefs.edit().putInt(PREF_NOTIFICATION_PERMISSION_DENIED_COUNT, currentCount + 1).apply()
            onPermissionResult(false)
        }
    }

    private fun showPermissionRationale(
        context: Context,
        onPositiveAction: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.notification_permission_title))
            .setMessage(context.getString(R.string.notification_permission_message))
            .setPositiveButton(context.getString(R.string.allow)) { _, _ ->
                onPositiveAction()
            }
            .setNegativeButton(context.getString(R.string.not_now)) { dialog, _ ->
                dialog.dismiss()
                handlePermissionResult(false) { }
            }
            .setCancelable(false)
            .show()
    }

    fun showPermissionExplanationDialog(
        context: Context,
        userRole: String,
        onSettingsClicked: () -> Unit
    ) {
        val message = when (userRole.lowercase()) {
            "vp_education" -> context.getString(R.string.notification_permission_vp_education_explanation)
            "member" -> context.getString(R.string.notification_permission_member_explanation)
            else -> context.getString(R.string.notification_permission_general_explanation)
        }

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.enable_notifications))
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.go_to_settings)) { _, _ ->
                onSettingsClicked()
            }
            .setNegativeButton(context.getString(R.string.maybe_later)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun markPermissionAsRequested() {
        prefs.edit().putBoolean(PREF_NOTIFICATION_PERMISSION_REQUESTED, true).apply()
    }

    fun resetPermissionState() {
        prefs.edit().putBoolean(PREF_NOTIFICATION_PERMISSION_REQUESTED, false).apply()
        prefs.edit().putInt(PREF_NOTIFICATION_PERMISSION_DENIED_COUNT, 0).apply()
    }

    fun getPermissionDenialCount(): Int {
        return prefs.getInt(PREF_NOTIFICATION_PERMISSION_DENIED_COUNT, 0)
    }

    fun hasReachedMaxDenials(): Boolean {
        return getPermissionDenialCount() >= MAX_PERMISSION_DENIAL_COUNT
    }
}
