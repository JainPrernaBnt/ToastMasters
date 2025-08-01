package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "guests",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["meetingID"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["meetingId"])]
)
data class Guest(
    @PrimaryKey(autoGenerate = true)
    val guestId: Int = 0,
    val meetingId: Int,
    val name: String,
    val phone: String,
    val email: String,
    val playedRole: Boolean = false,
    val rolePlayed: String?,
    val gender: String
)

