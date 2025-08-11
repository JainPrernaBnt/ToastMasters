package com.bntsoft.toastmasters.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.UserRole
import com.bntsoft.toastmasters.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole: StateFlow<UserRole?> = _userRole

    init {
        loadUserRole()
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            _userRole.value = UserRole.VP_EDUCATION
        }
    }

    fun setUserRole(role: UserRole) {
        viewModelScope.launch {
            // In a real app, this would save the user's role to a data source
            _userRole.value = role
        }
    }
}
