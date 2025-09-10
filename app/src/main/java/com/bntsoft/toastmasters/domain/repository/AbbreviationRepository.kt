package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.data.model.Abbreviations
import com.bntsoft.toastmasters.utils.Result

interface AbbreviationRepository {
    suspend fun getAbbreviations(meetingId: String, agendaId: String): Abbreviations
    suspend fun saveAbbreviations(meetingId: String, agendaId: String, abbreviations: Abbreviations): Result<Unit>
    suspend fun deleteAbbreviation(meetingId: String, agendaId: String, abbreviationKey: String): Result<Unit>
}
