package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.data.mapper.AgendaItemMapper
import com.bntsoft.toastmasters.domain.model.AgendaItem
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.repository.AgendaRepository
import com.bntsoft.toastmasters.utils.Resource
import com.bntsoft.toastmasters.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgendaTableViewModel @Inject constructor(
    private val agendaRepository: AgendaRepository,
    private val agendaItemMapper: AgendaItemMapper
) : ViewModel() {

    private val _saveStatus = MutableStateFlow<Resource<Unit>?>(null)
    val saveStatus: StateFlow<Resource<Unit>?> = _saveStatus

    private val _agendaItems = MutableStateFlow<Resource<List<AgendaItemDto>>>(Resource.Loading())
    val agendaItems: StateFlow<Resource<List<AgendaItemDto>>> = _agendaItems

    fun saveAgendaItem(dto: AgendaItemDto) {
        viewModelScope.launch {
            _saveStatus.value = Resource.Loading()
            try {
                val item = agendaItemMapper.mapToEntity(dto)
                val result = agendaRepository.saveAgendaItem(item)
                _saveStatus.value = when (result) {
                    is Resource.Success -> Resource.Success(Unit)
                    is Resource.Error -> Resource.Error(result.message ?: "Failed to save agenda item")
                    is Resource.Loading -> Resource.Loading()
                }
            } catch (e: Exception) {
                _saveStatus.value = Resource.Error(e.message ?: "Failed to save agenda item")
            }
        }
    }

    fun saveAllAgendaItems(meetingId: String, dtos: List<AgendaItemDto>) {
        viewModelScope.launch {
            _saveStatus.value = Resource.Loading()
            try {
                val items = dtos.map { agendaItemMapper.mapToEntity(it) }
                val result = agendaRepository.saveAllAgendaItems(meetingId, items)
                _saveStatus.value = when (result) {
                    is Resource.Success -> Resource.Success(Unit)
                    is Resource.Error -> Resource.Error(result.message ?: "Failed to save agenda items")
                    is Resource.Loading -> Resource.Loading()
                }
            } catch (e: Exception) {
                _saveStatus.value = Resource.Error(e.message ?: "Failed to save agenda items")
            }
        }
    }

    fun loadAgendaItems(meetingId: String) {
        viewModelScope.launch {
            _agendaItems.value = Resource.Loading()
            try {
                agendaRepository.getAgendaItems(meetingId).collectLatest { result ->
                    _agendaItems.value = when (result) {
                        is Resource.Success -> {
                            val dtos = result.data?.map { agendaItemMapper.mapFromEntity(it) } ?: emptyList()
                            Resource.Success(dtos)
                        }
                        is Resource.Error -> Resource.Error(result.message ?: "Failed to load agenda items")
                        is Resource.Loading -> Resource.Loading()
                    }
                }
            } catch (e: Exception) {
                _agendaItems.value = Resource.Error(e.message ?: "Failed to load agenda items")
            }
        }
    }

    fun deleteAgendaItem(meetingId: String, itemId: String) {
        viewModelScope.launch {
            _saveStatus.value = Resource.Loading()
            try {
                val result = agendaRepository.deleteAgendaItem(meetingId, itemId)
                _saveStatus.value = result
            } catch (e: Exception) {
                _saveStatus.value = Resource.Error(e.message ?: "Failed to delete item")
            }
        }
    }

    fun reorderItems(meetingId: String, dtos: List<AgendaItemDto>) {
        viewModelScope.launch {
            _saveStatus.value = Resource.Loading()
            try {
                val items = dtos.map { agendaItemMapper.mapToEntity(it) }
                val result = agendaRepository.reorderAgendaItems(meetingId, items)
                _saveStatus.value = when (result) {
                    is Resource.Success -> Resource.Success(Unit)
                    is Resource.Error -> Resource.Error(result.message ?: "Failed to reorder items")
                    is Resource.Loading -> Resource.Loading()
                }
            } catch (e: Exception) {
                _saveStatus.value = Resource.Error(e.message ?: "Failed to reorder items")
            }
        }
    }

    fun clearStatus() {
        _saveStatus.value = null
    }

    suspend fun publishAgenda(meetingId: String): Resource<Unit> {
        return try {
            val result = agendaRepository.updateAgendaStatus(meetingId, AgendaStatus.FINALIZED)
            when (result) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error -> Resource.Error(result.message ?: "Failed to publish agenda")
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to publish agenda")
        }
    }
}
