package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.model.SpeakerDetails
import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.RoleAssignmentItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FirebaseMeetingDataSource {

    fun getAllMeetings(): Flow<List<Meeting>>

    fun getUpcomingMeetings(afterDate: LocalDate = LocalDate.now()): Flow<List<Meeting>>

    suspend fun getMeetingById(id: String): Meeting?

    suspend fun createMeeting(meeting: Meeting): Result<Meeting>

    suspend fun updateMeeting(meeting: Meeting): Result<Unit>

    suspend fun deleteMeeting(id: String): Result<Unit>

    suspend fun completeMeeting(meetingId: String): Result<Unit>

    suspend fun sendMeetingNotification(meeting: Meeting): Result<Unit>

    suspend fun getUserPreferredRoles(meetingId: String, userId: String): List<String>?

    suspend fun getMeetingPreferredRoles(meetingId: String): List<String>?
    
    suspend fun saveRoleAssignments(meetingId: String, assignments: List<RoleAssignmentItem>): Result<Unit>
    
    suspend fun getAssignedRole(meetingId: String, userId: String): String?
    
    suspend fun getAssignedRoles(meetingId: String, userId: String): List<String>

    suspend fun getAllAssignedRoles(meetingId: String): Map<String, List<String>>
    
    // Speaker details methods
    suspend fun saveSpeakerDetails(meetingId: String, userId: String, speakerDetails: SpeakerDetails): Result<Unit>
    suspend fun getSpeakerDetails(meetingId: String, userId: String): SpeakerDetails?
    suspend fun getSpeakerDetailsForMeeting(meetingId: String): List<SpeakerDetails>

    // Grammarian details methods
    suspend fun saveGrammarianDetails(meetingId: String, userId: String, grammarianDetails: GrammarianDetails): Result<Unit>
    suspend fun getGrammarianDetails(meetingId: String, userId: String): GrammarianDetails?
    suspend fun getGrammarianDetailsForMeeting(meetingId: String): List<GrammarianDetails>
    
    // Member roles methods
    suspend fun getMemberRolesForMeeting(meetingId: String): List<Pair<String, List<String>>>

    suspend fun updateSpeakerEvaluator(meetingId: String, speakerId: String, evaluatorName: String, evaluatorId: String): Result<Unit>

    suspend fun updateMeetingRoleCounts(meetingId: String, roleCounts: Map<String, Int>)
}
