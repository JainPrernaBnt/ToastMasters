package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.remote.FirebaseAgendaDataSource
import com.bntsoft.toastmasters.domain.repository.AbbreviationRepository
import com.bntsoft.toastmasters.utils.Result
import javax.inject.Inject

class AbbreviationRepositoryImpl @Inject constructor(
    private val firebaseAgendaDataSource: FirebaseAgendaDataSource
) : AbbreviationRepository {

    override suspend fun getAbbreviations(meetingId: String, agendaId: String): Map<String, String> {
        return firebaseAgendaDataSource.getAbbreviations(meetingId, agendaId)
    }

    override suspend fun saveAbbreviations(
        meetingId: String,
        agendaId: String,
        abbreviations: Map<String, String>
    ): Result<Unit> {
        return firebaseAgendaDataSource.saveAbbreviations(meetingId, agendaId, abbreviations)
    }

    override suspend fun deleteAbbreviation(
        meetingId: String,
        agendaId: String,
        abbreviationKey: String
    ): Result<Unit> {
        return firebaseAgendaDataSource.deleteAbbreviation(meetingId, agendaId, abbreviationKey)
    }
}
