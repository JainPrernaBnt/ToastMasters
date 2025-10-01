package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.utils.Result

interface AbbreviationRepository {
    suspend fun getAbbreviations(meetingId: String, agendaId: String): Map<String, String>
    suspend fun saveAbbreviations(meetingId: String, agendaId: String, abbreviations: Map<String, String>): Result<Unit>
    suspend fun deleteAbbreviation(meetingId: String, agendaId: String, abbreviationKey: String): Result<Unit>
}
