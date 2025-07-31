package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MeetingRolesBackout
import com.bntsoft.toastmasters.data.model.dto.MeetingRolesBackoutDto
import javax.inject.Inject

class MeetingRolesBackoutMapper @Inject constructor() : Mapper<MeetingRolesBackoutDto, MeetingRolesBackout> {

    override fun mapFromEntity(entity: MeetingRolesBackout): MeetingRolesBackoutDto {
        return MeetingRolesBackoutDto(
            backoutRoleId = entity.backoutRoleId,
            meetingId = entity.meetingId,
            originalUserId = entity.originalUserId,
            roleName = entity.roleName,
            backupUserId = entity.backupUserId,
            reassignedAt = entity.reassignedAt,
            reassignedBy = entity.reassignedBy
        )
    }

    override fun mapToEntity(dto: MeetingRolesBackoutDto): MeetingRolesBackout {
        return MeetingRolesBackout(
            backoutRoleId = dto.backoutRoleId,
            meetingId = dto.meetingId,
            originalUserId = dto.originalUserId,
            roleName = dto.roleName,
            backupUserId = dto.backupUserId,
            reassignedAt = dto.reassignedAt,
            reassignedBy = dto.reassignedBy
        )
    }

    fun mapFromEntityList(entities: List<MeetingRolesBackout>): List<MeetingRolesBackoutDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<MeetingRolesBackoutDto>): List<MeetingRolesBackout> {
        return dtos.map { mapToEntity(it) }
    }
}
