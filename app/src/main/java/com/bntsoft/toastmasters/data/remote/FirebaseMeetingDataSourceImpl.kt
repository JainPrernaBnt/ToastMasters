package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.mapper.MeetingDomainMapper
import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.data.model.SpeakerDetails
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.RoleAssignmentItem
import com.bntsoft.toastmasters.utils.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMeetingDataSourceImpl @Inject constructor(
    private val meetingMapper: MeetingDomainMapper
) : FirebaseMeetingDataSource {

    companion object {
        private const val MEETINGS_COLLECTION = "meetings"
        private const val ROLE_ASSIGNMENTS_COLLECTION = "role_assignments"
    }

    private val firestore: FirebaseFirestore by lazy { Firebase.firestore }
    private val meetingsCollection by lazy { firestore.collection(MEETINGS_COLLECTION) }

    private fun parseCreatedAtTimestamp(timestamp: Any?): Long {
        return try {
            when (timestamp) {
                is Long -> timestamp
                is String -> {
                    // Parse the string timestamp to milliseconds
                    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' hh:mm:ss a z")
                    val dateTime = LocalDateTime.parse(timestamp, formatter)
                    dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }

                is Timestamp -> timestamp.toDate().time
                is Date -> timestamp.time
                else -> System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error parsing createdAt timestamp: $timestamp", e)
            System.currentTimeMillis()
        }
    }

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
                        val dateString = document.getString("date") ?: ""
                        val startTimeString = document.getString("startTime") ?: "00:00"
                        val endTimeString = document.getString("endTime") ?: ""

                        // Parse date and times
                        val date = try {
                            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (e: Exception) {
                            LocalDate.now()
                        }

                        val startTime = try {
                            LocalTime.parse(startTimeString)
                        } catch (e: Exception) {
                            LocalTime.NOON
                        }

                        val endTime = try {
                            if (endTimeString.isNotBlank()) {
                                LocalTime.parse(endTimeString)
                            } else {
                                startTime.plusHours(2)
                            }
                        } catch (e: Exception) {
                            startTime.plusHours(2)
                        }

                        // Create LocalDateTime objects
                        val dateTime = LocalDateTime.of(date, startTime)
                        val endDateTime = LocalDateTime.of(date, endTime)

                        val dto = MeetingDto(
                            meetingID = document.getString("meetingID") ?: document.id,
                            date = dateString,
                            startTime = startTimeString,
                            endTime = endTimeString,
                            dateTime = dateTime,
                            endDateTime = endDateTime,
                            venue = document.getString("venue") ?: "",
                            theme = document.getString("theme") ?: "No Theme",
                            roleCounts = (document.get("roleCounts") as? Map<String, *>)?.mapValues { (_, value) ->
                                when (value) {
                                    is Long -> value.toInt()
                                    is Int -> value
                                    else -> 1
                                }
                            } ?: emptyMap(),
                            createdAt = parseCreatedAtTimestamp(document.get("createdAt")),
                            status = document.getString("status")?.let {
                                try {
                                    com.bntsoft.toastmasters.domain.models.MeetingStatus.valueOf(it)
                                } catch (e: Exception) {
                                    com.bntsoft.toastmasters.domain.models.MeetingStatus.NOT_COMPLETED
                                }
                            } ?: com.bntsoft.toastmasters.domain.models.MeetingStatus.NOT_COMPLETED,
                            assignedCounts = (document.get("assignedCounts") as? Map<String, *>)?.mapValues { (_, value) ->
                                when (value) {
                                    is Long -> value.toInt()
                                    is Int -> value
                                    else -> 0
                                }
                            } ?: emptyMap()
                        )

                        Log.d(
                            "FirebaseDS",
                            "Mapped meeting in getAllMeetings: ${dto.meetingID} - ${dto.theme} - ${dto.startTime} to ${dto.endTime}"
                        )
                        meetingMapper.mapToDomain(dto)
                    } catch (e: Exception) {
                        Log.e("FirebaseDS", "Error parsing meeting document in getAllMeetings", e)
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
        val now = com.google.firebase.Timestamp.now()

        Log.d("FirebaseDS", "Fetching meetings after $dateString, current time: $now")

        val subscription = meetingsCollection
            .whereGreaterThanOrEqualTo("meetingDate", now)
            .whereEqualTo("meeting.status", "NOT_COMPLETED")
            .orderBy("meetingDate")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val meetings = snapshot?.documents?.mapNotNull { document ->
                    try {
                        Log.d("FirebaseDS", "Raw document data: ${document.data}")
                        // Get meeting data, handling both nested and root level fields
                        val meetingData = if (document.contains("meeting")) {
                            document.get("meeting") as? Map<String, Any> ?: emptyMap()
                        } else {
                            // If no nested meeting object, use root document fields
                            document.data ?: emptyMap()
                        }

                        val dateTime = (document.get("meetingDate") as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                            ?.atZone(java.time.ZoneId.systemDefault())
                            ?.toLocalDateTime() ?: LocalDateTime.now()

                        val startTimeString = document.getString("startTime") ?: "00:00"
                        val endTimeString = document.getString("endTime") ?: "23:59"

                        val startTime = try { LocalTime.parse(startTimeString) } catch (e: Exception) { LocalTime.NOON }
                        val endTime = try { LocalTime.parse(endTimeString) } catch (e: Exception) { startTime.plusHours(2) }

                        val date = dateTime.toLocalDate()
                        val startDateTime = LocalDateTime.of(date, startTime)
                        val endDateTime = LocalDateTime.of(date, endTime)

                        // Get officers map
                        val officers = document.get("officers") as? Map<String, String> ?: emptyMap()

                        // Create the DTO with all fields
                        val dto = MeetingDto(
                            meetingID = document.id,
                            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            startTime = startTimeString,
                            endTime = endTimeString,
                            dateTime = startDateTime,
                            endDateTime = endDateTime,
                            venue = document.getString("location") ?: "",
                            // Try to get theme from meetingData first, then from root document
                            theme = (meetingData["theme"] as? String)
                                ?: (document.getString("theme") ?: "No Theme"),
                            roleCounts = (meetingData["roleCounts"] as? Map<String, *>)?.mapValues { (_, value) ->
                                when (value) {
                                    is Long -> value.toInt()
                                    is Int -> value
                                    else -> 1
                                }
                            } ?: emptyMap(),
                            createdAt = parseCreatedAtTimestamp(document.get("createdAt")),
                            status = (meetingData["status"] as? String)?.let {
                                try {
                                    com.bntsoft.toastmasters.domain.models.MeetingStatus.valueOf(it)
                                } catch (e: Exception) {
                                    com.bntsoft.toastmasters.domain.models.MeetingStatus.NOT_COMPLETED
                                }
                            } ?: com.bntsoft.toastmasters.domain.models.MeetingStatus.NOT_COMPLETED,
                            assignedCounts = (document.get("assignedCounts") as? Map<String, *>)?.mapValues { (_, value) ->
                                when (value) {
                                    is Long -> value.toInt()
                                    is Int -> value
                                    else -> 0
                                }
                            } ?: emptyMap()
                        )

                        Log.d(
                            "FirebaseDS",
                            "Mapped meeting: ${dto.meetingID} - ${dto.theme} - ${dto.startTime} to ${dto.endTime} (${dto.dateTime} to ${dto.endDateTime})"
                        )

                        val meeting = try {
                            meetingMapper.mapToDomain(dto).also {
                                Log.d("FirebaseDS", "Successfully mapped meeting: ${it.id} - ${it.theme}")
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseDS", "Error mapping meeting DTO to domain: ${dto}", e)
                            null
                        }

                        meeting
                    } catch (e: Exception) {
                        Log.e("FirebaseDS", "Error parsing meeting document", e)
                        null
                    }
                } ?: emptyList()

                trySend(meetings).isSuccess
            }

        awaitClose {
            subscription.remove()

        }
    }


    override suspend fun getUserPreferredRoles(meetingId: String, userId: String): List<String> {
        return try {
            // Check if the user has marked any preferred roles for this meeting
            val preferenceDoc = firestore
                .collection("meetings")
                .document(meetingId)
                .collection("availability")
                .document(userId)
                .get()
                .await()

            // Return the preferred roles if they exist, otherwise empty list
            (preferenceDoc.get("preferredRoles") as? List<*>)?.filterIsInstance<String>()
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error fetching preferred roles for user $userId in meeting $meetingId", e)
            emptyList()
        }
    }

    override suspend fun getMeetingPreferredRoles(meetingId: String): List<String> {
        return try {
            val document = meetingsCollection.document(meetingId).get().await()
            val roleCounts =
                document.get("roleCounts") as? Map<String, *> ?: emptyMap<String, Any>()
            roleCounts.keys.toList()
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting meeting preferred roles", e)
            Log.e("FirebaseDS", "Error fetching preferred roles for meeting $meetingId", e)
            emptyList()
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
            Log.e("FirebaseDS", "Error getting meeting by id: $id", e)
            null
        }
    }

    override suspend fun createMeeting(meeting: Meeting): Result<Meeting> {
        return try {
            val document = meetingsCollection.document()
            val dto = meetingMapper.mapToDto(meeting).copy(meetingID = document.id)

            val meetingMap = hashMapOf(
                "meetingID" to dto.meetingID,
                "date" to dto.date,
                "startTime" to dto.startTime,
                "endTime" to dto.endTime,
                "venue" to dto.venue,
                "theme" to dto.theme,
                "roleCounts" to dto.roleCounts,
                "assignedCounts" to dto.assignedCounts,
                "createdAt" to dto.createdAt,
                "status" to dto.status.name,
                "assignedRoles" to (dto.assignedRoles ?: emptyMap()),
                "isRecurring" to (dto.isRecurring ?: false),
                "createdBy" to (dto.createdBy ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            Log.d("FirebaseDS", "Creating meeting with roleCounts: ${dto.roleCounts}")
            document.set(meetingMap).await()
            val newMeeting = meetingMapper.mapToDomain(dto)

            // Send notification about the new meeting
            sendMeetingNotification(newMeeting)

            Result.Success(newMeeting)
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Failed to create meeting", e)
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
                val updateMap = mutableMapOf<String, Any>(
                    "date" to dto.date,
                    "startTime" to dto.startTime,
                    "endTime" to dto.endTime,
                    "venue" to dto.venue,
                    "theme" to dto.theme,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "status" to dto.status.name,
                )

                // Update roleCounts if they're provided
                updateMap["roleCounts"] = dto.roleCounts.ifEmpty { emptyMap() }

                // Handle assignedCounts - merge with existing or initialize with zeros
                val currentData = document.data ?: emptyMap()
                val currentAssignedCounts =
                    currentData["assignedCounts"] as? Map<*, *> ?: emptyMap<String, Int>()

                // If we have new role counts, ensure all roles have assigned counts
                val updatedAssignedCounts = if (dto.roleCounts.isNotEmpty()) {
                    val mergedCounts = currentAssignedCounts.toMutableMap()
                    // Add any new roles with 0 assigned count
                    dto.roleCounts.keys.forEach { role ->
                        if (!mergedCounts.containsKey(role)) {
                            mergedCounts[role] = 0
                        }
                    }
                    // Remove any roles that no longer exist in roleCounts
                    mergedCounts.keys.toList().forEach { role ->
                        if (!dto.roleCounts.containsKey(role)) {
                            mergedCounts.remove(role)
                        }
                    }
                    mergedCounts
                } else {
                    currentAssignedCounts as Map<String, Int>
                }

                updateMap["assignedCounts"] = updatedAssignedCounts

                document.reference.update(updateMap).await()
                Result.Success(Unit)
            } else {
                Result.Error(NoSuchElementException("Meeting not found"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Failed to update meeting", e)
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
            Log.e("FirebaseDS", "Failed to delete meeting", e)
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
            Log.e("FirebaseDS", "Error completing meeting $meetingId", e)
            Result.Error(e)
        }
    }

    override suspend fun saveRoleAssignments(
        meetingId: String,
        assignments: List<RoleAssignmentItem>
    ): Result<Unit> {
        return try {
            val meetingRef = firestore.collection("meetings").document(meetingId)
            val assignedRolesRef = meetingRef.collection("assignedRole")

            // Collect all userIds we are updating
            val userIds = assignments.map { it.userId }.distinct()

            firestore.runTransaction { transaction ->
                // Get current meeting data
                val meetingDoc = transaction.get(meetingRef)
                val currentAssignedCounts =
                    (meetingDoc.get("assignedCounts") as? Map<*, *> ?: emptyMap<String, Int>())
                        .mapValues { (_, value) -> (value as? Number)?.toInt() ?: 0 }
                        .toMutableMap()

                // Save new user assignment docs
                assignments.forEach { assignment ->
                    val userDocRef = assignedRolesRef.document(assignment.userId)

                    val roleData = mapOf(
                        "userId" to assignment.userId,
                        "memberName" to assignment.memberName,
                        "roles" to assignment.selectedRoles,
                        "assignedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    // Merge to preserve additional fields like evaluator/evaluatorId/evaluatorRole
                    transaction.set(userDocRef, roleData, SetOptions.merge())
                }

                // Recalculate assignedCounts
                val allAssignedRoles =
                    assignments.flatMap { it.selectedRoles }.groupingBy { it }.eachCount()

                val updatedAssignedCounts = currentAssignedCounts.toMutableMap()

                // Reset roles that no longer exist
                currentAssignedCounts.keys.forEach { role ->
                    if (allAssignedRoles[role] == null) {
                        updatedAssignedCounts[role] = 0
                    }
                }
                // Update with new counts
                allAssignedRoles.forEach { (role, count) ->
                    updatedAssignedCounts[role] = count
                }

                transaction.update(meetingRef, "assignedCounts", updatedAssignedCounts)

                null
            }.await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error saving role assignments for meeting: $meetingId", e)
            Result.Error(e)
        }
    }


    override suspend fun getAssignedRole(meetingId: String, userId: String): String? {
        return try {
            // First check the assigned roles collection
            val assignedRoleQuery = firestore
                .collection("meetings")
                .document(meetingId)
                .collection("assignedRole") // Corrected subcollection name
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()

            val assignedRoleDoc = assignedRoleQuery.documents.firstOrNull()

            if (assignedRoleDoc?.exists() == true) {
                // The document ID is the role name
                return assignedRoleDoc.id
            } else {
                // Fallback to checking the old location for backward compatibility
                val snapshot = firestore
                    .collection(ROLE_ASSIGNMENTS_COLLECTION)
                    .whereEqualTo("meetingId", meetingId)
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    // Get the first document (should only be one due to limit(1))
                    val doc = snapshot.documents[0]

                    // First try to get roles as an array
                    val roles = doc.get("roles") as? List<*>
                    if (!roles.isNullOrEmpty()) {
                        Log.d("FirebaseDS", "Found roles array in legacy location: $roles")
                        return roles.firstOrNull()?.toString()
                    }

                    // Fall back to single role field if array not found
                    val role = doc.getString("role")
                    if (!role.isNullOrEmpty()) {
                        Log.d("FirebaseDS", "Found single role in legacy location: $role")
                        return role
                    }

                    Log.d("FirebaseDS", "No role found in legacy location")
                    null
                } else {
                    Log.d("FirebaseDS", "No role assignment found in any location")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting assigned role for user $userId in meeting $meetingId", e)
            null
        }
    }

    override suspend fun getAssignedRoles(meetingId: String, userId: String): List<String> {
        return try {
            val assignedRolesRef = firestore.collection("meetings")
                .document(meetingId)
                .collection("assignedRole")
                .document(userId)
                .get()
                .await()

            if (assignedRolesRef.exists()) {
                // Get the roles from the document data
                val roles = assignedRolesRef.get("roles") as? List<*>
                roles?.filterIsInstance<String>() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting assigned roles for user $userId in meeting $meetingId", e)
            emptyList()
        }
    }

    override suspend fun getAllAssignedRoles(meetingId: String): Map<String, List<String>> {
        return try {
            val assignedRolesRef = firestore.collection("meetings")
                .document(meetingId)
                .collection("assignedRole")

            val querySnapshot = assignedRolesRef.get().await()

            querySnapshot.documents.associate { doc ->
                val userId = doc.id
                val roles = (doc.get("roles") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                userId to roles
            }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting all assigned roles for meeting $meetingId", e)
            emptyMap()
        }
    }


    override suspend fun saveSpeakerDetails(
        meetingId: String,
        userId: String,
        speakerDetails: SpeakerDetails
    ): Result<Unit> {
        return try {
            val speakerRef = firestore.collection("meetings")
                .document(meetingId)
                .collection("speakerDetails")
                .document(userId)

            speakerRef.set(speakerDetails).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error saving speaker details for user $userId in meeting $meetingId", e)
            Result.Error(e)
        }
    }

    override suspend fun getSpeakerDetails(meetingId: String, userId: String): SpeakerDetails? {
        return try {
            val doc = firestore.collection("meetings")
                .document(meetingId)
                .collection("speakerDetails")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                doc.toObject(SpeakerDetails::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting speaker details for user $userId in meeting $meetingId", e)
            null
        }
    }

    override suspend fun getSpeakerDetailsForMeeting(meetingId: String): List<SpeakerDetails> {
        return try {
            val snapshot = firestore.collection("meetings")
                .document(meetingId)
                .collection("speakerDetails")
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(SpeakerDetails::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting speaker details for meeting $meetingId", e)
            emptyList()
        }
    }

    override suspend fun saveGrammarianDetails(
        meetingId: String,
        userId: String,
        grammarianDetails: GrammarianDetails
    ): Result<Unit> {
        return try {
            val detailsRef = firestore.collection("meetings")
                .document(meetingId)
                .collection("grammarianDetails")
                .document(userId)

            detailsRef.set(grammarianDetails).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error saving grammarian details for user $userId in meeting $meetingId", e)
            Result.Error(e)
        }
    }

    override suspend fun getGrammarianDetails(
        meetingId: String,
        userId: String
    ): GrammarianDetails? {
        return try {
            val doc = firestore.collection("meetings")
                .document(meetingId)
                .collection("grammarianDetails")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                doc.toObject(GrammarianDetails::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting grammarian details for user $userId in meeting $meetingId", e)
            null
        }
    }

    override suspend fun getGrammarianDetailsForMeeting(meetingId: String): List<GrammarianDetails> {
        return try {
            val snapshot = firestore.collection("meetings")
                .document(meetingId)
                .collection("grammarianDetails")
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(GrammarianDetails::class.java)?.copy(
                        userId = document.id
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseDS", "Error parsing grammarian details for document ${document.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting grammarian details for meeting $meetingId", e)
            emptyList()
        }
    }

    override suspend fun getMemberRolesForMeeting(meetingId: String): List<Pair<String, List<String>>> {
        return try {
            val snapshot = firestore.collection("meetings")
                .document(meetingId)
                .collection("assignedRole")
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    val memberName = document.getString("memberName") ?: return@mapNotNull null
                    val roles = document.get("roles") as? List<*> ?: emptyList<Any>()
                    val roleStrings = roles.filterIsInstance<String>()

                    if (roleStrings.isNotEmpty()) {
                        memberName to roleStrings
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseDS", "Error parsing member roles for document ${document.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting member roles for meeting $meetingId", e)
            emptyList()
        }
    }
    override suspend fun updateSpeakerEvaluator(
        meetingId: String,
        speakerId: String,
        evaluatorName: String,
        evaluatorId: String
    ): Result<Unit> {
        return try {
            // Step 1: Get the next evaluator number BEFORE transaction
            val nextEvaluatorNumber = getNextEvaluatorNumber(meetingId)
            val evaluatorRole = "Evaluator $nextEvaluatorNumber"

            val speakerRef = firestore.collection("meetings")
                .document(meetingId)
                .collection("assignedRole")
                .document(speakerId)

            val meetingRef = firestore.collection("meetings").document(meetingId)
            val evaluatorRef = firestore.collection("meetings")
                .document(meetingId)
                .collection("assignedRole")
                .document(evaluatorId)

            firestore.runTransaction { transaction ->
                val speakerDoc = transaction.get(speakerRef)
                val meetingDoc = transaction.get(meetingRef)
                val evaluatorDoc = transaction.get(evaluatorRef)

                // Check if evaluator already has a role
                val existingEvaluatorRoles = (evaluatorDoc.get("roles") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.filter { it.startsWith("Evaluator") }
                    ?: emptyList()

                // Only assign the evaluatorRole if evaluator has no existing one
                val roleToAssign = if (existingEvaluatorRoles.isNotEmpty()) {
                    existingEvaluatorRoles.first()
                } else {
                    evaluatorRole
                }

                // Update speaker's roles
                val currentRoles = (speakerDoc.get("roles") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                    ?: mutableListOf()
                currentRoles.removeIf { it.startsWith("Evaluator") }
                currentRoles.add(roleToAssign)

                // Prepare speaker data
                val speakerData = speakerDoc.data?.toMutableMap() ?: mutableMapOf()
                speakerData["userId"] = speakerDoc.getString("userId") ?: speakerId
                speakerData["memberName"] = speakerDoc.getString("memberName") ?: ""
                speakerData["assignedAt"] = speakerDoc.getTimestamp("assignedAt") ?: FieldValue.serverTimestamp()
                speakerData["evaluator"] = evaluatorName
                speakerData["evaluatorRole"] = roleToAssign
                speakerData["evaluatorId"] = evaluatorId
                speakerData["roles"] = currentRoles
                speakerData["updatedAt"] = FieldValue.serverTimestamp()

                transaction.set(speakerRef, speakerData, SetOptions.merge())
                Log.d("FirebaseTransaction", "Updated speaker $speakerId with evaluator $evaluatorName ($roleToAssign)")

                // Update meeting assignedCounts only if evaluator didn't have a role
                val currentCounts = (meetingDoc.get("assignedCounts") as? Map<*, *>
                    ?: emptyMap<String, Int>())
                    .mapValues { (_, v) -> (v as? Number)?.toInt() ?: 0 }
                    .toMutableMap()

                if (existingEvaluatorRoles.isEmpty()) {
                    currentCounts[roleToAssign] = (currentCounts[roleToAssign] ?: 0) + 1
                    transaction.update(meetingRef, "assignedCounts", currentCounts)
                    Log.d("FirebaseTransaction", "Updated assignedCounts for $roleToAssign: ${currentCounts[roleToAssign]}")
                }

                null
            }.await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseTransaction", "Error in updateSpeakerEvaluator", e)
            Result.Error(e)
        }
    }

    private suspend fun getNextEvaluatorNumber(meetingId: String): Int {
        return try {
            val meetingRef = firestore.collection("meetings").document(meetingId)
            val doc = meetingRef.get().await()
            doc.getLong("nextEvaluatorNumber")?.toInt() ?: 1
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting next evaluator number", e)
            1
        }
    }
    
    override suspend fun updateSpeakerEvaluators(
        meetingId: String,
        speakerId: String,
        evaluatorIds: List<String>,
        evaluatorNames: Map<String, String>
    ) {
        try {
            val meetingRef = firestore.collection("meetings").document(meetingId)
            val speakerRef = meetingRef.collection("assignedRole").document(speakerId)
            
            // Get or create the speaker document
            val speakerDoc = speakerRef.get().await()
            if (!speakerDoc.exists()) {
                // Create a new speaker document with basic information
                val speakerData = hashMapOf(
                    "userId" to speakerId,
                    "memberName" to "",  // This should be set from the caller if available
                    "roles" to listOf("Speaker"),
                    "evaluatorIds" to emptyList<String>(),
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                speakerRef.set(speakerData).await()
            }
            
            // Get the next available evaluator number
            var nextEvaluatorNumber = getNextEvaluatorNumber(meetingId)
            
            // Create a batch to update all documents in a single transaction
            val batch = firestore.batch()
            
            // Update the speaker document with the list of evaluator IDs
            batch.update(speakerRef, "evaluatorIds", evaluatorIds)
            
            // For each evaluator, assign them the next available evaluator role
            evaluatorIds.forEach { evaluatorId ->
                val evaluatorName = evaluatorNames[evaluatorId] ?: ""
                val evaluatorRole = "Evaluator $nextEvaluatorNumber"
                nextEvaluatorNumber++
                
                val evaluatorRef = meetingRef.collection("assignedRole").document(evaluatorId)
                
                // Check if the evaluator document exists
                val evaluatorDoc = evaluatorRef.get().await()
                
                // Create evaluator data with default values if it doesn't exist
                if (!evaluatorDoc.exists()) {
                    val evaluatorData = hashMapOf(
                        "userId" to evaluatorId,
                        "memberName" to evaluatorName,
                        "roles" to mutableListOf(evaluatorRole),
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                    batch.set(evaluatorRef, evaluatorData)
                } else {
                    // Document exists, update it
                    val currentRoles = (evaluatorDoc.get("roles") as? List<*>) 
                        ?.filterIsInstance<String>()
                        ?.toMutableList() ?: mutableListOf()
                    
                    // Add the evaluator role if not already present
                    if (!currentRoles.any { it.startsWith("Evaluator") }) {
                        currentRoles.add(evaluatorRole)
                        batch.update(evaluatorRef, "roles", currentRoles)
                    }
                    
                    // Update the evaluator's name if not already set
                    if (!evaluatorDoc.contains("name") || (evaluatorDoc.getString("name")?.isEmpty() == true)) {
                        batch.update(evaluatorRef, "name", evaluatorName)
                    }
                    
                    // Update the updatedAt timestamp
                    batch.update(evaluatorRef, "updatedAt", com.google.firebase.Timestamp.now())
                }
            }
            
            // Update the next evaluator number for future assignments
            batch.update(meetingRef, "nextEvaluatorNumber", nextEvaluatorNumber)
            
            // Commit the batch
            batch.commit().await()
            
            Log.d("FirebaseDS", "Updated speaker $speakerId with evaluators: $evaluatorIds")
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error updating speaker evaluators", e)
            throw e
        }
    }
    
    override suspend fun getUserDisplayName(userId: String): String? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.getString("displayName") ?: userDoc.getString("name") ?: userDoc.getString("email")
        } catch (e: Exception) {
            Log.e("FirebaseDS", "Error getting user display name", e)
            null
        }
    }

    override suspend fun updateMeetingRoleCounts(meetingId: String, roleCounts: Map<String, Int>) {
        try {
            firestore.collection("meetings")
                .document(meetingId)
                .update("roleCounts", roleCounts)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseMeetingDataSource", "Error updating role counts for meeting $meetingId", e)
            throw e
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
            Log.d("FirebaseMeetingDataSource", "Sending notification for meeting ${meeting.id} to members: $memberIds")

            // Simulate notification sending
            memberIds.forEach { memberId ->
                // In a real app, this would use a notification service
                // For example: notificationService.sendMeetingNotification(memberId, meeting)
                Log.d("FirebaseMeetingDataSource", "Notification sent to member $memberId about meeting ${meeting.id}")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseMeetingDataSource", "Failed to send notifications for meeting: ${meeting.id}", e)
            Result.Error(e)
        }
    }

}
