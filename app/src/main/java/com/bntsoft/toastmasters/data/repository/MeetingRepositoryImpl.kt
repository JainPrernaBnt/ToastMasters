package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.data.model.MemberRole
import com.bntsoft.toastmasters.data.model.SpeakerDetails
import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSource
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.model.RoleAssignmentItem
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.utils.Resource
import com.bntsoft.toastmasters.utils.Result
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseMeetingDataSource,
    private val memberResponseRepository: MemberResponseRepository
) : MeetingRepository {

    override fun getAllMeetings(): Flow<List<Meeting>> {
        return firebaseDataSource.getAllMeetings()
    }

    override fun getUpcomingMeetings(afterDate: LocalDate): Flow<List<Meeting>> {
        return firebaseDataSource.getAllMeetings().map { meetings ->
            meetings.filter { meeting ->
//                !meeting.dateTime.isBefore(afterDate.atStartOfDay()) &&
                meeting.status != MeetingStatus.COMPLETED
            }.sortedBy { it.dateTime }
        }
    }

    override suspend fun getMeetingById(id: String): Meeting? {
        Log.d("MeetingRepository", "Looking for meeting with id: $id")
        // First try direct lookup by ID
        val meeting = firebaseDataSource.getMeetingById(id)
        if (meeting != null) {
            Log.d("MeetingRepository", "Found meeting directly: $meeting")
            return meeting
        }

        // Fallback to fetching all meetings if direct lookup fails
        Log.d("MeetingRepository", "Meeting not found directly, checking all meetings...")
        val allMeetings = firebaseDataSource.getAllMeetings().first()
        val found = allMeetings.find { it.id == id }
        Log.d("MeetingRepository", if (found != null) "Found meeting in all meetings" else "Meeting not found in all meetings")
        return found
    }

    override suspend fun createMeeting(meeting: Meeting): Resource<Meeting> {
        return try {
            val result = firebaseDataSource.createMeeting(meeting)
            when (result) {
                is Result.Success -> {
                    val newMeeting = result.data
                    firebaseDataSource.sendMeetingNotification(newMeeting)
                    Resource.Success(newMeeting)
                }

                is Result.Error -> {
                    Resource.Error(
                        result.exception.message ?: "Failed to create meeting in Firebase"
                    )
                }

                Result.Loading -> {
                    Resource.Error("Unexpected loading state while creating meeting")
                }
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updateMeeting(meeting: Meeting): Resource<Unit> {
        return try {
            val result = firebaseDataSource.updateMeeting(meeting)
            when (result) {
                is Result.Success -> Resource.Success(Unit)
                is Result.Error -> Resource.Error("Failed to update meeting in Firebase: ${result.exception.message}")
                Result.Loading -> Resource.Error("Unexpected loading state while updating meeting")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun deleteMeeting(id: String): Resource<Unit> {
        return try {
            firebaseDataSource.deleteMeeting(id)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error deleting meeting $id", e)
            Resource.Error(e.message ?: "Failed to delete meeting")
        }
    }

    override suspend fun completeMeeting(meetingId: String): Resource<Unit> {
        return try {
            // First, get the meeting to check if it exists
            val meeting = getMeetingById(meetingId)
            if (meeting == null) {
                return Resource.Error("Meeting not found")
            }

            // Mark the meeting as completed in the data source
            val result = firebaseDataSource.completeMeeting(meetingId)
            when (result) {
                is Result.Success -> Resource.Success(Unit)
                is Result.Error -> Resource.Error("Failed to mark meeting as completed: ${result.exception.message}")
                Result.Loading -> Resource.Error("Unexpected loading state while completing meeting")
            }
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error completing meeting $meetingId", e)
            Resource.Error(e.message ?: "Failed to complete meeting")
        }
    }

    override fun getUpcomingMeetingsWithCounts(afterDate: LocalDate): Flow<List<MeetingWithCounts>> {
        return getUpcomingMeetings(afterDate).map { meetings ->
            meetings.map { meeting ->
                try {
                    // Get all responses for this meeting
                    val responses = memberResponseRepository.getResponsesForMeeting(meeting.id)
                        .first()

                    // Count responses by status
                    val availableCount =
                        responses.count { it.availability == MemberResponse.AvailabilityStatus.AVAILABLE }
                    val notAvailableCount =
                        responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_AVAILABLE }
                    val notConfirmedCount =
                        responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_CONFIRMED }
                    val notResponded =
                        responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_RESPONDED }
                    MeetingWithCounts(
                        meeting = meeting,
                        availableCount = availableCount,
                        notAvailableCount = notAvailableCount,
                        notConfirmedCount = notConfirmedCount,
                        notResponded = notResponded
                    )
                } catch (e: Exception) {
                    Log.e("MeetingRepository", "Error counting responses for meeting ${meeting.id}", e)
                    // Return with zero counts if there's an error
                    MeetingWithCounts(
                        meeting = meeting,
                        availableCount = 0,
                        notAvailableCount = 0,
                        notConfirmedCount = 0,
                        notResponded = 0
                    )
                }
            }
        }
    }

    override suspend fun getPreferredRoles(meetingId: String, userId: String): List<String> {
        return try {
            // Fetch user's preferred roles for this meeting
            // Path: meetings/{meetingId}/availability/{userId}/preferredRoles
            firebaseDataSource.getUserPreferredRoles(meetingId, userId) ?: emptyList()
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error fetching preferred roles for user $userId in meeting $meetingId", e)
            emptyList()
        }
    }

    override suspend fun getMeetingRoles(meetingId: String): List<String> {
        return try {
            // Fetch meeting's preferred roles list
            // Path: meetings/{meetingId}/preferredRoles
            firebaseDataSource.getMeetingPreferredRoles(meetingId) ?: emptyList()
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error fetching meeting roles for meeting $meetingId", e)
            emptyList()
        }
    }

    override suspend fun saveRoleAssignments(
        meetingId: String,
        assignments: List<RoleAssignmentItem>
    ): Result<Unit> {
        return try {
            firebaseDataSource.saveRoleAssignments(meetingId, assignments)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error saving role assignments for meeting $meetingId", e)
            Result.Error(e)
        }
    }

    override suspend fun getAssignedRole(meetingId: String, userId: String): String? {
        return try {
            firebaseDataSource.getAssignedRole(meetingId, userId)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error getting assigned role for user $userId in meeting $meetingId", e)
            null
        }
    }

    override suspend fun getAssignedRoles(meetingId: String, userId: String): List<String> {
        return try {
            firebaseDataSource.getAssignedRoles(meetingId, userId)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error getting assigned roles for user $userId in meeting $meetingId", e)
            emptyList()
        }
    }

    override suspend fun saveSpeakerDetails(
        meetingId: String,
        userId: String,
        speakerDetails: SpeakerDetails
    ): Result<Unit> {
        return try {
            firebaseDataSource.saveSpeakerDetails(meetingId, userId, speakerDetails)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error saving speaker details for user $userId in meeting $meetingId", e)
            Result.Error(e)
        }
    }

    override suspend fun getSpeakerDetails(meetingId: String, userId: String): SpeakerDetails? {
        return try {
            firebaseDataSource.getSpeakerDetails(meetingId, userId)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error getting speaker details for user $userId in meeting $meetingId", e)
            null
        }
    }

    override fun getSpeakerDetailsForMeeting(meetingId: String): Flow<List<SpeakerDetails>> = flow {
        try {
            val details = firebaseDataSource.getSpeakerDetailsForMeeting(meetingId)
            emit(details)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error getting speaker details for meeting $meetingId", e)
            emit(emptyList())
        }
    }

    override suspend fun saveGrammarianDetails(
        meetingId: String,
        userId: String,
        grammarianDetails: GrammarianDetails
    ): Result<Unit> {
        return try {
            firebaseDataSource.saveGrammarianDetails(meetingId, userId, grammarianDetails)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error saving grammarian details for user $userId in meeting $meetingId", e)
            Result.Error(e)
        }
    }

    override suspend fun getGrammarianDetails(
        meetingId: String,
        userId: String
    ): GrammarianDetails? {
        return try {
            firebaseDataSource.getGrammarianDetails(meetingId, userId)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error getting grammarian details for user $userId in meeting $meetingId", e)
            null
        }
    }

    override fun getGrammarianDetailsForMeeting(meetingId: String): Flow<List<GrammarianDetails>> =
        flow {
            try {
                val details = firebaseDataSource.getGrammarianDetailsForMeeting(meetingId)
                emit(details)
            } catch (e: Exception) {
                Log.e("MeetingRepository", "Error getting grammarian details for meeting $meetingId", e)
                emit(emptyList())
            }
        }

    override suspend fun getMemberRolesForMeeting(meetingId: String): List<MemberRole> {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("meetings")
            .document(meetingId)
            .collection("assignedRole")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(MemberRole::class.java)?.copy(id = doc.id)
        }
    }


    override suspend fun updateSpeakerEvaluator(
        meetingId: String,
        speakerId: String,
        evaluatorName: String,
        evaluatorId: String
    ): Result<Unit> {
        return firebaseDataSource.updateSpeakerEvaluator(
            meetingId,
            speakerId,
            evaluatorName,
            evaluatorId
        )
    }

    override suspend fun updateMeetingRoleCounts(
        meetingId: String,
        roleCounts: Map<String, Int>
    ): Result<Unit> {
        return try {
            firebaseDataSource.updateMeetingRoleCounts(meetingId, roleCounts)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getRecentCompletedMeetings(limit: Int): List<Meeting> {
        return try {
            val now = LocalDateTime.now()
            val meetings = firebaseDataSource.getAllMeetings().first()

            Log.d("MeetingRepository", "Total meetings found: ${meetings.size}")
            meetings.forEach { meeting ->
                Log.d("MeetingRepository", "Meeting: ${meeting.id} - Status: ${meeting.status} - Date: ${meeting.dateTime}")
            }

            val completedMeetings = meetings.filter { meeting ->
                val isCompleted = meeting.status == MeetingStatus.COMPLETED
                val isBeforeNow = meeting.dateTime?.let { !it.isAfter(now) } ?: true

                // If meeting is completed, we don't care if it's in the future
                val includeMeeting = isCompleted || isBeforeNow

                Log.d(
                    "MeetingRepository",
                    "Checking meeting ${meeting.id}: " +
                            "status=${meeting.status}, " +
                            "dateTime=${meeting.dateTime}, " +
                            "isCompleted=$isCompleted, " +
                            "isBeforeNow=$isBeforeNow, " +
                            "include=$includeMeeting"
                )

                isCompleted // only completed meetings are included
            }

            Log.d("MeetingRepository", "Found ${completedMeetings.size} completed meetings out of ${meetings.size} total")

            completedMeetings
                .sortedByDescending { it.dateTime } // sort by most recent date
                .take(limit)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error getting recent completed meetings", e)
            emptyList()
        }
    }

    override suspend fun getRoleAssignmentsForMeetings(
        meetingIds: List<String>
    ): Map<String, Map<String, List<String>>> {
        if (meetingIds.isEmpty()) return emptyMap()

        return try {
            coroutineScope {
                meetingIds.map { meetingId ->
                    async {
                        try {
                            val assignments = firebaseDataSource.getAllAssignedRoles(meetingId) // Map<userId, List<role>>
                            if (assignments.isNotEmpty()) meetingId to assignments else null
                        } catch (e: Exception) {
                            Log.e("MeetingRepository", "Error getting assignments for meeting $meetingId", e)
                            null
                        }
                    }
                }.awaitAll()
                    .filterNotNull()
                    .toMap()
            }
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error getting role assignments for meetings", e)
            emptyMap()
        }
    }

    override suspend fun getAllAssignedRoles(meetingId: String): Map<String, List<String>> {
        return try {
            firebaseDataSource.getAllAssignedRoles(meetingId)
        } catch (e: Exception) {
            Log.e("MeetingRepository", "Error getting all assigned roles for meeting $meetingId", e)
            emptyMap()
        }
    }
}
