package com.bntsoft.toastmasters.data.remote

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
    suspend fun saveAgendaItem(meetingId: String, item: AgendaItem): Result<String>
    suspend fun deleteAgendaItem(meetingId: String, itemId: String): Result<Unit>
    suspend fun reorderAgendaItems(meetingId: String, items: List<AgendaItem>): Result<Unit>
    
    // Batch operations
    suspend fun saveAllAgendaItems(meetingId: String, items: List<AgendaItem>): Result<Unit>
    
    // Status updates
    fun observeAgendaStatus(meetingId: String): Flow<AgendaStatus>
}
