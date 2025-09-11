package com.bntsoft.toastmasters.presentation.ui.common.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val error: String? = null,
    val navigateToLogin: Boolean = false
) {
    companion object {
        val Loading = SettingsUiState(isLoading = true)
        fun Success(user: User) = SettingsUiState(isLoading = false, user = user)
        fun Error(message: String) = SettingsUiState(isLoading = false, error = message)
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user = userRepository.getCurrentUser()
                if (user != null) {
                    _uiState.value = SettingsUiState.Success(user)
                } else {
                    _uiState.value = SettingsUiState.Error("User not found")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                firebaseAuth.signOut()
                _uiState.update { it.copy(navigateToLogin = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Logout failed") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNavigationComplete() {
        _uiState.update { it.copy(navigateToLogin = false) }
    }
}
