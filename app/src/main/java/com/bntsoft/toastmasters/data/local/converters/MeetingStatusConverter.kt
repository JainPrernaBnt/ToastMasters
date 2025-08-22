package com.bntsoft.toastmasters.data.local.converters

import androidx.room.TypeConverter
import com.bntsoft.toastmasters.domain.models.MeetingStatus

class MeetingStatusConverter {
    @TypeConverter
    fun fromMeetingStatus(status: MeetingStatus): String {
        return status.name  // saves as "COMPLETED" or "NOT_COMPLETED"
    }

    @TypeConverter
    fun toMeetingStatus(value: String): MeetingStatus {
        return try {
            MeetingStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            MeetingStatus.NOT_COMPLETED // fallback if unknown
        }
    }
}