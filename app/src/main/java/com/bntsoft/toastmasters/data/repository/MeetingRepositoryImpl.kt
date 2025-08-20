package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSource
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.utils.Resource
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
        return firebaseDataSource.getAllMeetings()
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
            if (result.isSuccess) {
                val newMeeting = result.getOrThrow()
                firebaseDataSource.sendMeetingNotification(newMeeting)
                Resource.Success(newMeeting)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Failed to create meeting in Firebase")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updateMeeting(meeting: Meeting): Resource<Unit> {
        return try {
            val result = firebaseDataSource.updateMeeting(meeting)
            if (result.isSuccess) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update meeting in Firebase")
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
            Resource.Error(e.message ?: "Failed to delete meeting")
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
                    val availableCount = responses.count { it.availability == MemberResponse.AvailabilityStatus.AVAILABLE }
                    val notAvailableCount = responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_AVAILABLE }
                    val notConfirmedCount = responses.count { it.availability == MemberResponse.AvailabilityStatus.NOT_CONFIRMED }
                    
                    MeetingWithCounts(
                        meeting = meeting,
                        availableCount = availableCount,
                        notAvailableCount = notAvailableCount,
                        notConfirmedCount = notConfirmedCount
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error counting responses for meeting ${meeting.id}")
                    // Return with zero counts if there's an error
                    MeetingWithCounts(
                        meeting = meeting,
                        availableCount = 0,
                        notAvailableCount = 0,
                        notConfirmedCount = 0
                    )
                }
            }
        }
    }

    // Sync function is no longer needed as we are not using a local cache
    override suspend fun syncMeetings(): Resource<Unit> {
        return Resource.Success(Unit) // Does nothing now
    }
}

