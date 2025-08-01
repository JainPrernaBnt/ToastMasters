package com.bntsoft.toastmasters.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bntsoft.toastmasters.data.local.entity.MeetingAvailability
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingAvailabilityDao {
    // Insert availability (when member submits response)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvailability(availability: MeetingAvailability)

    // Update availability
    @Update
    suspend fun updateAvailability(availability: MeetingAvailability)

    // Get all availability submissions for a meeting
    @Query("""SELECT * FROM meeting_availability WHERE meetingId = :meetingId ORDER BY submittedOn DESC""")

    fun getAvailabilityForMeeting(meetingId: Int): Flow<List<MeetingAvailability>>

    // Get a specific member's availability for a meeting
    @Query(""" SELECT * FROM meeting_availability WHERE meetingId = :meetingId AND userId = :userId LIMIT 1""")
    suspend fun getMemberAvailability(meetingId: Int, userId: Int): MeetingAvailability?

    // Get all meetings a member has submitted availability for
    @Query("SELECT * FROM meeting_availability WHERE userId = :userId")
    fun getAvailabilityByUser(userId: Int): Flow<List<MeetingAvailability>>

    // Delete availability for a meeting (e.g., if meeting is deleted)
    @Query("DELETE FROM meeting_availability WHERE meetingId = :meetingId")
    suspend fun deleteAvailabilityForMeeting(meetingId: Int)

}