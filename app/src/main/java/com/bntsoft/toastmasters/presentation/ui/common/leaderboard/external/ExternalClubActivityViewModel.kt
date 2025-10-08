package com.bntsoft.toastmasters.presentation.ui.common.leaderboard.external

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.ExternalClubActivity
import com.bntsoft.toastmasters.domain.repository.ExternalClubActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ExternalClubActivityViewModel @Inject constructor(
    private val repository: ExternalClubActivityRepository,
    private val authRepository: com.bntsoft.toastmasters.domain.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExternalClubActivityUiState())
    val uiState: StateFlow<ExternalClubActivityUiState> = _uiState.asStateFlow()

    private val _activities = MutableStateFlow<List<ExternalClubActivity>>(emptyList())
    val activities: StateFlow<List<ExternalClubActivity>> = _activities.asStateFlow()

    init {
        loadActivities()
    }

    fun loadActivities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                repository.getAllActivities().collect { activitiesList ->
                    _activities.value = activitiesList
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEmpty = activitiesList.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load activities"
                )
            }
        }
    }

    fun refreshActivities() {
        loadActivities()
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            try {
                repository.deleteActivity(activityId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete activity"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getCurrentUserId(): String {
        return runBlocking {
            authRepository.getCurrentUser()?.id ?: ""
        }
    }
}

data class ExternalClubActivityUiState(
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val error: String? = null
)
