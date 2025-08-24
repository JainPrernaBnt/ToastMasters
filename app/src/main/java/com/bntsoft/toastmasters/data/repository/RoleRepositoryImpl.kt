package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.mapper.RoleMapper
import com.bntsoft.toastmasters.data.source.remote.RoleRemoteDataSource
import com.bntsoft.toastmasters.domain.model.role.AssignRoleRequest
import com.bntsoft.toastmasters.domain.model.role.MemberRole
import com.bntsoft.toastmasters.domain.model.role.MemberRolePreference
import com.bntsoft.toastmasters.domain.model.role.Role
import com.bntsoft.toastmasters.domain.model.role.RoleAssignmentResponse
import com.bntsoft.toastmasters.domain.repository.RoleRepository
import com.bntsoft.toastmasters.util.NetworkMonitor
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleRepositoryImpl @Inject constructor(
    private val remoteDataSource: RoleRemoteDataSource,
    private val mapper: RoleMapper,
    private val networkMonitor: NetworkMonitor
) : RoleRepository {

    override suspend fun getAllRoles(): Result<List<Role>> {
        return try {
            val isOnline = networkMonitor.isOnline.first()
            if (!isOnline) {
                return Result.Error(Exception("No internet connection"))
            }

            val roles = remoteDataSource.getAllRoles()
            Result.Success(roles.map { mapper.mapToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "Error getting all roles")
            Result.Error(e)
        }
    }

    override suspend fun getRoleById(roleId: String): Result<Role> {
        return try {
            val role = remoteDataSource.getRoleById(roleId)
                ?: return Result.Error(Exception("Role not found"))
            Result.Success(mapper.mapToDomain(role))
        } catch (e: Exception) {
            Timber.e(e, "Error getting role by id: $roleId")
            Result.Error(e)
        }
    }

    override suspend fun getRolesByIds(roleIds: List<String>): Result<List<Role>> {
        return try {
            val roles = remoteDataSource.getRolesByIds(roleIds)
            Result.Success(roles.map { mapper.mapToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "Error getting roles by ids: $roleIds")
            Result.Error(e)
        }
    }

    override suspend fun assignRole(request: AssignRoleRequest): Result<RoleAssignmentResponse> {
        return try {
            val isOnline = networkMonitor.isOnline.first()
            if (!isOnline) {
                return Result.Error(Exception("No internet connection"))
            }

            val response = remoteDataSource.assignRole(mapper.mapToDto(request))
            Result.Success(mapper.mapToDomain(response))
        } catch (e: Exception) {
            Timber.e(e, "Error assigning role: $request")
            Result.Error(e)
        }
    }

    override suspend fun getMemberRoles(memberId: String): Result<List<MemberRole>> {
        return try {
            val roles = remoteDataSource.getMemberRoles(memberId)
            Result.Success(roles.map { mapper.mapToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "Error getting member roles for: $memberId")
            Result.Error(e)
        }
    }

    override suspend fun getMeetingRoles(meetingId: String): Result<List<MemberRole>> {
        return try {
            val isOnline = networkMonitor.isOnline.first()
            if (!isOnline) {
                return Result.Error(Exception("No internet connection"))
            }
            val roles = remoteDataSource.getMeetingRoles(meetingId)
            Result.Success(roles.map { mapper.mapToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "Error getting meeting roles for: $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun getMeetingRoleAssignments(meetingId: String): Result<List<MemberRole>> {
        return try {
            val isOnline = networkMonitor.isOnline.first()
            if (!isOnline) {
                return Result.Error(Exception("No internet connection"))
            }
            val roles = remoteDataSource.getMeetingRoles(meetingId)
            Result.Success(roles.map { mapper.mapToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "Error getting meeting role assignments for: $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun getMemberRoleForMeeting(
        memberId: String,
        meetingId: String
    ): Result<MemberRole?> {
        return try {
            val role = remoteDataSource.getMemberRoleForMeeting(memberId, meetingId)
            Result.Success(role?.let { mapper.mapToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "Error getting member role for meeting: $memberId, $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun removeRoleAssignment(assignmentId: String): Result<Unit> {
        return try {
            remoteDataSource.removeRoleAssignment(assignmentId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error removing role assignment: $assignmentId")
            Result.Error(e)
        }
    }

    override suspend fun getMemberPreferences(memberId: String): Result<MemberRolePreference> {
        return try {
            val prefs = remoteDataSource.getMemberPreferences(memberId)
            Result.Success(mapper.mapToDomain(prefs))
        } catch (e: Exception) {
            Timber.e(e, "Error getting member preferences for: $memberId")
            Result.Error(e)
        }
    }

    override suspend fun updateMemberPreferences(
        memberId: String,
        preferredRoles: List<String>?,
        unavailableRoles: List<String>?
    ): Result<Unit> {
        return try {
            remoteDataSource.updateMemberPreferences(
                memberId = memberId,
                preferredRoles = preferredRoles,
                unavailableRoles = unavailableRoles
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating member preferences for: $memberId")
            Result.Error(e)
        }
    }

    override suspend fun getMemberRoleHistory(
        memberId: String,
        limit: Int
    ): Result<List<MemberRole>> {
        return try {
            val history = remoteDataSource.getMemberRoleHistory(memberId, limit)
            Result.Success(history.map { mapper.mapToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "Error getting role history for: $memberId")
            Result.Error(e)
        }
    }

    override suspend fun getRoleStatistics(roleId: String): Result<Map<String, Any>> {
        return try {
            val stats = remoteDataSource.getRoleStatistics(roleId)
            Result.Success(stats)
        } catch (e: Exception) {
            Timber.e(e, "Error getting statistics for role: $roleId")
            Result.Error(e)
        }
    }

    override suspend fun assignMultipleRoles(requests: List<AssignRoleRequest>): Result<List<RoleAssignmentResponse>> {
        return try {
            val responses = remoteDataSource.assignMultipleRoles(
                requests.map { mapper.mapToDto(it) }
            )
            Result.Success(responses.map { mapper.mapToDomain(it) })
        } catch (e: Exception) {
            Timber.e(e, "Error assigning multiple roles")
            Result.Error(e)
        }
    }

    override suspend fun getRoleTemplates(): Result<Map<String, List<String>>> {
        return try {
            val templates = remoteDataSource.getRoleTemplates()
            Result.Success(templates)
        } catch (e: Exception) {
            Timber.e(e, "Error getting role templates")
            Result.Error(e)
        }
    }

    override suspend fun applyRoleTemplate(meetingId: String, templateId: String): Result<Unit> {
        return try {
            remoteDataSource.applyRoleTemplate(meetingId, templateId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error applying role template: $templateId to meeting: $meetingId")
            Result.Error(e)
        }
    }
}
