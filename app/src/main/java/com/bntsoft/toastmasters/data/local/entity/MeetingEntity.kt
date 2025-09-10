package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.bntsoft.toastmasters.data.local.converters.MapConverter
import com.bntsoft.toastmasters.data.local.converters.MapStringConverter
import com.bntsoft.toastmasters.data.local.converters.MeetingStatusConverter
import com.bntsoft.toastmasters.data.local.converters.PreferredRoleConverter
import com.bntsoft.toastmasters.domain.models.MeetingStatus

@Entity(tableName = "meetings")
@TypeConverters(
    MeetingStatusConverter::class,
    MapConverter::class,
    MapStringConverter::class
)
data class MeetingEntity(
    @PrimaryKey val meetingID: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venue: String,
    val theme: String,
    @field:TypeConverters(MapConverter::class)
    val roleCounts: Map<String, Int>,
    @field:TypeConverters(MapConverter::class)
    val assignedCounts: Map<String, Int>,
    @field:TypeConverters(MapStringConverter::class)
    val assignedRoles: Map<String, String> = emptyMap(),
    @field:TypeConverters(MapStringConverter::class)
    val officers: Map<String, String> = emptyMap(),
    val createdAt: Long,
    val updatedAt: Long? = null,
    val status: MeetingStatus,
    val agendaId: String? = null,
    val isRecurring: Boolean = false,
    val createdBy: String? = null
)