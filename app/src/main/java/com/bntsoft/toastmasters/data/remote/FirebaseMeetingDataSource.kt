package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.domain.model.Meeting
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FirebaseMeetingDataSource {

    fun getAllMeetings(): Flow<List<Meeting>>

    fun getUpcomingMeetings(afterDate: LocalDate = LocalDate.now()): Flow<List<Meeting>>

    suspend fun getMeetingById(id: Int): Meeting?

    suspend fun createMeeting(meeting: Meeting): Result<Unit>

    suspend fun updateMeeting(meeting: Meeting): Result<Unit>

    suspend fun deleteMeeting(id: Int): Result<Unit>

    suspend fun sendMeetingNotification(meeting: Meeting): Result<Unit>
}
