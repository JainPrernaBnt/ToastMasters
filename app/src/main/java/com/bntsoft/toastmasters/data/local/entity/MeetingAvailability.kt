package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meeting_availability",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["meetingID"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["meetingId"]),
        Index(value = ["userId"])
    ]
)
data class MeetingAvailability(
    @PrimaryKey(autoGenerate = true)
    val availabilityId: Int = 0,
    val meetingId: Int,
    val userId: Int,
    val preferredRole: String,
    val available: String,     // "yes", "no", "not sure"
    val submittedOn: String    // e.g., "2025-07-30 14:50"
)
