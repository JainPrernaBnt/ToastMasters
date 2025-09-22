package com.bntsoft.toastmasters.utils

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager
) {

    fun getDeviceId(): String {
        return preferenceManager.deviceId ?: generateAndStoreDeviceId()
    }

    private fun generateAndStoreDeviceId(): String {
        return try {
            // Try to get ANDROID_ID first
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            
            // If ANDROID_ID is not available or is invalid, generate a random UUID
            val deviceId = if (androidId.isNullOrEmpty() || androidId == "9774d56d682e549c") {
                UUID.randomUUID().toString()
            } else {
                // Use ANDROID_ID as a seed to generate a consistent UUID
                UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
            }
            
            // Store the device ID
            preferenceManager.deviceId = deviceId
            deviceId
        } catch (e: Exception) {
            // Fallback to random UUID if anything goes wrong
            val fallbackId = UUID.randomUUID().toString()
            preferenceManager.deviceId = fallbackId
            fallbackId
        }
    }

    fun isCurrentDevice(deviceId: String?): Boolean {
        if (deviceId.isNullOrEmpty()) return true // If no device ID is stored, allow access
        return deviceId == preferenceManager.deviceId
    }
}
