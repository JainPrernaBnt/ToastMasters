package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.model.dto.MeetingAgendaDto
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingAgendaMapper @Inject constructor() : Mapper<MeetingAgendaDto, MeetingAgenda> {

    override fun mapFromEntity(entity: MeetingAgenda): MeetingAgendaDto {
        return MeetingAgendaDto(
            id = entity.id,
            meetingId = entity.meeting.id,
            meetingTitle = entity.meeting.theme,
            meetingDate = entity.meetingDate,
            startTime = entity.startTime,
            endTime = entity.endTime,
            meetingTheme = entity.meeting.theme,
            venue = entity.meeting.location,
            createdBy = entity.meeting.createdBy,
            officers = entity.officers,
            agendaStatus = entity.agendaStatus.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun mapToEntity(dto: MeetingAgendaDto): MeetingAgenda {
        return MeetingAgenda(
            id = dto.id,
            meeting = com.bntsoft.toastmasters.domain.model.Meeting(
                id = dto.meetingId,
                dateTime = dto.meetingDate?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDateTime()
                    ?: java.time.LocalDateTime.now(),
                location = dto.venue,
                theme = dto.meetingTheme,
                createdBy = dto.createdBy,
                roleCounts = emptyMap(),
                assignedRoles = emptyMap(),
                assignedCounts = emptyMap(),
                isRecurring = false,
                isEditMode = false,
                status = MeetingStatus.NOT_COMPLETED,
                endDateTime = null,
                updatedAt = dto.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                createdAt = dto.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                availability = null
            ),
            meetingDate = dto.meetingDate,
            startTime = dto.startTime,
            endTime = dto.endTime,
            officers = dto.officers,
            agendaStatus = AgendaStatus.valueOf(dto.agendaStatus),
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun mapFromEntityList(entities: List<MeetingAgenda>): List<MeetingAgendaDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<MeetingAgendaDto>): List<MeetingAgenda> {
        return dtos.map { mapToEntity(it) }
    }
}
