package com.bntsoft.toastmasters.presentation.ui.vp.clubmembers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClubMembersUiState(
    val isLoading: Boolean = true,
    val members: List<User> = emptyList(),
    val error: String? = null,
    val currentUser: User? = null
)

@HiltViewModel
class ClubMembersViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClubMembersUiState())
    val uiState: StateFlow<ClubMembersUiState> = _uiState.asStateFlow()

    init {
        loadClubMembers()
    }

    private fun loadClubMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val currentUser = userRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "Unable to get current user information"
                        ) 
                    }
                    return@launch
                }

                if (currentUser.clubId.isEmpty()) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "No club ID found for current user"
                        ) 
                    }
                    return@launch
                }

                val members = userRepository.getClubMembers(currentUser.clubId)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        members = members,
                        currentUser = currentUser
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "An error occurred while loading club members"
                    ) 
                }
            }
        }
    }

    fun refreshMembers() {
        loadClubMembers()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
