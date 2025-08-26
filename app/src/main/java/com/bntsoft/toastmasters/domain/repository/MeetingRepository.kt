package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.model.RoleAssignmentItem
import com.bntsoft.toastmasters.utils.Resource
import kotlinx.coroutines.flow.Flow
import com.bntsoft.toastmasters.utils.Result
import java.time.LocalDate

interface MeetingRepository {
    // Basic meeting operations
    fun getAllMeetings(): Flow<List<Meeting>>
    fun getUpcomingMeetings(afterDate: LocalDate = LocalDate.now()): Flow<List<Meeting>>
    suspend fun getMeetingById(id: String): Meeting?
    suspend fun createMeeting(meeting: Meeting): Resource<Meeting>
    suspend fun updateMeeting(meeting: Meeting): Resource<Unit>
    suspend fun deleteMeeting(id: String): Resource<Unit>
    suspend fun completeMeeting(meetingId: String): Resource<Unit>
    fun getUpcomingMeetingsWithCounts(afterDate: LocalDate = LocalDate.now()): Flow<List<MeetingWithCounts>>

//    Assign Roles
    suspend fun getPreferredRoles(meetingId: String, userId: String): List<String>
    suspend fun getMeetingRoles(meetingId: String): List<String>
    suspend fun saveRoleAssignments(meetingId: String, assignments: List<RoleAssignmentItem>): Result<Unit>
}
