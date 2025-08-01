package com.bntsoft.toastmasters.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bntsoft.toastmasters.data.local.entity.MeetingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    // Insert a single meeting
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity)

    // Insert multiple meetings at once
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetings(meetings: List<MeetingEntity>)

    // Update a meeting
    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)

    // Delete a single meeting
    @Delete
    suspend fun deleteMeeting(meeting: MeetingEntity)

    // Delete by meeting ID
    @Query("DELETE FROM meetings WHERE meetingID = :meetingId")
    suspend fun deleteMeetingById(meetingId: Int)

    // Get all meetings (sorted by date ascending)
    @Query("SELECT * FROM meetings ORDER BY date ASC")
    fun getAllMeetings(): Flow<List<MeetingEntity>>

    // Get upcoming meetings only (date >= today)
    @Query("SELECT * FROM meetings WHERE date >= :today ORDER BY date ASC")
    fun getUpcomingMeetings(today: String): Flow<List<MeetingEntity>>

    // Get a specific meeting by ID
    @Query("SELECT * FROM meetings WHERE meetingID = :meetingId LIMIT 1")
    suspend fun getMeetingById(meetingId: Int): MeetingEntity?

    // Get all recurring meetings (e.g., every Saturday)
    @Query("SELECT * FROM meetings WHERE isRecurring = 1 ORDER BY date ASC")
    fun getRecurringMeetings(): Flow<List<MeetingEntity>>

    // Delete all recurring meetings
    @Query("DELETE FROM meetings WHERE isRecurring = 1")
    suspend fun deleteAllRecurringMeetings()
}