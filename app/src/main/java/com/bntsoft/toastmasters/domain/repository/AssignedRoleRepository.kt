package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.data.model.MemberRole
import com.bntsoft.toastmasters.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AssignedRoleRepository {
    suspend fun getAssignedRoles(meetingId: String): Flow<Resource<List<MemberRole>>>
    
    suspend fun getAssignedRole(meetingId: String, roleId: String): Flow<Resource<MemberRole?>>
    
    suspend fun saveAssignedRole(meetingId: String, assignedRole: MemberRole): Resource<Unit>
    
    suspend fun deleteAssignedRole(meetingId: String, roleId: String): Resource<Unit>
    
    fun MemberRole.toDisplayStrings(): List<String> {
        return roles.map { "$memberName - $it" }
    }
}
