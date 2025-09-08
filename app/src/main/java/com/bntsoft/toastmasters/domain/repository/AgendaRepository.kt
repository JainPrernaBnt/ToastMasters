package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.AgendaItem
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AgendaRepository {
    
    // Meeting Agenda operations
    suspend fun getMeetingAgenda(meetingId: String): Flow<Resource<MeetingAgenda>>
    suspend fun createOrUpdateMeetingAgenda(agenda: MeetingAgenda): Resource<Unit>
    suspend fun updateAgendaStatus(meetingId: String, status: AgendaStatus): Resource<Unit>
    
    // Agenda Item operations
    suspend fun getAgendaItem(meetingId: String, itemId: String): Resource<AgendaItem>
    suspend fun getAgendaItems(meetingId: String): Flow<Resource<List<AgendaItem>>>
    suspend fun saveAgendaItem(item: AgendaItem): Resource<String> // Returns item ID
    suspend fun deleteAgendaItem(meetingId: String, itemId: String): Resource<Unit>
    suspend fun reorderAgendaItems(meetingId: String, items: List<AgendaItem>): Resource<Unit>
    
    // Batch operations
    suspend fun saveAllAgendaItems(meetingId: String, items: List<AgendaItem>): Resource<Unit>
    
    // Status updates
    fun observeAgendaStatus(meetingId: String): Flow<AgendaStatus>
}
