package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.model.role.*
import com.bntsoft.toastmasters.domain.model.role.*
import javax.inject.Inject

class RoleMapper @Inject constructor() {
    
    // Domain to DTO mapping
    fun mapToDto(domain: Role): RoleDto = RoleDto(
        id = domain.id,
        name = domain.name,
        description = domain.description,
        isLeadership = domain.isLeadership,
        isSpeaker = domain.isSpeaker,
        isEvaluator = domain.isEvaluator
    )

    fun mapToDto(domain: MemberRole): MemberRoleDto = MemberRoleDto(
        id = domain.id,
        meetingId = domain.meetingId,
        memberId = domain.memberId,
        roleId = domain.roleId,
        assignedAt = domain.assignedAt,
        notes = domain.notes
    )

    fun mapToDto(domain: MemberRolePreference): MemberRolePreferenceDto = MemberRolePreferenceDto(
        memberId = domain.memberId,
        preferredRoles = domain.preferredRoles,
        unavailableRoles = domain.unavailableRoles,
        lastUpdated = domain.lastUpdated
    )

    fun mapToDto(domain: AssignRoleRequest): AssignRoleRequestDto = AssignRoleRequestDto(
        meetingId = domain.meetingId,
        memberId = domain.memberId,
        roleId = domain.roleId,
        notes = domain.notes
    )

    // DTO to Domain mapping
    fun mapToDomain(dto: RoleDto): Role = Role(
        id = dto.id,
        name = dto.name,
        description = dto.description,
        isLeadership = dto.isLeadership,
        isSpeaker = dto.isSpeaker,
        isEvaluator = dto.isEvaluator
    )

    fun mapToDomain(dto: MemberRoleDto): MemberRole = MemberRole(
        id = dto.id,
        meetingId = dto.meetingId,
        memberId = dto.memberId,
        roleId = dto.roleId,
        assignedAt = dto.assignedAt,
        notes = dto.notes
    )

    fun mapToDomain(dto: MemberRolePreferenceDto): MemberRolePreference = MemberRolePreference(
        memberId = dto.memberId,
        preferredRoles = dto.preferredRoles,
        unavailableRoles = dto.unavailableRoles,
        lastUpdated = dto.lastUpdated
    )

    fun mapToDomain(dto: RoleAssignmentResponseDto): RoleAssignmentResponse = RoleAssignmentResponse(
        success = dto.success,
        message = dto.message,
        assignment = dto.assignment?.let { mapToDomain(it) }
    )

    // List mappings
    fun mapToDomain(roles: List<RoleDto>): List<Role> = roles.map { mapToDomain(it) }
    fun mapToDomain(assignments: List<MemberRoleDto>): List<MemberRole> = assignments.map { mapToDomain(it) }
    fun mapToDomain(responses: List<RoleAssignmentResponseDto>): List<RoleAssignmentResponse> = 
        responses.map { mapToDomain(it) }
}
