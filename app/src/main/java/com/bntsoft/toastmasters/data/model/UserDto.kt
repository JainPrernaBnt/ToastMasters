package com.bntsoft.toastmasters.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserDto(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val level: String? = null,
    val gender: String = "",
    val joinedDate: Date = Date(),
    val toastmastersId: String = "",
    val clubId: String = "",
    val role: String = "MEMBER", // "VP_EDUCATION" or "MEMBER"
    val isApproved: Boolean = false,
    val mentorNames: List<String> = emptyList(),
    val fcmToken: String? = null,
    val isRejected: Boolean = false,
    val rejectionReason: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)
