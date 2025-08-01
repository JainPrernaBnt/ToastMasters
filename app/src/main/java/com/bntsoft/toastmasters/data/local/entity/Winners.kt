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
    val meetingId: Int,
    val category: String, // e.g., "Best Speaker", "Best Evaluator"
    val winnerUserId: Int?, // nullable if guest winner
    val isGuest: Boolean, // true if guest won
    val guestName: String?, // filled if guest winner
)

