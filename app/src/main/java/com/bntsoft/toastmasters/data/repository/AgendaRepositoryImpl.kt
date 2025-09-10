package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.remote.FirebaseAgendaDataSource
import com.bntsoft.toastmasters.domain.model.AgendaItem
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.domain.repository.AgendaRepository
import com.bntsoft.toastmasters.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgendaRepositoryImpl @Inject constructor(
    private val agendaDataSource: FirebaseAgendaDataSource
) : AgendaRepository {

    override suspend fun getMeetingAgenda(meetingId: String): Flow<Resource<MeetingAgenda>> {
        return agendaDataSource.observeMeetingAgenda(meetingId)
            .map { result ->
                mapResult(result) { it.message ?: "Failed to load meeting agenda" }
            }
    }

    override suspend fun createOrUpdateMeetingAgenda(agenda: MeetingAgenda): Resource<Unit> {
        return mapResult(
            result = agendaDataSource.saveMeetingAgenda(agenda),
            onError = { it.message ?: "Failed to save meeting agenda" }
        )
    }

    override suspend fun updateAgendaStatus(
        meetingId: String,
        status: AgendaStatus
    ): Resource<Unit> {
        return mapResult(
            result = agendaDataSource.updateAgendaStatus(meetingId, status),
            onError = { it.message ?: "Failed to update agenda status" }
        )
    }

    override suspend fun getAgendaItem(meetingId: String, itemId: String): Resource<AgendaItem> {
        return mapResult(
            result = agendaDataSource.getAgendaItem(meetingId, itemId),
            onError = { it.message ?: "Failed to get agenda item" }
        )
    }

    override suspend fun getAgendaItems(meetingId: String): Flow<Resource<List<AgendaItem>>> {
        return flow {
            try {
                // Get initial list
                val initial = agendaDataSource.getAgendaItems(meetingId)
                emit(Resource.Success(initial))

                // Observe for changes
                var firstEmission = true
                agendaDataSource.observeAgendaItems(meetingId).collect { items ->
                    if (firstEmission) {
                        firstEmission = false
                        // Ignore first empty emission if we already have items
                        if (initial.isNotEmpty() && items.isEmpty()) {
                            return@collect
                        }
                    }
                    emit(Resource.Success(items))
                }
            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Failed to load agenda items"))
            }
        }
    }

    override suspend fun saveAgendaItem(item: AgendaItem): Resource<String> {
        return mapResult(
            result = agendaDataSource.saveAgendaItem(item.meetingId, item),
            onError = { it.message ?: "Failed to save agenda item" }
        )
    }

    override suspend fun deleteAgendaItem(meetingId: String, itemId: String): Resource<Unit> {
        return mapResult(
            result = agendaDataSource.deleteAgendaItem(meetingId, itemId),
            onError = { it.message ?: "Failed to delete agenda item" }
        )
    }

    override suspend fun reorderAgendaItems(
        meetingId: String,
        items: List<AgendaItem>
    ): Resource<Unit> {
        return mapResult(
            result = agendaDataSource.reorderAgendaItems(meetingId, items),
            onError = { it.message ?: "Failed to reorder agenda items" }
        )
    }

    override suspend fun saveAllAgendaItems(
        meetingId: String,
        items: List<AgendaItem>
    ): Resource<Unit> {
        return mapResult(
            result = agendaDataSource.saveAllAgendaItems(meetingId, items),
            onError = { it.message ?: "Failed to save all agenda items" }
        )
    }

    override fun observeAgendaStatus(meetingId: String): Flow<AgendaStatus> {
        return agendaDataSource.observeAgendaStatus(meetingId)
    }

    // Helper function to map between different Result types
    private fun <T> mapResult(
        result: com.bntsoft.toastmasters.utils.Result<T>,
        onError: (Exception) -> String = { it.message ?: "An unknown error occurred" }
    ): Resource<T> {
        return when (result) {
            is com.bntsoft.toastmasters.utils.Result.Success -> Resource.Success(result.data)
            is com.bntsoft.toastmasters.utils.Result.Error -> Resource.Error(onError(result.exception))
            is com.bntsoft.toastmasters.utils.Result.Loading -> Resource.Loading()
        }
    }
}
