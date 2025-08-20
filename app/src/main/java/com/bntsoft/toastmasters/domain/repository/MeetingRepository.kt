package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.utils.Resource
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MeetingRepository {
    // Get all meetings
    fun getAllMeetings(): Flow<List<Meeting>>
    
    // Get upcoming meetings (after or equal to the given date)
    fun getUpcomingMeetings(afterDate: LocalDate = LocalDate.now()): Flow<List<Meeting>>
    
    // Get a meeting by ID
    suspend fun getMeetingById(id: String): Meeting?
    
    // Create a new meeting
    suspend fun createMeeting(meeting: Meeting): Resource<Meeting>
    
    // Update an existing meeting
    suspend fun updateMeeting(meeting: Meeting): Resource<Unit>
    
    // Delete a meeting
    suspend fun deleteMeeting(id: String): Resource<Unit>
    
    // Get upcoming meetings with response counts
    fun getUpcomingMeetingsWithCounts(afterDate: LocalDate = LocalDate.now()): Flow<List<MeetingWithCounts>>
    
    // Sync meetings with remote data source
    suspend fun syncMeetings(): Resource<Unit>
}
