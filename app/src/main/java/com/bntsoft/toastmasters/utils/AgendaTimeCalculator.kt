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
        
        try {
            // Parse the meeting start time
            calendar.time = timeFormat.parse(meetingStartTime) ?: return items
        } catch (e: Exception) {
            return items
        }
        
        for (index in items.indices) {
            val item = items[index]
            if (index == 0) {
                // First item uses the meeting start time
                result.add(item.copy(time = meetingStartTime))
            } else {
                // Calculate time based on the PREVIOUS RECALCULATED item's time and red card
                val prevItem = result[index - 1] // Use the recalculated item from result
                val prevTime = timeFormat.parse(prevItem.time) ?: continue
                
                calendar.time = prevTime
                // Convert red time from seconds to minutes
                calendar.add(Calendar.MINUTE, prevItem.redTime / 60)
                
                result.add(item.copy(time = timeFormat.format(calendar.time)))
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
        
        // If starting from position 0, perform a FULL recalculation from meeting start time
        if (startPosition == 0) {
            // Use the provided meeting start time, or fall back to the existing time of the first item
            val startTime = meetingStartTime ?: items.firstOrNull()?.time ?: return items
            return calculateAgendaTimes(items, startTime)
        }
        
        // For other positions, we still need to ensure we're using recalculated times
        // So we'll perform a full recalculation from position 0 to ensure consistency
        val startTime = meetingStartTime ?: items.firstOrNull()?.time ?: return items
        return calculateAgendaTimes(items, startTime)
    }
}
