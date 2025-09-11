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
        
        var lastValidTime = calendar.time
        
        for (index in items.indices) {
            val item = items[index]
            
            if (item.isSessionHeader) {
                // For session headers, use the last valid time
                result.add(item.copy(time = timeFormat.format(lastValidTime)))
                continue
            }
            
            if (index == 0) {
                // First non-header item uses the meeting start time
                result.add(item.copy(time = meetingStartTime))
                lastValidTime = timeFormat.parse(meetingStartTime) ?: continue
            } else {
                // Calculate time based on the last valid item's time and red card
                val prevItem = result[index - 1] // Use the recalculated item from result
                calendar.time = lastValidTime
                // Convert red time from seconds to minutes
                calendar.add(Calendar.MINUTE, prevItem.redTime / 60)
                
                val newTime = calendar.time
                result.add(item.copy(time = timeFormat.format(newTime)))
                lastValidTime = newTime
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
