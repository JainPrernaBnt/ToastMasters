package com.bntsoft.toastmasters.data.model

import com.bntsoft.toastmasters.domain.models.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date
import com.bntsoft.toastmasters.domain.model.User as DomainUser

object UserDeserializer {
    fun fromDocument(document: DocumentSnapshot): DomainUser? {
        return try {
            val data = document.data ?: return null

            // Get the data model
            val user = User(
                id = document.id,
                name = data["name"] as? String ?: "",
                email = data["email"] as? String ?: "",
                phoneNumber = data["phoneNumber"] as? String ?: "",
                address = data["address"] as? String ?: "",
                gender = data["gender"] as? String ?: "",
                joinedDate = (data["joinedDate"] as? Timestamp)?.toDate() ?: Date(),
                toastmastersId = data["toastmastersId"] as? String ?: "",
                clubId = data["clubId"] as? String ?: "",
                profileImageUrl = data["profileImageUrl"] as? String ?: "",
                fcmToken = data["fcmToken"] as? String ?: "",
                mentorNames = (data["mentorNames"] as? List<*>)?.filterIsInstance<String>()
                    ?: emptyList(),
                // For backward compatibility, handle both 'role' and 'roles' fields
                // and both 'isApproved' and 'status' fields
                roles = when {
                    data["role"] != null -> listOf(UserRole.valueOf(data["role"].toString()))
                    data["roles"] is List<*> -> (data["roles"] as List<*>).mapNotNull {
                        try {
                            UserRole.valueOf(it.toString())
                        } catch (e: Exception) {
                            null
                        }
                    }

                    else -> emptyList()
                },
                status = when {
                    data["isApproved"] == true -> User.Status.APPROVED
                    data["status"] != null -> {
                        try {
                            User.Status.valueOf((data["status"] as String))
                        } catch (e: Exception) {
                            User.Status.PENDING_APPROVAL
                        }
                    }

                    else -> User.Status.PENDING_APPROVAL
                },
                lastLogin = (data["lastLogin"] as? Timestamp)?.toDate(),
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
            )

            // Convert to domain model
            val role = user.roles.firstOrNull() ?: (data["role"]?.let {
                try {
                    UserRole.valueOf(it.toString())
                } catch (e: Exception) {
                    UserRole.MEMBER
                }
            } ?: UserRole.MEMBER)

            val isApproved = when {
                data["isApproved"] == true -> true
                data["status"]?.toString()?.equals("APPROVED", ignoreCase = true) == true -> true
                user.isApproved -> true
                else -> false
            }

            DomainUser(
                id = user.id,
                name = user.name,
                email = user.email,
                phoneNumber = user.phoneNumber,
                address = user.address,
                level = data["level"] as? String,
                gender = user.gender,
                joinedDate = user.joinedDate,
                toastmastersId = user.toastmastersId,
                clubId = user.clubId,
                fcmToken = user.fcmToken,
                mentorNames = user.mentorNames,
                role = role,
                isApproved = isApproved,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
