package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

import androidx.room.*

@Entity(
    tableName = "assigned_roles",
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
        Index(value = ["userId"]),
        Index(value = ["assignedRole"])
    ]
)
data class MeetingRoles(
    @PrimaryKey(autoGenerate = true)
    val roleId: Int = 0,
    val meetingId: Int,
    val userId: Int,
    val assignedRole: String,
    val isBackupMember: Boolean,
    val isCancelled: Boolean = false,
    val assignedAt: Long,          // Timestamp when assigned
    val cancelledAt: Long? = null  // Timestamp when cancelled (nullable)
)

