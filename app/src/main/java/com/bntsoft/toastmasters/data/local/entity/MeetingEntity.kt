package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey val meetingID: Int,
    val title: String,
    val date: String,
    val time: String,
    val venue: String,
    val theme: String,
    val preferredRoles: List<String>,
    val createdAt: Long
)