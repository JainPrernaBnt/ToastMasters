package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.data.remote.FirebaseAgendaDataSource
import com.bntsoft.toastmasters.data.remote.FirebaseMeetingDataSource
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.domain.repository.AuthRepository
import com.bntsoft.toastmasters.domain.repository.MeetingAgendaRepository
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MeetingAgendaRepositoryImpl @Inject constructor(
    private val agendaDataSource: FirebaseAgendaDataSource,
    private val meetingDataSource: FirebaseMeetingDataSource,
    private val authRepository: AuthRepository
) : MeetingAgendaRepository {

    override fun getMeetingAgenda(meetingId: String): Flow<Result<MeetingAgenda>> {
        return agendaDataSource.observeMeetingAgenda(meetingId)
    }

    override suspend fun saveMeetingAgenda(agenda: MeetingAgenda): Result<Unit> {
        return agendaDataSource.saveMeetingAgenda(agenda)
    }

    override suspend fun updateAgendaStatus(meetingId: String, status: AgendaStatus): Result<Unit> {
        return agendaDataSource.updateAgendaStatus(meetingId, status)
    }

    override fun observeMeetingAgenda(meetingId: String): Flow<Result<MeetingAgenda>> {
        return agendaDataSource.observeMeetingAgenda(meetingId)
    }

    override suspend fun getGrammarianDetails(meetingId: String): GrammarianDetails {
        return try {
            // Get all grammarian details for the meeting
            val detailsList = meetingDataSource.getGrammarianDetailsForMeeting(meetingId)
            
            // Return the first one found, or a new one if none exist
            detailsList.firstOrNull() ?: GrammarianDetails(meetingID = meetingId)
        } catch (e: Exception) {
            GrammarianDetails(meetingID = meetingId)
        }
    }

    override suspend fun saveGrammarianDetails(meetingId: String, details: GrammarianDetails): Result<Unit> {
        return try {
            // Get the existing details to preserve the original userId
            val existingDetails = getGrammarianDetails(meetingId)
            
            // If we have existing details, use the original user ID, otherwise use current user
            val grammarianUserId = if (existingDetails.userId.isNotBlank()) {
                existingDetails.userId
            } else {
                getCurrentUserId()
            }
            
            // Create updated details with preserved user ID and meeting ID
            val updatedDetails = details.copy(
                meetingID = meetingId,
                userId = grammarianUserId
            )
            
            // Save the details using the grammarian's user ID as the document ID
            val result = meetingDataSource.saveGrammarianDetails(
                meetingId = meetingId,
                userId = grammarianUserId, // Use the grammarian's user ID as the document ID
                updatedDetails
            )
            
            if (result is Result.Success) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to save grammarian details"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateMeetingOfficers(agenda: MeetingAgenda): Result<Unit> {
        return try {
            // Update the meeting with new officers and club info
            val result = meetingDataSource.updateMeeting(agenda.meeting)
            if (result is Result.Success) {
                // Also update the agenda's last updated timestamp
                val updatedAgenda = agenda.copy(
                    meeting = agenda.meeting.copy(
                        updatedAt = System.currentTimeMillis()
                    )
                )
                agendaDataSource.saveMeetingAgenda(updatedAgenda)
            } else {
                result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getCurrentUserId(): String {
        return authRepository.getCurrentUser()?.id ?: throw IllegalStateException("No user is currently logged in")
    }
    
    override suspend fun saveAbbreviations(meetingId: String, agendaId: String, abbreviations: Map<String, String>): Result<Unit> {
        return agendaDataSource.saveAbbreviations(meetingId, agendaId, abbreviations)
    }
    
    override suspend fun getClubInformation(): Result<Map<String, Any>> {
        return agendaDataSource.getClubInformation()
    }
    
    override suspend fun saveClubInformation(clubInfo: Map<String, Any>): Result<Unit> {
        return agendaDataSource.saveClubInformation(clubInfo)
    }
    
    override suspend fun getClubOfficers(): Result<Map<String, String>> {
        return agendaDataSource.getClubOfficers()
    }
    
    override suspend fun saveClubOfficers(officers: Map<String, String>): Result<Unit> {
        return agendaDataSource.saveClubOfficers(officers)
    }

}
