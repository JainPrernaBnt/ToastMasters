package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.model.Abbreviations
import com.bntsoft.toastmasters.data.model.ClubInfo
import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.domain.model.AgendaItem
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow

interface FirebaseAgendaDataSource {
    
    // Meeting Agenda operations
    suspend fun getMeetingAgenda(meetingId: String): Result<MeetingAgenda>
    suspend fun saveMeetingAgenda(agenda: MeetingAgenda): Result<Unit>
    suspend fun updateAgendaStatus(meetingId: String, status: AgendaStatus): Result<Unit>
    fun observeMeetingAgenda(meetingId: String): Flow<Result<MeetingAgenda>>
    
    // Agenda Item operations
    suspend fun getAgendaItem(meetingId: String, itemId: String): Result<AgendaItem>
    suspend fun getAgendaItems(meetingId: String): List<AgendaItem>
    fun observeAgendaItems(meetingId: String): Flow<List<AgendaItem>>
    suspend fun saveAgendaItem(meetingId: String, item: AgendaItem): Result<String>
    suspend fun deleteAgendaItem(meetingId: String, itemId: String): Result<Unit>
    suspend fun reorderAgendaItems(meetingId: String, items: List<AgendaItem>): Result<Unit>
    
    // Batch operations
    suspend fun saveAllAgendaItems(meetingId: String, items: List<AgendaItem>): Result<Unit>
    
    // Status updates
    fun observeAgendaStatus(meetingId: String): Flow<AgendaStatus>
    
    // Grammarian Details
    suspend fun getGrammarianDetails(meetingId: String, userId: String): GrammarianDetails?
    suspend fun saveGrammarianDetails(meetingId: String, userId: String, details: GrammarianDetails): Result<Unit>
    suspend fun getGrammarianDetailsForMeeting(meetingId: String): List<GrammarianDetails>
    
    // Abbreviation operations
    suspend fun getAbbreviations(meetingId: String, agendaId: String): Abbreviations
    suspend fun saveAbbreviations(meetingId: String, agendaId: String, abbreviations: Map<String, String>): Result<Unit>
    suspend fun deleteAbbreviation(meetingId: String, agendaId: String, abbreviationKey: String): Result<Unit>
    
    // Club Information operations
    suspend fun getClubInformation(): Result<Map<String, Any>>
    suspend fun saveClubInformation(clubInfo: Map<String, Any>): Result<Unit>
    
    // Club Officers operations
    suspend fun getClubOfficers(): Result<Map<String, String>>
    suspend fun saveClubOfficers(officers: Map<String, String>): Result<Unit>

}
