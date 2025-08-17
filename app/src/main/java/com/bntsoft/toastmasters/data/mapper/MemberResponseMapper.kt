package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MemberResponseEntity
import com.bntsoft.toastmasters.data.remote.dto.MemberResponseDto
import com.bntsoft.toastmasters.domain.model.MemberResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberResponseMapper @Inject constructor() {

    fun mapToEntity(domain: MemberResponse): MemberResponseEntity {
        return MemberResponseEntity.fromDomain(domain)
    }

    fun mapToDomain(entity: MemberResponseEntity): MemberResponse {
        return entity.toDomain()
    }

    fun mapToDomain(dto: MemberResponseDto): MemberResponse {
        return MemberResponse(
            id = dto.id,
            meetingId = dto.meetingId,
            memberId = dto.memberId,
            availability = MemberResponse.AvailabilityStatus.fromString(dto.availability),
            preferredRoles = dto.preferredRoles,
            notes = dto.notes,
            lastUpdated = dto.lastUpdated
        )
    }

    fun mapToDto(domain: MemberResponse): MemberResponseDto {
        return MemberResponseDto(
            id = domain.id,
            meetingId = domain.meetingId,
            memberId = domain.memberId,
            availability = domain.availability.name,
            preferredRoles = domain.preferredRoles,
            notes = domain.notes,
            lastUpdated = domain.lastUpdated
        )
    }

    fun mapEntityToDto(entity: MemberResponseEntity): MemberResponseDto {
        return MemberResponseDto(
            id = entity.id,
            meetingId = entity.meetingId,
            memberId = entity.memberId,
            availability = entity.availability,
            preferredRoles = entity.preferredRoles.split(",").filter { it.isNotBlank() },
            notes = entity.notes,
            lastUpdated = entity.lastUpdated
        )
    }

    fun mapDtoToEntity(dto: MemberResponseDto): MemberResponseEntity {
        return MemberResponseEntity(
            id = dto.id,
            meetingId = dto.meetingId,
            memberId = dto.memberId,
            availability = dto.availability,
            preferredRoles = dto.preferredRoles.joinToString(","),
            notes = dto.notes,
            lastUpdated = dto.lastUpdated
        )
    }
}
