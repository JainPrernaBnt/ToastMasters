package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meeting_roles_backout",
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
            childColumns = ["originalUserId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["backupUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["meetingId"]),
        Index(value = ["originalUserId"]),
        Index(value = ["backupUserId"])
    ]
)
data class MeetingRolesBackout(
    @PrimaryKey(autoGenerate = true)
    val backoutRoleId: Int = 0,
    val meetingId: Int,
    val originalUserId: Int,
    val roleName: String,
    val backupUserId: Int?,
    val reassignedAt: Long?,
    val reassignedBy: String?
)
