package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.MeetingRoles
import com.bntsoft.toastmasters.data.model.dto.MeetingRolesDto
import javax.inject.Inject

class MeetingRolesMapper @Inject constructor() : Mapper<MeetingRolesDto, MeetingRoles> {

    override fun mapFromEntity(entity: MeetingRoles): MeetingRolesDto {
        return MeetingRolesDto(
            roleId = entity.roleId,
            meetingId = entity.meetingId,
            userId = entity.userId,
            assignedRole = entity.assignedRole,
            isBackupMember = entity.isBackupMember,
            isCancelled = entity.isCancelled,
            assignedAt = entity.assignedAt,
            cancelledAt = entity.cancelledAt
        )
    }

    override fun mapToEntity(dto: MeetingRolesDto): MeetingRoles {
        return MeetingRoles(
            roleId = dto.roleId,
            meetingId = dto.meetingId,
            userId = dto.userId,
            assignedRole = dto.assignedRole,
            isBackupMember = dto.isBackupMember,
            isCancelled = dto.isCancelled,
            assignedAt = dto.assignedAt,
            cancelledAt = dto.cancelledAt
        )
    }

    fun mapFromEntityList(entities: List<MeetingRoles>): List<MeetingRolesDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<MeetingRolesDto>): List<MeetingRoles> {
        return dtos.map { mapToEntity(it) }
    }
}
