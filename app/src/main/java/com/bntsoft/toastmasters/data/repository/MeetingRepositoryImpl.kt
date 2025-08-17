package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.local.dao.MeetingDao
import com.bntsoft.toastmasters.data.mapper.MeetingDomainMapper
import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSource
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val meetingDao: MeetingDao,
    private val firebaseDataSource: FirebaseMeetingDataSource,
    private val mapper: MeetingDomainMapper
) : MeetingRepository {

    // Cache the last sync timestamp to avoid unnecessary syncs
    private var lastSyncTime: Long = 0
    private val SYNC_INTERVAL = 30 * 60 * 1000 // 30 minutes in milliseconds

    override fun getAllMeetings(): Flow<List<Meeting>> {
        // 1. Return Room data as Flow
        val localFlow = meetingDao.getAllMeetings()
            .map { entities -> entities.map { mapper.mapToDomain(it) } }

        // 2. Launch network sync in background without blocking return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (shouldSync()) {
                    syncMeetings()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync meetings")
            }
        }

        return localFlow
    }

    override fun getUpcomingMeetings(afterDate: LocalDate): Flow<List<Meeting>> {
        val localFlow = meetingDao.getUpcomingMeetings(afterDate.toString())
            .map { entities -> entities.map { mapper.mapToDomain(it) } }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (shouldSync()) {
                    syncMeetings()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync upcoming meetings")
            }
        }

        return localFlow
    }


    override suspend fun getMeetingById(id: String): Meeting? {
        return withContext(Dispatchers.IO) {
            // First try to get from local database
            val localMeeting =
                meetingDao.getMeetingById(id.toIntOrNull() ?: return@withContext null)
                    ?.let { mapper.mapToDomain(it) }

            // If not found locally or sync is needed, try to sync
            if (localMeeting == null || shouldSync()) {
                syncMeetings()
                // Try to get again after sync
                meetingDao.getMeetingById(id.toIntOrNull() ?: return@withContext null)
                    ?.let { mapper.mapToDomain(it) }
            } else {
                localMeeting
            }
        }
    }

    override suspend fun createMeeting(meeting: Meeting): Resource<Unit> {
        return try {
            // First, save to Firebase
            val result = firebaseDataSource.createMeeting(meeting)

            if (result.isSuccess) {
                // If Firebase save is successful, save to local database
                val entity = mapper.mapToEntity(meeting)
                meetingDao.insertMeeting(entity)

                // Send notification to all members
                firebaseDataSource.sendMeetingNotification(meeting)

                // Update last sync time
                lastSyncTime = System.currentTimeMillis()
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to create meeting in Firebase")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updateMeeting(meeting: Meeting): Resource<Unit> {
        return try {
            // First, update in Firebase
            val result = firebaseDataSource.updateMeeting(meeting)

            if (result.isSuccess) {
                // If Firebase update is successful, update in local database
                val entity = mapper.mapToEntity(meeting)
                meetingDao.updateMeeting(entity)

                // Update last sync time
                lastSyncTime = System.currentTimeMillis()
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
            // First, delete from Firebase
            val result = firebaseDataSource.deleteMeeting(id)

            if (result.isSuccess) {
                // If Firebase deletion is successful, delete from local database
                meetingDao.deleteMeetingById(
                    id.toIntOrNull() ?: return Resource.Error("Invalid meeting ID")
                )

                // Update last sync time
                lastSyncTime = System.currentTimeMillis()
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to delete meeting from Firebase")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun syncMeetings(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get all meetings from Firebase
            val remoteMeetings = firebaseDataSource.getAllMeetings()
                .first() // Get the first emission

            // Convert to entities
            val entities = remoteMeetings.map { mapper.mapToEntity(it) }

            // Replace all local meetings with the remote ones
            meetingDao.insertMeetings(entities)

            // Update last sync time
            lastSyncTime = System.currentTimeMillis()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to sync meetings")
        }
    }

    private fun shouldSync(): Boolean {
        return (System.currentTimeMillis() - lastSyncTime) > SYNC_INTERVAL
    }
}
