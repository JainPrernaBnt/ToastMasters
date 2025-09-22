package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.data.model.SpeakerDetails
import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.data.model.MemberRole
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
    
    suspend fun getAssignedRole(meetingId: String, userId: String): String?
    suspend fun getAssignedRoles(meetingId: String, userId: String): List<String>
    suspend fun getAllAssignedRoles(meetingId: String): Map<String, List<String>>
    
    // Speaker details
    suspend fun saveSpeakerDetails(meetingId: String, userId: String, speakerDetails: SpeakerDetails): Result<Unit>
    suspend fun getSpeakerDetails(meetingId: String, userId: String): SpeakerDetails?
    fun getSpeakerDetailsForMeeting(meetingId: String): Flow<List<SpeakerDetails>>

    // Grammarian details
    suspend fun saveGrammarianDetails(meetingId: String, userId: String, grammarianDetails: GrammarianDetails): Result<Unit>
    suspend fun getGrammarianDetails(meetingId: String, userId: String): GrammarianDetails?
    fun getGrammarianDetailsForMeeting(meetingId: String): Flow<List<GrammarianDetails>>

    suspend fun getMemberRolesForMeeting(meetingId: String): List<MemberRole>

    suspend fun updateSpeakerEvaluator(meetingId: String, speakerId: String, evaluatorName: String, evaluatorId: String): Result<Unit>
    suspend fun updateSpeakerEvaluators(meetingId: String, speakerId: String, evaluatorIds: List<String>): Result<Unit>
    suspend fun updateMeetingRoleCounts(meetingId: String, roleCounts: Map<String, Int>): Result<Unit>
    
    // Recent roles functionality
    suspend fun getRecentCompletedMeetings(limit: Int = 3): List<Meeting>
    suspend fun getRoleAssignmentsForMeetings(
        meetingIds: List<String>
    ): Map<String, Map<String, List<String>>>
}
