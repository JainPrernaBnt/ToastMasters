package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.bntsoft.toastmasters.data.local.converters.MapConverter
import com.bntsoft.toastmasters.data.local.converters.MeetingStatusConverter
import com.bntsoft.toastmasters.data.local.converters.PreferredRoleConverter
import com.bntsoft.toastmasters.domain.models.MeetingStatus

@Entity(tableName = "meetings")
@TypeConverters(MeetingStatusConverter::class, MapConverter::class)
data class MeetingEntity(
    @PrimaryKey val meetingID: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venue: String,
    val theme: String,
    val roleCounts: Map<String, Int>,
    val assignedCounts: Map<String, Int>,
    val createdAt: Long,
    val status: MeetingStatus
)