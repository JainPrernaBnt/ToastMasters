package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.mapper.MeetingDomainMapper
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.role.AssignRoleRequest
import com.bntsoft.toastmasters.domain.model.role.MemberRole
import com.bntsoft.toastmasters.domain.model.role.Role
import com.bntsoft.toastmasters.domain.model.role.RoleAssignmentResponse
import com.bntsoft.toastmasters.utils.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMeetingDataSourceImpl @Inject constructor(
    private val meetingMapper: MeetingDomainMapper
) : FirebaseMeetingDataSource {

    companion object {
        private const val MEETINGS_COLLECTION = "meetings"
        private const val ROLES_COLLECTION = "meeting_roles"
        private const val ROLE_ASSIGNMENTS_COLLECTION = "role_assignments"
    }

    private val firestore: FirebaseFirestore by lazy { Firebase.firestore }
    private val meetingsCollection by lazy { firestore.collection(MEETINGS_COLLECTION) }

    override fun getAllMeetings(): Flow<List<Meeting>> = callbackFlow {
        val subscription = meetingsCollection
            .orderBy("date")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val meetings = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val dto = MeetingDto(
                            meetingID = document.getString("meetingID") ?: document.id,
                            date = document.getString("date") ?: "",
                            startTime = document.getString("startTime") ?: "",
                            endTime = document.getString("endTime") ?: "",
                            venue = document.getString("venue") ?: "",
                            theme = document.getString("theme") ?: "No Theme",
                            preferredRoles = (document.get("preferredRoles") as? List<*>)?.filterIsInstance<String>()
                                ?: emptyList(),
                            createdAt = document.getLong("createdAt") ?: 0,
                            isRecurring = document.getBoolean("isRecurring") ?: false
                        )
                        meetingMapper.mapToDomain(dto)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(meetings).isSuccess
            }

        awaitClose { subscription.remove() }
    }

    override fun getUpcomingMeetings(afterDate: LocalDate): Flow<List<Meeting>> = callbackFlow {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dateString = afterDate.format(formatter)

        val subscription = meetingsCollection
            .whereGreaterThanOrEqualTo("date", dateString)
            .orderBy("date")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val meetings = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val dateString = document.getString("date") ?: ""
                        val startTimeString = document.getString("startTime") ?: "00:00"
                        val endTimeString = document.getString("endTime") ?: "23:59"

                        // Parse date and time
                        val date = LocalDate.parse(dateString, formatter)
                        val startTime = LocalTime.parse(startTimeString)
                        val endTime = LocalTime.parse(endTimeString)

                        val dateTime = LocalDateTime.of(date, startTime)
                        val endDateTime = LocalDateTime.of(date, endTime)

                        val dto = MeetingDto(
                            meetingID = document.getString("meetingID") ?: document.id,
                            date = dateString,
                            startTime = startTimeString,
                            endTime = endTimeString,
                            venue = document.getString("venue") ?: "",
                            theme = document.getString("theme") ?: "No Theme",
                            preferredRoles = (document.get("preferredRoles") as? List<*>)?.filterIsInstance<String>()
                                ?: emptyList(),
                            createdAt = document.getLong("createdAt") ?: 0,
                            isRecurring = document.getBoolean("isRecurring") ?: false
                        )

                        val meeting = meetingMapper.mapToDomain(dto)
                        // Ensure the meeting's date matches the filter
                        if (date.isAfter(afterDate) || date.isEqual(afterDate)) {
                            meeting
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing meeting document")
                        null
                    }
                } ?: emptyList()

                trySend(meetings).isSuccess
            }

        awaitClose {
            subscription.remove()

        }
    }


    override suspend fun getMeetingById(id: String): Meeting? {
        return try {
            // First try with meetingID (uppercase ID)
            var query = meetingsCollection.whereEqualTo("meetingID", id).limit(1).get().await()
            var document = query.documents.firstOrNull()

            // If not found, try with id (lowercase)
            if (document == null) {
                query = meetingsCollection.whereEqualTo("id", id).limit(1).get().await()
                document = query.documents.firstOrNull()
            }

            val dto = document?.toObject(MeetingDto::class.java)
            dto?.let { meetingMapper.mapToDomain(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error getting meeting by id: $id")
            null
        }
    }

    override suspend fun createMeeting(meeting: Meeting): Result<Meeting> {
        return try {
            val document = meetingsCollection.document()
            val dto = meetingMapper.mapToDto(meeting).copy(meetingID = document.id)
            document.set(dto).await()
            val newMeeting = meetingMapper.mapToDomain(dto)

            // Send notification about the new meeting
            sendMeetingNotification(newMeeting)

            Result.Success(newMeeting)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create meeting")
            Result.Error(e)
        }
    }

    override suspend fun updateMeeting(meeting: Meeting): Result<Unit> {
        return try {
            val dto = meetingMapper.mapToDto(meeting)
            // Find the document with the matching meetingID
            val query =
                meetingsCollection.whereEqualTo("meetingID", meeting.id).limit(1).get().await()
            val document = query.documents.firstOrNull()

            if (document != null) {
                document.reference.update(
                    mapOf(
                        "date" to dto.date,
                        "startTime" to dto.startTime,
                        "endTime" to dto.endTime,
                        "venue" to dto.venue,
                        "theme" to dto.theme,
                        "preferredRoles" to dto.preferredRoles,
                        "isRecurring" to dto.isRecurring
                    )
                ).await()
                Result.Success(Unit)
            } else {
                Result.Error(NoSuchElementException("Meeting not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update meeting")
            Result.Error(e)
        }
    }

    override suspend fun deleteMeeting(id: String): Result<Unit> {
        return try {
            // Find the document with the matching meetingID field and delete it
            val query = meetingsCollection.whereEqualTo("meetingID", id).limit(1).get().await()
            val document = query.documents.firstOrNull()
            if (document != null) {
                document.reference.delete().await()
                Result.Success(Unit)
            } else {
                Result.Error(NoSuchElementException("Meeting not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete meeting")
            Result.Error(e)
        }
    }

    override suspend fun completeMeeting(meetingId: String): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to "COMPLETED"
            )
            val query =
                meetingsCollection.whereEqualTo("meetingID", meetingId).limit(1).get().await()
            val document = query.documents.firstOrNull()

            if (document != null) {
                document.reference.update(updates).await()
                Result.Success(Unit)
            } else {
                Result.Error(NoSuchElementException("Meeting not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error completing meeting $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun sendMeetingNotification(meeting: Meeting): Result<Unit> {
        return try {
            // Get all members assigned to roles in this meeting
            val snapshot = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("meetingId", meeting.id)
                .get()
                .await()

            // Extract unique member IDs
            val memberIds = snapshot.documents.mapNotNull { doc ->
                doc.getString("memberId")
            }.distinct()

            // In a real implementation, we would send notifications to each member
            // For now, we'll just log the notification details
            Timber.d("Sending notification for meeting ${meeting.id} to members: $memberIds")
            
            // Simulate notification sending
            memberIds.forEach { memberId ->
                // In a real app, this would use a notification service
                // For example: notificationService.sendMeetingNotification(memberId, meeting)
                Timber.d("Notification sent to member $memberId about meeting ${meeting.id}")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send notifications for meeting: ${meeting.id}")
            Result.Error(e)
        }
    }

    override suspend fun assignRole(request: AssignRoleRequest): Result<RoleAssignmentResponse> {
        return try {
            val now = LocalDateTime.now()
            val assignmentRef = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION).document()
            val assignment = hashMapOf(
                "meetingId" to request.meetingId,
                "memberId" to request.memberId,
                "roleId" to request.roleId,
                "assignedAt" to now,
                "assignedBy" to request.assignedBy,
                "status" to "assigned",
                "notes" to ""
            )
            
            assignmentRef.set(assignment).await()
            
            // Create MemberRole object for the response
            val memberRole = MemberRole(
                id = assignmentRef.id,
                meetingId = request.meetingId,
                memberId = request.memberId,
                roleId = request.roleId,
                assignedAt = now,
                assignedBy = request.assignedBy,
                status = "assigned",
                notes = ""
            )
            
            val response = RoleAssignmentResponse(
                success = true,
                message = "Role assigned successfully",
                assignment = memberRole
            )
            
            Result.Success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to assign role")
            Result.Error(e)
        }
    }

    override suspend fun getAssignedRoles(meetingId: String): Result<List<MemberRole>> {
        return try {
            val snapshot = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("meetingId", meetingId)
                .get()
                .await()

            val roles = snapshot.documents.mapNotNull { doc ->
                try {
                    val assignedAt = when (val timestamp = doc.getTimestamp("assignedAt")) {
                        null -> LocalDateTime.now()
                        else -> timestamp.toDate().toInstant().atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    }

                    MemberRole(
                        id = doc.id,
                        meetingId = doc.getString("meetingId") ?: "",
                        memberId = doc.getString("memberId") ?: "",
                        roleId = doc.getString("roleId") ?: "",
                        assignedAt = assignedAt,
                        assignedBy = doc.getString("assignedBy") ?: "",
                        status = doc.getString("status") ?: "assigned",
                        notes = doc.getString("notes") ?: ""
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing role assignment")
                    null
                }
            }

            Result.Success(roles)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get assigned roles for meeting: $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun getAssignedRole(
        meetingId: String,
        memberId: String
    ): Result<MemberRole?> {
        return try {
            val snapshot = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("meetingId", meetingId)
                .whereEqualTo("memberId", memberId)
                .limit(1)
                .get()
                .await()

            val role = snapshot.documents.firstOrNull()?.let { doc ->
                try {
                    val assignedAt = when (val timestamp = doc.getTimestamp("assignedAt")) {
                        null -> LocalDateTime.now()
                        else -> timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }
                    
                    MemberRole(
                        id = doc.id,
                        meetingId = doc.getString("meetingId") ?: "",
                        memberId = doc.getString("memberId") ?: "",
                        roleId = doc.getString("roleId") ?: "",
                        assignedAt = assignedAt,
                        assignedBy = doc.getString("assignedBy") ?: "",
                        status = doc.getString("status") ?: "assigned",
                        notes = doc.getString("notes") ?: ""
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing role assignment")
                    null
                }
            }

            Result.Success(role)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get assigned role for member: $memberId")
            Result.Error(e)
        }
    }

    override suspend fun removeRoleAssignment(assignmentId: String): Result<Unit> {
        return try {
            firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .document(assignmentId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error removing role assignment: $assignmentId")
            Result.Error(e)
        }
    }

    override suspend fun updateRoleAssignment(assignment: MemberRole): Result<Unit> {
        return try {
            val timestamp = Timestamp(
                assignment.assignedAt.atZone(ZoneId.systemDefault()).toEpochSecond(),
                assignment.assignedAt.nano
            )
            
            val data = hashMapOf(
                "meetingId" to assignment.meetingId,
                "memberId" to assignment.memberId,
                "roleId" to assignment.roleId,
                "status" to assignment.status,
                "assignedBy" to assignment.assignedBy,
                "assignedAt" to timestamp,
                "notes" to assignment.notes
            )
            
            firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .document(assignment.id)
                .update(data as Map<String, Any>)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update role assignment")
            Result.Error(e)
        }
    }

    // Role availability
    override suspend fun getAvailableRoles(meetingId: String): Result<List<Role>> {
        return try {
            val snapshot = firestore.collection(ROLES_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .await()
                
            val roles = snapshot.documents.mapNotNull { doc ->
                try {
                    Role(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        isLeadership = doc.getBoolean("isLeadership") ?: false,
                        isSpeaker = doc.getBoolean("isSpeaker") ?: false,
                        isEvaluator = doc.getBoolean("isEvaluator") ?: false
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing role")
                    null
                }
            }
            
            Result.Success(roles)
        } catch (e: Exception) {
            Timber.e(e, "Error getting available roles for meeting: $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun getMembersForRole(
        meetingId: String,
        roleId: String
    ): Result<List<String>> {
        return try {
            val snapshot = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("meetingId", meetingId)
                .whereEqualTo("roleId", roleId)
                .get()
                .await()
                
            val memberIds = snapshot.documents.mapNotNull { doc ->
                doc.getString("memberId")
            }
            
            Result.Success(memberIds)
        } catch (e: Exception) {
            Timber.e(e, "Error getting members for role: $roleId in meeting: $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun assignMultipleRoles(requests: List<AssignRoleRequest>): Result<List<RoleAssignmentResponse>> {
        return try {
            if (requests.isEmpty()) return Result.Success(emptyList())
            
            val batch = firestore.batch()
            val responses = mutableListOf<RoleAssignmentResponse>()
            val now = LocalDateTime.now()
            
            // First pass: Prepare all batch operations and collect responses
            requests.forEach { request ->
                try {
                    val docRef = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION).document()
                    val assignment = hashMapOf(
                        "meetingId" to request.meetingId,
                        "memberId" to request.memberId,
                        "roleId" to request.roleId,
                        "assignedAt" to now,
                        "assignedBy" to request.assignedBy,
                        "status" to "assigned",
                        "notes" to ""
                    )
                    batch.set(docRef, assignment)
                    
                    // Create MemberRole object for the response
                    val memberRole = MemberRole(
                        id = docRef.id,
                        meetingId = request.meetingId,
                        memberId = request.memberId,
                        roleId = request.roleId,
                        assignedAt = now,
                        assignedBy = request.assignedBy,
                        status = "assigned",
                        notes = ""
                    )
                    
                    responses.add(
                        RoleAssignmentResponse(
                            success = true,
                            message = "Role assigned successfully",
                            assignment = memberRole
                        )
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error creating role assignment for member: ${request.memberId}")
                    responses.add(
                        RoleAssignmentResponse(
                            success = false,
                            message = e.message ?: "Failed to assign role",
                            assignment = null
                        )
                    )
                }
            }

            // Only commit if there are successful operations
            if (responses.any { it.success }) {
                batch.commit().await()
            }
            
            Result.Success(responses)
        } catch (e: Exception) {
            Timber.e(e, "Error in assignMultipleRoles")
            Result.Error(e)
        }
    }

    override suspend fun getMeetingRoleAssignments(meetingId: String): Result<Map<String, MemberRole>> {
        return try {
            val snapshot = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("meetingId", meetingId)
                .get()
                .await()
                
            val assignments = snapshot.documents.associate { doc ->
                val assignedAt = when (val timestamp = doc.getTimestamp("assignedAt")) {
                    null -> LocalDateTime.now()
                    else -> timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                }
                
                val role = MemberRole(
                    id = doc.id,
                    meetingId = doc.getString("meetingId") ?: "",
                    memberId = doc.getString("memberId") ?: "",
                    roleId = doc.getString("roleId") ?: "",
                    assignedAt = assignedAt,
                    assignedBy = doc.getString("assignedBy") ?: "",
                    status = doc.getString("status") ?: "assigned",
                    notes = doc.getString("notes") ?: ""
                )
                doc.id to role
            }
            
            Result.Success(assignments)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get meeting role assignments")
            Result.Error(e)
        }
    }

    // Role statistics
    override suspend fun getMeetingRoleStatistics(meetingId: String): Result<Map<String, Any>> {
        return try {
            val assignmentsResult = getAssignedRoles(meetingId)
            val assignments = when (assignmentsResult) {
                is Result.Success -> assignmentsResult.data
                is Result.Error -> emptyList()
                Result.Loading -> emptyList()
            }
            
            val availableRolesResult = getAvailableRoles(meetingId)
            val availableRoles = when (availableRolesResult) {
                is Result.Success -> availableRolesResult.data.size
                is Result.Error -> 0
                Result.Loading -> 0
            }
            
            val stats = mutableMapOf<String, Any>(
                "totalAssignments" to assignments.size,
                "assignedMembers" to assignments.map { it.memberId }.distinct().size,
                "availableRoles" to availableRoles,
                "assignmentsByRole" to assignments.groupBy { it.roleId }.mapValues { it.value.size },
                "assignmentsByMember" to assignments.groupBy { it.memberId }.mapValues { it.value.size },
                "lastUpdated" to System.currentTimeMillis()
            )
            
            Result.Success(stats)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get meeting role statistics")
            Result.Error(e)
        }
    }

    // Role templates
    override suspend fun applyRoleTemplate(meetingId: String, templateId: String): Result<Unit> {
        return try {
            // In a real implementation, this would apply a predefined set of roles to the meeting
            // For now, we'll just log the action
            Timber.d("Applying role template $templateId to meeting $meetingId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply role template $templateId to meeting $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun getAvailableRoleTemplates(): Result<Map<String, String>> {
        return try {
            // In a real implementation, this would fetch available role templates from Firestore
            // For now, return some sample templates
            val templates = mapOf(
                "default" to "Default Meeting Roles",
                "evaluation" to "Evaluation Meeting",
                "speech_contest" to "Speech Contest",
                "officer_meeting" to "Officer Meeting"
            )
            Result.Success(templates)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get available role templates")
            Result.Error(e)
        }
    }

}
