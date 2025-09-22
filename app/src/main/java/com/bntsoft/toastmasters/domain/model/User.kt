package com.bntsoft.toastmasters.domain.model

import com.bntsoft.toastmasters.domain.models.UserRole
import java.util.Date

data class User(
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
    val password: String = "",
    val role: UserRole = UserRole.MEMBER,
    val isApproved: Boolean = false,
    val mentorNames: List<String> = emptyList(),
    val fcmToken: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val isVpEducation: Boolean
        get() = role == UserRole.VP_EDUCATION
        
    // Device management fields
    val deviceId: String? = null
    val lastLogin: Long = System.currentTimeMillis()
}

