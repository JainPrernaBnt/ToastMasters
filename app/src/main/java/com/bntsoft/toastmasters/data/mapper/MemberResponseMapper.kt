package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MemberResponseEntity
import com.bntsoft.toastmasters.data.remote.dto.MemberResponseDto
import com.bntsoft.toastmasters.domain.model.MemberResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberResponseMapper @Inject constructor() : Mapper<MemberResponseDto, MemberResponse> {

    fun map(input: MemberResponseDto): MemberResponse {
        return MemberResponse(
            id = input.id,
            meetingId = input.meetingId,
            memberId = input.memberId,
            availability = MemberResponse.AvailabilityStatus.valueOf(input.availability),
            preferredRoles = input.preferredRoles,
            notes = input.notes,
            lastUpdated = input.lastUpdated
        )
    }

    fun toEntity(domain: MemberResponse): MemberResponseEntity {
        return MemberResponseEntity.fromDomain(domain)
    }

    fun toDomain(entity: MemberResponseEntity): MemberResponse {
        return entity.toDomain()
    }

    fun toDto(domain: MemberResponse): MemberResponseDto {
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

    fun toEntity(dto: MemberResponseDto): MemberResponseEntity {
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

    fun toDto(entity: MemberResponseEntity): MemberResponseDto {
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

    override fun mapFromEntity(entity: MemberResponse): MemberResponseDto {
        return toDto(entity)
    }

    override fun mapToEntity(dto: MemberResponseDto): MemberResponse {
        return map(dto)
    }
}
