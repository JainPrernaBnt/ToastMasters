package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MeetingAvailability
import com.bntsoft.toastmasters.data.model.dto.MeetingAvailabilityDto
import javax.inject.Inject

class MeetingAvailabilityMapper @Inject constructor() :
    Mapper<MeetingAvailabilityDto, MeetingAvailability> {

    override fun mapFromEntity(entity: MeetingAvailability): MeetingAvailabilityDto {
        return MeetingAvailabilityDto(
            availabilityId = entity.availabilityId,
            meetingId = entity.meetingId,
            userId = entity.userId,
            preferredRole = entity.preferredRole,
            available = entity.available,
            submittedOn = entity.submittedOn
        )
    }

    override fun mapToEntity(dto: MeetingAvailabilityDto): MeetingAvailability {
        return MeetingAvailability(
            availabilityId = dto.availabilityId,
            meetingId = dto.meetingId,
            userId = dto.userId,
            preferredRole = dto.preferredRole,
            available = dto.available,
            submittedOn = dto.submittedOn
        )
    }

    fun mapFromEntityList(entities: List<MeetingAvailability>): List<MeetingAvailabilityDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<MeetingAvailabilityDto>): List<MeetingAvailability> {
        return dtos.map { mapToEntity(it) }
    }
}
