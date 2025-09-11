package com.bntsoft.toastmasters.utils

import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import java.text.SimpleDateFormat
import java.util.*

object AgendaTimeCalculator {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    /**
     * Calculate times for all agenda items based on the meeting start time
     * @param items List of agenda items sorted by orderIndex
     * @param meetingStartTime Start time of the meeting in format "h:mm a"
     * @return List of agenda items with calculated times
     */
    fun calculateAgendaTimes(items: List<AgendaItemDto>, meetingStartTime: String): List<AgendaItemDto> {
        if (items.isEmpty()) return items

        val result = mutableListOf<AgendaItemDto>()
        val calendar = Calendar.getInstance()

        val start = try { timeFormat.parse(meetingStartTime) } catch (_: Exception) { null } ?: return items
        var currentTime = start

        items.forEach { item ->
            // Set current item's displayed time
            val timeString = timeFormat.format(currentTime)

            // Determine if this row is a break-style header: either flagged or text contains BREAK
            val isBreakHeader = item.isSessionHeader || (item.activity.contains("BREAK", ignoreCase = true))

            if (isBreakHeader) {
                result.add(item.copy(time = timeString))
                val breakMinutes = (item.redTime / 60).coerceAtLeast(0)
                if (breakMinutes > 0) {
                    calendar.time = currentTime
                    calendar.add(Calendar.MINUTE, breakMinutes)
                    currentTime = calendar.time
                }
            } else {
                result.add(item.copy(time = timeString))
                val redMinutes = (item.redTime / 60).coerceAtLeast(0)
                if (redMinutes > 0) {
                    calendar.time = currentTime
                    calendar.add(Calendar.MINUTE, redMinutes)
                    currentTime = calendar.time
                }
            }
        }

        return result
    }
    
    /**
     * Recalculate times for all items starting from the given position
     * @param items List of agenda items
     * @param startPosition Position to start recalculating from (0 to recalculate all)
     * @param meetingStartTime The meeting start time to use for the first item (optional)
     * @return List of agenda items with recalculated times
     */
    fun recalculateTimesFromPosition(items: List<AgendaItemDto>, startPosition: Int = 0, meetingStartTime: String? = null): List<AgendaItemDto> {
        if (items.isEmpty() || startPosition < 0 || startPosition >= items.size) return items
        
        // Always perform a full recalculation to ensure consistency with session headers
        val startTime = meetingStartTime ?: items.firstOrNull()?.time ?: return items
        return calculateAgendaTimes(items, startTime)
    }
}
