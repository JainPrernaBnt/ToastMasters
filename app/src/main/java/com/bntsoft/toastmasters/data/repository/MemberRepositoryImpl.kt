package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.model.role.MemberRole
import com.bntsoft.toastmasters.domain.model.role.Role
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.domain.repository.RoleRepository
import com.bntsoft.toastmasters.utils.NotificationHelper
import com.bntsoft.toastmasters.utils.Result
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val notificationRepository: NotificationRepository,
    private val roleRepository: RoleRepository
) : MemberRepository {

    override fun getPendingApprovals(): Flow<List<User>> {
        return firestoreService.getPendingApprovals().map { documents ->
            documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
        }
    }

    override suspend fun approveMember(userId: String, mentorNames: List<String>): Boolean {
        return try {
            // First update mentor names if provided
            if (mentorNames.isNotEmpty()) {
                val updates = mapOf(
                    "mentorNames" to mentorNames,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                firestoreService.getUserDocument(userId).update(updates).await()
            }

            // Then update approval status
            var updateSuccess = firestoreService.approveMember(userId, mentorNames)

            if (updateSuccess) {
                // Double-check the approval status was actually updated
                val userDoc = firestoreService.getUserDocument(userId).get().await()
                val isApproved =
                    userDoc.getBoolean("isApproved") ?: userDoc.getBoolean("approved") ?: false

                if (!isApproved) {
                    // If not approved, try a direct update
                    val updates = mapOf(
                        "isApproved" to true,
                        "approved" to true,
                        "mentorNames" to mentorNames,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    firestoreService.getUserDocument(userId).update(updates).await()

                    // Verify again
                    val updatedDoc = firestoreService.getUserDocument(userId).get().await()
                    updateSuccess =
                        updatedDoc.getBoolean("isApproved") ?: updatedDoc.getBoolean("approved")
                                ?: false
                }

                if (updateSuccess) {
                    // Send notification only if update was successful
                    val notification = NotificationData(
                        title = "Membership approved",
                        message = "Your account has been approved. You can now sign in and use the app.",
                        type = NotificationHelper.TYPE_MEMBER_APPROVAL,
                        data = mapOf(NotificationHelper.EXTRA_USER_ID to userId)
                    )
                    try {
                        notificationRepository.sendNotificationToUser(userId, notification)
                    } catch (e: Exception) {
                        // Log error but don't fail the operation
                        e.printStackTrace()
                    }
                }
            }

            updateSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun rejectMember(userId: String, reason: String?): Boolean {
        val ok = firestoreService.rejectMember(userId, reason)
        if (ok) {
            val msg = if (!reason.isNullOrBlank()) {
                "Your account has been rejected. Reason: $reason"
            } else {
                "Your account has been rejected. Please contact club officers."
            }
            val notification = NotificationData(
                title = "Membership rejected",
                message = msg,
                type = NotificationHelper.TYPE_MEMBER_APPROVAL,
                data = mapOf(NotificationHelper.EXTRA_USER_ID to userId)
            )
            try {
                notificationRepository.sendNotificationToUser(userId, notification)
            } catch (_: Exception) {
            }
        }
        return ok
    }

    override suspend fun updateMember(user: User): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "mentorNames" to user.mentorNames,
                "role" to user.role.toString(),
                "vpEducation" to (user.role == com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestoreService.getUserDocument(user.id).update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getAllMembers(includePending: Boolean): Flow<List<User>> = flow {
        if (includePending) {
            // Get all users including pending approvals
            val pendingUsers = firestoreService.getPendingApprovals()
                .map { documents ->
                    documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                }
                .catch { e ->
                    e.printStackTrace()
                    emit(emptyList())
                }

            val approvedUsers = firestoreService.getApprovedMembers()
                .map { documents ->
                    documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                }
                .catch { e ->
                    e.printStackTrace()
                    emit(emptyList())
                }

            val combined = combine(pendingUsers, approvedUsers) { pending, approved ->
                (pending + approved).distinctBy { it.id }
            }

            combined.collect { users ->
                emit(users)
            }
        } else {
            // Get only approved users (both members and mentors)
            firestoreService.getApprovedMembers()
                .map { documents ->
                    documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                }
                .catch { e ->
                    e.printStackTrace()
                    emit(emptyList())
                }
                .collect { users ->
                    emit(users)
                }
        }
    }

    override fun getMentors(): Flow<List<User>> {
        return firestoreService.getMentors().map { documents ->
            documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
        }
    }

    override suspend fun getMemberById(userId: String): User? {
        return try {
            val userDoc = firestoreService.getUserDocument(userId).get().await()
            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java)?.copy(id = userId)
                // Ensure role is properly set from vpEducation field if needed
                user?.let { u ->
                    val vpEducation = userDoc.getBoolean("vpEducation") ?: false
                    if (vpEducation && u.role != com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION) {
                        u.copy(role = com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION)
                    } else {
                        u
                    }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getMemberByEmail(email: String): User? {
        return try {
            val querySnapshot = firestoreService.getUserByEmail(email)
            val firstDoc = querySnapshot.documents.firstOrNull() ?: return null

            val user = firstDoc.toObject(User::class.java)?.copy(id = firstDoc.id) ?: return null

            // Ensure role is properly set from vpEducation field if needed
            val vpEducation = firstDoc.getBoolean("vpEducation") ?: false
            if (vpEducation && user.role != com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION) {
                user.copy(role = com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION)
            } else {
                user
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getMemberByPhone(phoneNumber: String): User? {
        return try {
            val querySnapshot = firestoreService.getUserByPhone(phoneNumber)
            val firstDoc = querySnapshot.documents.firstOrNull() ?: return null

            val user = firstDoc.toObject(User::class.java)?.copy(id = firstDoc.id) ?: return null

            // Ensure role is properly set from vpEducation field if needed
            val vpEducation = firstDoc.getBoolean("vpEducation") ?: false
            if (vpEducation && user.role != com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION) {
                user.copy(role = com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION)
            } else {
                user
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun observeMember(userId: String): Flow<User?> {
        return firestoreService.observeUser(userId)
            .map { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)?.copy(id = document.id)
                    // Ensure role is properly set from vpEducation field if needed
                    user?.let { u ->
                        val vpEducation = document.getBoolean("vpEducation") ?: false
                        if (vpEducation && u.role != com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION) {
                            u.copy(role = com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION)
                        } else {
                            u
                        }
                    }
                } else {
                    null
                }
            }
            .catch { e ->
                Timber.e(e, "Error observing user: $userId")
                emit(null)
            }
    }

    override suspend fun getMemberRoles(memberId: String): Result<List<MemberRole>> {
        return try {
            val result = roleRepository.getMemberRoles(memberId)
            when (result) {
                is Result.Success<List<MemberRole>> -> result
                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected loading state while getting member roles"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(e)
        }
    }

    override suspend fun getMemberRoleHistory(
        memberId: String,
        limit: Int
    ): Result<List<MemberRole>> {
        return try {
            val result = roleRepository.getMemberRoleHistory(memberId, limit)
            when (result) {
                is Result.Success<*> -> {
                    val roles = (result.data as? List<*>)?.filterIsInstance<MemberRole>()
                        ?: return Result.Error(Exception("Invalid data format"))
                    Result.Success(roles)
                }

                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected loading state while getting member role history"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(e)
        }
    }

    override suspend fun getAvailableMembersForRole(
        roleId: String,
        meetingId: String
    ): Result<List<User>> {
        return try {
            // 1. Get all members from the repository
            val allMembers = getAllMembers(includePending = false).first()

            // 2. Get meeting role assignments
            val assignedMembers = roleRepository.getMeetingRoleAssignments(meetingId)
            val assignedMemberIds = when (assignedMembers) {
                is Result.Success<*> -> {
                    (assignedMembers.data as? List<*>)?.filterIsInstance<MemberRole>()
                        ?.map { it.memberId }?.toSet() ?: emptySet()
                }
                is Result.Error -> {
                    Timber.e(assignedMembers.exception, "Error getting meeting role assignments")
                    return assignedMembers
                }
                else -> emptySet()
            }

            // 3. Filter available members
            val availableMembers = allMembers
                .filter { member -> member.id !in assignedMemberIds }
                .filter { member ->
                    isMemberAvailableForRole(member.id, roleId, meetingId)
                }

            Result.Success(availableMembers)
        } catch (e: Exception) {
            Timber.e(e, "Error getting available members for role: $roleId")
            Result.Error(e)
        }
    }

    override suspend fun getMemberAvailabilityForRole(
        memberId: String,
        roleId: String,
        meetingId: String
    ): Result<Boolean> {
        return try {
            // Check if member is already assigned to any role in this meeting
            val assignedRoles = roleRepository.getMemberRoleForMeeting(memberId, meetingId)
            val isAssigned = when (assignedRoles) {
                is Result.Success<*> -> assignedRoles.data != null
                else -> false
            }

            if (isAssigned) {
                return Result.Success(false)
            }

            // Check member's role preferences
            val preferences = getMemberRolePreferences(memberId)
            val hasPreference = when (preferences) {
                is Result.Success -> roleId in preferences.data
                else -> false
            }

            Result.Success(hasPreference)
        } catch (e: Exception) {
            Timber.e(e, "Error checking member availability for role")
            Result.Error(e)
        }
    }

    override suspend fun getMemberRolePreferences(memberId: String): Result<Set<String>> {
        return try {
            val result = firestoreService.getMemberPreferences(memberId)
            when (result) {
                is Result.Success<*> -> {
                    val prefs = (result.data as? Map<*, *>)?.get("preferredRoles") as? List<*>
                    val roleIds = prefs?.filterIsInstance<String>()?.toSet() ?: emptySet()
                    Result.Success(roleIds)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected loading state while getting role preferences"))
                else -> Result.Error(Exception("Unknown result type"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting role preferences for member: $memberId")
            Result.Error(e)
        }
    }

    override suspend fun updateMemberRolePreference(
        memberId: String,
        preferredRole: String,
        isPreferred: Boolean
    ): Result<Unit> {
        return try {
            val updateData = if (isPreferred) {
                mapOf("preferredRoles" to com.google.firebase.firestore.FieldValue.arrayUnion(preferredRole))
            } else {
                mapOf("preferredRoles" to com.google.firebase.firestore.FieldValue.arrayRemove(preferredRole))
            }
            
            val result = firestoreService.updateMemberPreferences(memberId, updateData)
            when (result) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected loading state while updating role preference"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating role preference for member: $memberId")
            Result.Error(e)
        }
    }

    override suspend fun getMemberRoleStatistics(memberId: String): Result<Map<String, Int>> {
        return try {
            val result = roleRepository.getMemberRoleHistory(memberId, 1000) // Get all roles
            when (result) {
                is Result.Success<*> -> {
                    val roles = (result.data as? List<*>)?.filterIsInstance<MemberRole>() ?: emptyList()
                    val roleCounts = roles.groupBy { it.roleId }.mapValues { it.value.size }

                    // Get role names for the counts
                    val roleIds = roleCounts.keys.toList()
                    val rolesResult = roleRepository.getRolesByIds(roleIds)

                    val roleNameMap = when (rolesResult) {
                        is Result.Success<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val roleList = rolesResult.data as? List<Role> ?: emptyList()
                            roleList.associate { it.id to it.name }
                        }
                        else -> emptyMap()
                    }

                    // Map role IDs to role names in the result
                    val statistics = roleCounts.mapKeys { (roleId, _) ->
                        roleNameMap[roleId] ?: roleId
                    }

                    Result.Success(statistics)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Success(emptyMap())
                else -> Result.Error(Exception("Unknown result type"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting role statistics for member: $memberId")
            Result.Error(e)
        }
    }

    override suspend fun getClubRoleStatistics(): Result<Map<String, Int>> {
        return try {
            // Get all meetings
            val meetings = firestoreService.getAllMeetings()
                .map { it.toObject(com.bntsoft.toastmasters.domain.model.Meeting::class.java) }
                .filterNotNull()

            // Collect all role assignments
            val allAssignments = mutableListOf<MemberRole>()
            for (meeting in meetings) {
                val result = roleRepository.getMeetingRoleAssignments(meeting.id)
                if (result is Result.Success<*>) {
                    val assignments = (result.data as? List<*>)?.filterIsInstance<MemberRole>()
                    if (assignments != null) {
                        allAssignments.addAll(assignments)
                    }
                }
            }

            // Count role occurrences
            val roleCounts = allAssignments.groupBy { it.roleId }
                .mapValues { it.value.size }

            // Get role names for the counts
            val roleIds = roleCounts.keys.toList()
            val rolesResult = roleRepository.getRolesByIds(roleIds)

            val roleNameMap = when (rolesResult) {
                is Result.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val roleList = rolesResult.data as? List<Role> ?: emptyList()
                    roleList.associate { it.id to it.name }
                }
                else -> emptyMap()
            }

            // Map role IDs to role names in the result
            val statistics = roleCounts.mapKeys { (roleId, _) ->
                roleNameMap[roleId] ?: roleId
            }

            Result.Success(statistics)
        } catch (e: Exception) {
            Timber.e(e, "Error getting club role statistics")
            Result.Error(e)
        }
    }

    override suspend fun isMemberAvailableForRole(
        memberId: String,
        roleId: String,
        meetingId: String
    ): Boolean {
        val availability = getMemberAvailabilityForRole(memberId, roleId, meetingId)
        return when (availability) {
            is Result.Success -> availability.data
            is Result.Error -> {
                Timber.e(availability.exception, "Error checking member availability for role")
                false
            }
            is Result.Loading -> false
        }
    }

    override suspend fun getMembersForRole(roleId: String, limit: Int): Result<List<User>> {
        return try {
            val result = roleRepository.getMembersForRole(roleId, limit)
            when (result) {
                is Result.Success<*> -> {
                    val memberIds = (result.data as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val members = memberIds.mapNotNull { memberId -> getMemberById(memberId) }
                    Result.Success(members)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected loading state while getting members for role"))
                else -> Result.Error(Exception("Unknown result type"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting members for role: $roleId")
            Result.Error(e)
        }
    }
}
