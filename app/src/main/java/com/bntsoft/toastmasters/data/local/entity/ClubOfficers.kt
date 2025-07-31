package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "club_officers",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["meetingID"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.SET_NULL
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
data class ClubOfficers(
    @PrimaryKey(autoGenerate = true)
    val officersId: Int = 0,
    val meetingId: Int?,
    val userId: Int,
    val role: String,            // "President", "VP Education", etc.
    val termStart: String,
    val termEnd: String
)

