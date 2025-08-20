package com.bntsoft.toastmasters.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val gender: String = "",
    val joinedDate: Date = Date(),
    val toastmastersId: String = "",
    val clubId: String = "",
    val profileImageUrl: String = "",
    val fcmToken: String = "",
    val mentorNames: List<String> = emptyList(),
    val roles: List<UserRole> = emptyList(),
    val status: Status = Status.PENDING_APPROVAL,
    val lastLogin: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) : Parcelable {

    enum class Status {
        PENDING_APPROVAL,
        APPROVED,
        REJECTED
    }

    // Helper properties
    val isApproved: Boolean
        get() = status == Status.APPROVED

    val isPendingApproval: Boolean
        get() = status == Status.PENDING_APPROVAL

    val isRejected: Boolean
        get() = status == Status.REJECTED


    val isVpEducation: Boolean
        get() = roles.contains(UserRole.VP_EDUCATION)

    val isOfficer: Boolean
        get() = roles.any { role ->
            role == UserRole.VP_EDUCATION || role == UserRole.MEMBER || role == UserRole.VP_EDUCATION_MEMBER

        }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "email" to email.lowercase(),
            "phoneNumber" to phoneNumber,
            "address" to address,
            "gender" to gender,
            "joinedDate" to joinedDate,
            "toastmastersId" to toastmastersId,
            "clubId" to clubId,
            "profileImageUrl" to profileImageUrl,
            "fcmToken" to fcmToken,
            "mentorNames" to mentorNames,
            "roles" to roles.map { it.name },
            "status" to status.name,
            "lastLogin" to lastLogin,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {

        fun fromMap(map: Map<String, Any>): User {
            return User(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                phoneNumber = map["phoneNumber"] as? String ?: "",
                address = map["address"] as? String ?: "",
                gender = map["gender"] as? String ?: "",
                joinedDate = (map["joinedDate"] as? com.google.firebase.Timestamp)?.toDate()
                    ?: Date(),
                toastmastersId = map["toastmastersId"] as? String ?: "",
                clubId = map["clubId"] as? String ?: "",
                profileImageUrl = map["profileImageUrl"] as? String ?: "",
                fcmToken = map["fcmToken"] as? String ?: "",
                mentorNames = (map["mentorNames"] as? List<*>)?.filterIsInstance<String>()
                    ?: emptyList(),
                roles = (map["roles"] as? List<*>)?.mapNotNull { role ->
                    try {
                        UserRole.valueOf(role.toString())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                } ?: emptyList(),
                status = try {
                    Status.valueOf((map["status"] as? String) ?: "PENDING_APPROVAL")
                } catch (e: IllegalArgumentException) {
                    Status.PENDING_APPROVAL
                },
                lastLogin = (map["lastLogin"] as? com.google.firebase.Timestamp)?.toDate(),
                createdAt = (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                    ?: Date(),
                updatedAt = (map["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
            )
        }
    }
}
