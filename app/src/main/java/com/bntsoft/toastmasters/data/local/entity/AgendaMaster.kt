package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agenda_master",
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
            childColumns = ["createdBy"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["meetingId"]),
        Index(value = ["createdBy"])
    ]
)
data class AgendaMaster(

    @PrimaryKey(autoGenerate = true)
    val agendaId: Int = 0,
    val meetingId: Int,
    val createdBy: Int?,
    val createdAt: Long,
    val status: String,      // e.g., "draft", "completed", "cancelled"
    val meetingStartTime: String,
    val meetingEndTime: String
)
