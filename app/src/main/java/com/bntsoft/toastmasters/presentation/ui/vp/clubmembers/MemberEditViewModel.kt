package com.bntsoft.toastmasters.presentation.ui.vp.clubmembers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.bntsoft.toastmasters.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val member: User? = null,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MemberEditViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberEditUiState())
    val uiState: StateFlow<MemberEditUiState> = _uiState.asStateFlow()

    fun loadMember(memberId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = userRepository.getUserById(memberId)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            member = result.data
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = result.exception.message ?: "Failed to load member details"
                        ) 
                    }
                }

                Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }

            }
        }
    }

    fun updateMember(
        name: String,
        email: String,
        phoneNumber: String,
        address: String,
        toastmastersId: String,
        gender: String,
        level: String?,
        mentorNames: List<String>
    ) {
        val currentMember = _uiState.value.member ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            val updatedMember = currentMember.copy(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                address = address,
                toastmastersId = toastmastersId,
                gender = gender,
                level = level,
                mentorNames = mentorNames
            )
            
            when (val result = userRepository.updateUser(updatedMember)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isSaving = false, 
                            saveSuccess = true,
                            member = updatedMember
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isSaving = false, 
                            error = result.exception.message ?: "Failed to update member details"
                        ) 
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
