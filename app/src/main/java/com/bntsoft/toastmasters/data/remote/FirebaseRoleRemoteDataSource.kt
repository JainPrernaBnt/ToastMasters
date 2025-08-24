package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.mapper.RoleMapper
import com.bntsoft.toastmasters.data.model.role.*
import com.bntsoft.toastmasters.data.source.remote.RoleRemoteDataSource
import com.bntsoft.toastmasters.util.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRoleRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : RoleRemoteDataSource {

    companion object {
        private const val ROLES_COLLECTION = "roles"
        private const val ROLE_ASSIGNMENTS_COLLECTION = "role_assignments"
        private const val MEMBER_PREFERENCES_COLLECTION = "member_preferences"
    }

    // Role operations
    override suspend fun getAllRoles(): List<RoleDto> {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            firestore.collection(ROLES_COLLECTION)
                .get()
                .await()
                .mapNotNull { document ->
                    document.toObject(RoleDto::class.java).copy(id = document.id)
                }
        } catch (e: Exception) {
            Timber.e(e, "Error getting all roles")
            throw e
        }
    }

    override suspend fun getRoleById(roleId: String): RoleDto? {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            val document = firestore.collection(ROLES_COLLECTION)
                .document(roleId)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject(RoleDto::class.java)?.copy(id = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting role by id: $roleId")
            throw e
        }
    }

    override suspend fun getRolesByIds(roleIds: List<String>): List<RoleDto> {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            if (roleIds.isEmpty()) return emptyList()
            
            // Firestore has a limit of 10 documents per 'in' query
            return roleIds.chunked(10).flatMap { chunk ->
                firestore.collection(ROLES_COLLECTION)
                    .whereIn("__name__", chunk)
                    .get()
                    .await()
                    .mapNotNull { document ->
                        document.toObject(RoleDto::class.java).copy(id = document.id)
                    }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting roles by ids: $roleIds")
            throw e
        }
    }

    // Role assignment operations
    override suspend fun assignRole(request: AssignRoleRequestDto): RoleAssignmentResponseDto {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            val assignment = MemberRoleDto(
                id = "", // Let Firestore auto-generate the ID
                meetingId = request.meetingId,
                memberId = request.memberId,
                roleId = request.roleId,
                notes = request.notes
            )
            
            val docRef = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION).document()
            val newAssignment = assignment.copy(id = docRef.id)
            
            docRef.set(newAssignment).await()
            
            RoleAssignmentResponseDto(
                success = true,
                message = "Role assigned successfully",
                assignment = newAssignment
            )
        } catch (e: Exception) {
            Timber.e(e, "Error assigning role: $request")
            RoleAssignmentResponseDto(
                success = false,
                message = e.message ?: "Failed to assign role"
            )
        }
    }

    override suspend fun getMemberRoles(memberId: String): List<MemberRoleDto> {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("memberId", memberId)
                .get()
                .await()
                .mapNotNull { document ->
                    document.toObject(MemberRoleDto::class.java).copy(id = document.id)
                }
        } catch (e: Exception) {
            Timber.e(e, "Error getting roles for member: $memberId")
            throw e
        }
    }

    override suspend fun getMeetingRoles(meetingId: String): List<MemberRoleDto> {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("meetingId", meetingId)
                .get()
                .await()
                .mapNotNull { document ->
                    document.toObject(MemberRoleDto::class.java).copy(id = document.id)
                }
        } catch (e: Exception) {
            Timber.e(e, "Error getting roles for meeting: $meetingId")
            throw e
        }
    }
    
    override suspend fun getMeetingRoleAssignments(meetingId: String): List<MemberRoleDto> {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("meetingId", meetingId)
                .get()
                .await()
                .mapNotNull { document ->
                    document.toObject(MemberRoleDto::class.java).copy(id = document.id)
                }
        } catch (e: Exception) {
            Timber.e(e, "Error getting role assignments for meeting: $meetingId")
            throw e
        }
    }

    override suspend fun getMemberRoleForMeeting(memberId: String, meetingId: String): MemberRoleDto? {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            val querySnapshot = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("memberId", memberId)
                .whereEqualTo("meetingId", meetingId)
                .limit(1)
                .get()
                .await()
            
            querySnapshot.documents.firstOrNull()?.let { document ->
                document.toObject(MemberRoleDto::class.java)?.copy(id = document.id)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting role for member: $memberId in meeting: $meetingId")
            throw e
        }
    }

    override suspend fun removeRoleAssignment(assignmentId: String) {
        try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .document(assignmentId)
                .delete()
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error removing role assignment: $assignmentId")
            throw e
        }
    }

    // Member preferences
    override suspend fun getMemberPreferences(memberId: String): MemberRolePreferenceDto {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            val document = firestore.collection(MEMBER_PREFERENCES_COLLECTION)
                .document(memberId)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject(MemberRolePreferenceDto::class.java) ?: 
                    MemberRolePreferenceDto(memberId = memberId)
            } else {
                MemberRolePreferenceDto(memberId = memberId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting preferences for member: $memberId")
            throw e
        }
    }

    override suspend fun updateMemberPreferences(
        memberId: String,
        preferredRoles: List<String>?,
        unavailableRoles: List<String>?
    ) {
        try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            val updates = mutableMapOf<String, Any>(
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            
            preferredRoles?.let { updates["preferredRoles"] = it }
            unavailableRoles?.let { updates["unavailableRoles"] = it }
            
            firestore.collection(MEMBER_PREFERENCES_COLLECTION)
                .document(memberId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error updating preferences for member: $memberId")
            throw e
        }
    }

    // Role history and statistics
    override suspend fun getMemberRoleHistory(memberId: String, limit: Int): List<MemberRoleDto> {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("memberId", memberId)
                .orderBy("assignedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .mapNotNull { document ->
                    document.toObject(MemberRoleDto::class.java).copy(id = document.id)
                }
        } catch (e: Exception) {
            Timber.e(e, "Error getting role history for member: $memberId")
            throw e
        }
    }

    override suspend fun getRoleStatistics(roleId: String): Map<String, Any> {
        return try {
            if (!networkMonitor.isOnline.first()) throw IllegalStateException("No internet connection")
            
            // This is a simplified example - in a real app, you might use Firestore aggregation queries
            // or Cloud Functions to compute statistics
            val assignments = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("roleId", roleId)
                .get()
                .await()
                .size()
            
            mapOf(
                "totalAssignments" to assignments,
                "roleId" to roleId,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting statistics for role: $roleId")
            throw e
        }
    }

    // Batch operations
    override suspend fun assignMultipleRoles(requests: List<AssignRoleRequestDto>): List<RoleAssignmentResponseDto> {
        return if (!networkMonitor.isOnline.first()) {
            requests.map { 
                RoleAssignmentResponseDto(
                    success = false,
                    message = "No internet connection"
                )
            }
        } else {
            try {
                val batch = firestore.batch()
                val responses = mutableListOf<RoleAssignmentResponseDto>()
                
                requests.forEach { request ->
                    try {
                        val docRef = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION).document()
                        val assignment = MemberRoleDto(
                            id = docRef.id,
                            meetingId = request.meetingId,
                            memberId = request.memberId,
                            roleId = request.roleId,
                            notes = request.notes
                        )
                        
                        batch.set(docRef, assignment)
                        responses.add(
                            RoleAssignmentResponseDto(
                                success = true,
                                message = "Role assigned successfully",
                                assignment = assignment
                            )
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error in batch role assignment: $request")
                        responses.add(
                            RoleAssignmentResponseDto(
                                success = false,
                                message = e.message ?: "Failed to assign role"
                            )
                        )
                    }
                }
                
                batch.commit().await()
                responses
            } catch (e: Exception) {
                Timber.e(e, "Error in batch role assignments")
                requests.map { 
                    RoleAssignmentResponseDto(
                        success = false,
                        message = e.message ?: "Failed to assign roles in batch"
                    )
                }
            }
        }
    }

    // Role templates
    override suspend fun getRoleTemplates(): Map<String, List<String>> {
        // In a real app, this would fetch templates from Firestore
        // For now, return some default templates
        return mapOf(
            "standard" to listOf("toastmaster", "general_evaluator", "speaker", "evaluator"),
            "speech_contest" to listOf("contest_chair", "chief_judge", "contestant", "judge"),
            "evaluation_contest" to listOf("contest_chair", "test_speaker", "contestant", "judge")
        )
    }

    override suspend fun applyRoleTemplate(meetingId: String, templateId: String) {
        // In a real app, this would apply the template to the meeting
        // For now, just log the action
        Timber.d("Applying template $templateId to meeting $meetingId")
    }
}
