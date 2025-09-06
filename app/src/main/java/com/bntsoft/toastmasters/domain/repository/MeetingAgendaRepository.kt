package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow
import com.bntsoft.toastmasters.domain.model.AgendaStatus // Make sure this exists

interface MeetingAgendaRepository {
    fun getMeetingAgenda(meetingId: String): Flow<Result<MeetingAgenda>>
    suspend fun saveMeetingAgenda(agenda: MeetingAgenda): Result<Unit>
    suspend fun updateAgendaStatus(meetingId: String, status: AgendaStatus): Result<Unit>
    fun observeMeetingAgenda(meetingId: String): Flow<Result<MeetingAgenda>>
}
