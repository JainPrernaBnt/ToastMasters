package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.bntsoft.toastmasters.data.local.converters.MeetingStatusConverter
import com.bntsoft.toastmasters.data.local.converters.PreferredRoleConverter
import com.bntsoft.toastmasters.domain.models.MeetingStatus

@Entity(tableName = "meetings")
@TypeConverters(PreferredRoleConverter::class, MeetingStatusConverter::class)
data class MeetingEntity(
    @PrimaryKey val meetingID: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venue: String,
    val theme: String,
    val preferredRoles: List<String>,
    val createdAt: Long,

    // New fields to handle recurring pattern
    val isRecurring: Boolean = true,
    val recurringDayOfWeek: Int? = null,
    val recurringStartTime: String? = null,
    val recurringEndTime: String? = null,
    val status: MeetingStatus
)