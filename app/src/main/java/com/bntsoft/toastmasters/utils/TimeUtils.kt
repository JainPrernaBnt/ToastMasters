package com.bntsoft.toastmasters.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimeUtils {
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun calculateNextTime(currentTime: String, minutesToAdd: Int): String {
        return try {
            val calendar = Calendar.getInstance()
            val date = timeFormat.parse(currentTime) ?: return currentTime
            calendar.time = date
            calendar.add(Calendar.MINUTE, minutesToAdd)
            timeFormat.format(calendar.time)
        } catch (e: Exception) {
            currentTime // Return original time if parsing fails
        }
    }

    fun parseTimeToMinutes(timeString: String): Int {
        return try {
            val calendar = Calendar.getInstance()
            val date = timeFormat.parse(timeString) ?: return 0
            calendar.time = date
            calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        } catch (e: Exception) {
            0
        }
    }

    fun formatMinutesToTime(minutes: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, minutes / 60)
        calendar.set(Calendar.MINUTE, minutes % 60)
        return timeFormat.format(calendar.time)
    }
}
