package com.bntsoft.toastmasters.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for date and time formatting.
 */
object DateTimeUtils {
    
    private const val DATE_TIME_FORMAT = "MMM d, yyyy hh:mm a"
    private const val TIME_FORMAT = "hh:mm a"
    private const val DATE_FORMAT = "MMM d, yyyy"
    
    /**
     * Format a timestamp to a readable date and time string.
     * @param timestamp The timestamp to format (in milliseconds)
     * @return Formatted date and time string
     */
    fun formatDateTime(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Format a timestamp to a time string.
     * @param timestamp The timestamp to format (in milliseconds)
     * @return Formatted time string
     */
    fun formatTime(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Format a timestamp to a date string.
     * @param timestamp The timestamp to format (in milliseconds)
     * @return Formatted date string
     */
    fun formatDate(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Format a time difference to a relative time string (e.g., "2 minutes ago").
     * @param timestamp The timestamp to format (in milliseconds)
     * @return Relative time string
     */
    fun getRelativeTimeSpanString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 1000 * 60 -> "Just now"
            diff < 1000 * 60 * 60 -> "${diff / (1000 * 60)} minutes ago"
            diff < 1000 * 60 * 60 * 24 -> "${diff / (1000 * 60 * 60)} hours ago"
            diff < 1000L * 60 * 60 * 24 * 30 -> "${diff / (1000 * 60 * 60 * 24)} days ago"
            diff < 1000L * 60 * 60 * 24 * 365 -> "${diff / (1000L * 60 * 60 * 24 * 30)} months ago"
            else -> formatDate(timestamp)
        }
    }
}
