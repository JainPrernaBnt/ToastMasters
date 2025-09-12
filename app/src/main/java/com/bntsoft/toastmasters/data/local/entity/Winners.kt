package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "winners",
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
            childColumns = ["winnerUserId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["meetingId"]),
        Index(value = ["winnerUserId"])
    ]
)
data class Winners(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val meetingId: String,
    val category: String,
    val isMember: Boolean = true,            // True if winner is a member, false if guest
    val winnerUserId: Int? = null,           // Member ID (nullable for guest)
    val memberName: String? = null,          // Member name
    val guestName: String? = null            // Guest name (filled if isMember = false)
)

