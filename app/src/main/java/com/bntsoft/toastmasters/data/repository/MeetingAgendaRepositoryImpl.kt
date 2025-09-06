package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.remote.FirebaseAgendaDataSource
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.repository.MeetingAgendaRepository
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MeetingAgendaRepositoryImpl @Inject constructor(
    private val agendaDataSource: FirebaseAgendaDataSource
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
}
