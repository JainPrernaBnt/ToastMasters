package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSource
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.model.RoleAssignmentItem
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.utils.Resource
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
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
                !meeting.dateTime.isBefore(afterDate.atStartOfDay())
            }.sortedBy { it.dateTime }
        }
    }

    override suspend fun getMeetingById(id: String): Meeting? {
        Timber.d("Looking for meeting with id: $id")
        // First try direct lookup by ID
        val meeting = firebaseDataSource.getMeetingById(id)
        if (meeting != null) {
            Timber.d("Found meeting directly: $meeting")
            return meeting
        }

        // Fallback to fetching all meetings if direct lookup fails
        Timber.d("Meeting not found directly, checking all meetings...")
        val allMeetings = firebaseDataSource.getAllMeetings().first()
        val found = allMeetings.find { it.id == id }
        Timber.d(if (found != null) "Found meeting in all meetings" else "Meeting not found in all meetings")
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
            Timber.e(e, "Error deleting meeting $id")
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
            Timber.e(e, "Error completing meeting $meetingId")
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
                    Timber.e(e, "Error counting responses for meeting ${meeting.id}")
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
            Timber.e(e, "Error fetching preferred roles for user $userId in meeting $meetingId")
            emptyList()
        }
    }

    override suspend fun getMeetingRoles(meetingId: String): List<String> {
        return try {
            // Fetch meeting's preferred roles list
            // Path: meetings/{meetingId}/preferredRoles
            firebaseDataSource.getMeetingPreferredRoles(meetingId) ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error fetching meeting roles for meeting $meetingId")
            emptyList()
        }
    }
    
    override suspend fun saveRoleAssignments(meetingId: String, assignments: List<RoleAssignmentItem>): Result<Unit> {
        return try {
            firebaseDataSource.saveRoleAssignments(meetingId, assignments)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving role assignments for meeting $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun getAssignedRole(meetingId: String, userId: String): String? {
        return try {
            firebaseDataSource.getAssignedRole(meetingId, userId)
        } catch (e: Exception) {
            Timber.e(e, "Error getting assigned role for user $userId in meeting $meetingId")
            null
        }
    }
    
    override suspend fun getAssignedRoles(meetingId: String, userId: String): List<String> {
        return try {
            firebaseDataSource.getAssignedRoles(meetingId, userId)
        } catch (e: Exception) {
            Timber.e(e, "Error getting assigned roles for user $userId in meeting $meetingId")
            emptyList()
        }
    }
}

