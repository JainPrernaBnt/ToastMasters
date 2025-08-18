package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSource
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseMeetingDataSource
) : MeetingRepository {

    override fun getAllMeetings(): Flow<List<Meeting>> {
        return firebaseDataSource.getAllMeetings()
    }

    override fun getUpcomingMeetings(afterDate: LocalDate): Flow<List<Meeting>> {
        // Filter the full list from Firebase
        return firebaseDataSource.getAllMeetings().map { meetings ->
            meetings.filter { it.dateTime.toLocalDate().isAfter(afterDate) || it.dateTime.toLocalDate().isEqual(afterDate) }
        }
    }

    override suspend fun getMeetingById(id: String): Meeting? {
        // Fetch all and find the specific meeting
        return firebaseDataSource.getAllMeetings().first().find { it.id == id }
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

    // Sync function is no longer needed as we are not using a local cache
    override suspend fun syncMeetings(): Resource<Unit> {
        return Resource.Success(Unit) // Does nothing now
    }
}

