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
        
        val calendar = Calendar.getInstance()
        try {
            // Parse the meeting start time
            calendar.time = timeFormat.parse(meetingStartTime) ?: return items
        } catch (e: Exception) {
            return items
        }
        
        return items.mapIndexed { index, item ->
            if (index == 0) {
                // First item uses the meeting start time
                item.copy(time = meetingStartTime)
            } else {
                // Calculate time based on previous item's time and red card
                val prevItem = items[index - 1]
                val prevTime = timeFormat.parse(prevItem.time) ?: return@mapIndexed item
                
                calendar.time = prevTime
                calendar.add(Calendar.MINUTE, prevItem.redTime)
                
                item.copy(time = timeFormat.format(calendar.time))
            }
        }
    }
    
    /**
     * Recalculate times for all items starting from the given position
     * @param items List of agenda items
     * @param startPosition Position to start recalculating from (0 to recalculate all)
     * @return List of agenda items with recalculated times
     */
    fun recalculateTimesFromPosition(items: List<AgendaItemDto>, startPosition: Int = 0): List<AgendaItemDto> {
        if (items.isEmpty() || startPosition < 0 || startPosition >= items.size) return items
        
        val result = items.toMutableList()
        
        // If starting from position 0, we need the meeting start time
        if (startPosition == 0) {
            // Use the existing time of the first item as the meeting start time
            val meetingStartTime = items.firstOrNull()?.time ?: return items
            return calculateAgendaTimes(items, meetingStartTime)
        }
        
        // For other positions, calculate based on the previous item
        for (i in startPosition until items.size) {
            val prevItem = result[i - 1]
            val prevTime = timeFormat.parse(prevItem.time) ?: continue
            
            val calendar = Calendar.getInstance()
            calendar.time = prevTime
            calendar.add(Calendar.MINUTE, prevItem.redTime)
            
            result[i] = result[i].copy(time = timeFormat.format(calendar.time))
        }
        
        return result
    }
}
