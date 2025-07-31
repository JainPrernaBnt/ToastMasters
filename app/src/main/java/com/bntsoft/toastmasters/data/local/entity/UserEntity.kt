package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class UserEntity(
    @PrimaryKey val id: Int,
    val role: String,
    val name: String,
    val email: String,
    val number: Int,
    val address: String,
    val level: Int,
    val gender: String,
    val isApproved: Boolean,
    val clubId: Int,
    val toastmastersId: Int,
    val mentor: String,
    val mentorAssignedBy: String?,
    val mentorAssignedDate: Long?
)