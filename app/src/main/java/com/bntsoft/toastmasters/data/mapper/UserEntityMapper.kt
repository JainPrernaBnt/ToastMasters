package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.UserEntity
import com.bntsoft.toastmasters.data.model.UserRole
import com.bntsoft.toastmasters.domain.model.User
import java.util.*
import javax.inject.Inject

class UserEntityMapper @Inject constructor() {

    fun mapToDomain(entity: UserEntity): User {
        return User(
            id = entity.id.toString(),
            name = entity.name,
            email = entity.email,
            phoneNumber = entity.number.toString(),
            address = entity.address,
            level = entity.level.toString(),
            gender = entity.gender,
            joinedDate = Date(entity.createdAt), // Using createdAt as joinedDate
            toastmastersId = entity.toastmastersId.toString(),
            clubId = entity.clubId.toString(),
            role = if (entity.role.equals("VP_EDUCATION", ignoreCase = true)) {
                UserRole.VP_EDUCATION
            } else {
                UserRole.MEMBER
            },
            isApproved = entity.isApproved,
            isNewMember = entity.mentorAssignedBy == null, // Assuming new if no mentor assigned
            mentorIds = if (entity.mentor.isNotBlank()) {
                listOf(entity.mentor)
            } else {
                emptyList()
            },
            fcmToken = null, // Not stored in entity
            createdAt = Date(entity.createdAt),
            updatedAt = Date(entity.mentorAssignedDate ?: entity.createdAt)
        )
    }

    fun mapFromDomain(domain: User): UserEntity {
        return UserEntity(
            id = domain.id.toIntOrNull() ?: 0,
            role = domain.role.name,
            name = domain.name,
            email = domain.email,
            number = domain.phoneNumber.toIntOrNull() ?: 0,
            password = "", // Password is managed by Firebase Auth
            address = domain.address,
            level = domain.level?.toIntOrNull() ?: 0,
            gender = domain.gender,
            isApproved = domain.isApproved,
            hasVpPermission = domain.role == UserRole.VP_EDUCATION,
            clubId = domain.clubId.toIntOrNull() ?: 0,
            toastmastersId = domain.toastmastersId.toIntOrNull() ?: 0,
            mentor = domain.mentorIds.firstOrNull() ?: "",
            mentorAssignedBy = if (domain.mentorIds.isNotEmpty()) "system" else null,
            mentorAssignedDate = if (domain.mentorIds.isNotEmpty()) {
                domain.updatedAt.time
            } else {
                null
            },
            createdAt = domain.createdAt.time
        )
    }
}
