package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.UserEntity
import com.bntsoft.toastmasters.data.model.dto.UserDto
import javax.inject.Inject

class UserMapper @Inject constructor() : Mapper<UserDto, UserEntity> {
    
    override fun mapFromEntity(entity: UserEntity): UserDto {
        return UserDto(
            id = entity.id,
            role = entity.role,
            name = entity.name,
            email = entity.email,
            number = entity.number,
            address = entity.address,
            level = entity.level,
            gender = entity.gender,
            isApproved = entity.isApproved,
            clubId = entity.clubId,
            toastmastersId = entity.toastmastersId,
            mentor = entity.mentor,
            mentorAssignedBy = entity.mentorAssignedBy,
            mentorAssignedDate = entity.mentorAssignedDate,
            password = entity.password,
            hasVpPermission = entity.hasVpPermission,
            createdAt = entity.createdAt
        )
    }

    override fun mapToEntity(dto: UserDto): UserEntity {
        return UserEntity(
            id = dto.id,
            role = dto.role,
            name = dto.name,
            email = dto.email,
            number = dto.number,
            address = dto.address,
            level = dto.level,
            gender = dto.gender,
            isApproved = dto.isApproved,
            clubId = dto.clubId,
            toastmastersId = dto.toastmastersId,
            mentor = dto.mentor,
            mentorAssignedBy = dto.mentorAssignedBy,
            mentorAssignedDate = dto.mentorAssignedDate,
            password = dto.password,
            hasVpPermission = dto.hasVpPermission,
            createdAt = dto.createdAt
        )
    }

    fun mapFromEntityList(entities: List<UserEntity>): List<UserDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<UserDto>): List<UserEntity> {
        return dtos.map { mapToEntity(it) }
    }
}
