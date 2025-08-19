package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.model.UserDto
import com.bntsoft.toastmasters.data.model.UserRole
import com.bntsoft.toastmasters.domain.model.User
import java.util.*
import javax.inject.Inject

class UserDomainMapper @Inject constructor() {

    fun mapToDomain(dto: UserDto): User {
        return User(
            id = dto.id,
            name = dto.name,
            email = dto.email,
            phoneNumber = dto.phoneNumber,
            address = dto.address,
            level = dto.level,
            gender = dto.gender,
            joinedDate = dto.joinedDate,
            toastmastersId = dto.toastmastersId,
            clubId = dto.clubId,
            role = when (dto.role.uppercase()) {
                "VP_EDUCATION" -> UserRole.VP_EDUCATION
                else -> UserRole.MEMBER
            },
            isApproved = dto.isApproved,
            isNewMember = dto.isNewMember,
            mentorNames = dto.mentorNames,
            fcmToken = dto.fcmToken,
            createdAt = dto.createdAt ?: Date(),
            updatedAt = dto.updatedAt ?: Date()
        )
    }

    fun mapFromDomain(domain: User): UserDto {
        return UserDto(
            id = domain.id,
            name = domain.name,
            email = domain.email,
            phoneNumber = domain.phoneNumber,
            address = domain.address,
            level = domain.level,
            gender = domain.gender,
            joinedDate = domain.joinedDate,
            toastmastersId = domain.toastmastersId,
            clubId = domain.clubId,
            role = when (domain.role) {
                UserRole.VP_EDUCATION -> "VP_EDUCATION"
                else -> "MEMBER"
            },
            isApproved = domain.isApproved,
            isNewMember = domain.isNewMember,
            mentorNames = domain.mentorNames,
            fcmToken = domain.fcmToken,
            isRejected = false, // Default value, adjust as needed
            rejectionReason = null,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
