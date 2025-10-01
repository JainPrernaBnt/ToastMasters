package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow
import com.bntsoft.toastmasters.domain.model.AgendaStatus

interface MeetingAgendaRepository {
    // Meeting Agenda methods
    fun getMeetingAgenda(meetingId: String): Flow<Result<MeetingAgenda>>
    suspend fun saveMeetingAgenda(agenda: MeetingAgenda): Result<Unit>
    suspend fun updateAgendaStatus(meetingId: String, status: AgendaStatus): Result<Unit>
    fun observeMeetingAgenda(meetingId: String): Flow<Result<MeetingAgenda>>
    
    // Grammarian Details methods
    suspend fun getGrammarianDetails(meetingId: String): GrammarianDetails
    suspend fun saveGrammarianDetails(meetingId: String, details: GrammarianDetails): Result<Unit>
    
    // Utility methods
    suspend fun getCurrentUserId(): String
    
    suspend fun updateMeetingOfficers(agenda: MeetingAgenda): Result<Unit>

    suspend fun saveAbbreviations(meetingId: String, agendaId: String, abbreviations: Map<String, String>): Result<Unit>
    
    // Club Information methods
    suspend fun getClubInformation(): Result<Map<String, Any>>
    suspend fun saveClubInformation(clubInfo: Map<String, Any>): Result<Unit>
    
    // Club Officers methods
    suspend fun getClubOfficers(): Result<Map<String, String>>
    suspend fun saveClubOfficers(officers: Map<String, String>): Result<Unit>
}
