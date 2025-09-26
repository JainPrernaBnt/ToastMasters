package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.GemMemberData
import com.bntsoft.toastmasters.domain.models.UserRole
import com.bntsoft.toastmasters.domain.repository.GemOfMonthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GemOfMonthViewModel @Inject constructor(
    private val gemOfMonthRepository: GemOfMonthRepository,
    private val userRepository: com.bntsoft.toastmasters.domain.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GemOfMonthUiState())
    val uiState: StateFlow<GemOfMonthUiState> = _uiState.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    init {
        loadCurrentUserRole()
        loadMemberData()
    }
    
    private fun loadCurrentUserRole() {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser()
                Log.d("GemOfMonthViewModel", "Current user: ${currentUser?.name} (${currentUser?.id})")
                Log.d("GemOfMonthViewModel", "Current user role: ${currentUser?.role}")
                Log.d("GemOfMonthViewModel", "VP_EDUCATION enum: ${UserRole.VP_EDUCATION}")
                
                val isVpEducation = currentUser?.role == UserRole.VP_EDUCATION
                Log.d("GemOfMonthViewModel", "Is VP Education: $isVpEducation")
                
                _uiState.value = _uiState.value.copy(isVpEducation = isVpEducation)
            } catch (e: Exception) {
                Log.e("GemOfMonthViewModel", "Error loading current user role", e)
                // Default to member role if error
                _uiState.value = _uiState.value.copy(isVpEducation = false)
            }
        }
    }

    fun loadMemberData() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            Log.d("GemOfMonthViewModel", "Starting to load member data for ${_selectedYear.value}-${_selectedMonth.value}")
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                gemOfMonthRepository.getMemberPerformanceData(
                    year = _selectedYear.value,
                    month = _selectedMonth.value
                ).catch { exception ->
                    Log.e("GemOfMonthViewModel", "Error in flow", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }.collect { memberDataList ->
                    Log.d("GemOfMonthViewModel", "Received ${memberDataList.size} members, loading existing gem selection...")
                    
                    // Load existing gem selection for this month
                    loadExistingGemSelection(memberDataList)
                    
                    val loadTime = System.currentTimeMillis() - startTime
                    Log.d("GemOfMonthViewModel", "Member data loaded in ${loadTime}ms")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        memberDataList = memberDataList,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("GemOfMonthViewModel", "Error loading member data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    private suspend fun loadExistingGemSelection(memberDataList: List<GemMemberData>) {
        try {
            val existingGem = gemOfMonthRepository.getGemOfTheMonth(
                year = _selectedYear.value,
                month = _selectedMonth.value
            )
            
            if (existingGem != null) {
                // Find the selected member in the current list
                val selectedMember = memberDataList.find { it.user.id == existingGem.userId }
                Log.d("GemOfMonthViewModel", "Found existing gem: ${existingGem.memberName} (${existingGem.userId})")
                Log.d("GemOfMonthViewModel", "Selected member found in list: ${selectedMember?.user?.name}")
                
                _uiState.value = _uiState.value.copy(
                    selectedGem = selectedMember,
                    isEditMode = false
                )
            } else {
                Log.d("GemOfMonthViewModel", "No existing gem found for ${_selectedYear.value}-${_selectedMonth.value}")
                _uiState.value = _uiState.value.copy(
                    selectedGem = null,
                    isEditMode = false
                )
            }
        } catch (e: Exception) {
            // No existing selection found
            _uiState.value = _uiState.value.copy(
                selectedGem = null,
                isEditMode = false
            )
        }
    }

    fun selectMonth(year: Int, month: Int) {
        Log.d("GemOfMonthViewModel", "Selecting month: $month/$year")
        
        // Only update if values actually changed
        val yearChanged = _selectedYear.value != year
        val monthChanged = _selectedMonth.value != month
        
        if (yearChanged || monthChanged) {
            _selectedYear.value = year
            _selectedMonth.value = month
            
            // Reset edit mode when changing months
            _uiState.value = _uiState.value.copy(
                isEditMode = false,
                selectedGem = null
            )
            
            loadMemberData()
        } else {
            Log.d("GemOfMonthViewModel", "Month/year unchanged, skipping reload")
        }
    }

    fun sortMembers(sortType: SortType) {
        val currentList = _uiState.value.memberDataList
        val sortedList = when (sortType) {
            SortType.SCORE_DESC -> currentList.sortedByDescending { it.performanceScore }
            SortType.SCORE_ASC -> currentList.sortedBy { it.performanceScore }
            SortType.NAME_ASC -> currentList.sortedBy { it.user.name }
            SortType.NAME_DESC -> currentList.sortedByDescending { it.user.name }
            SortType.ATTENDANCE_DESC -> currentList.sortedByDescending { it.attendanceData.attendancePercentage }
            SortType.ROLES_DESC -> currentList.sortedByDescending { it.roleData.totalRoles }
        }
        
        _uiState.value = _uiState.value.copy(
            memberDataList = sortedList,
            currentSortType = sortType
        )
    }

    fun selectGemOfTheMonth(memberData: GemMemberData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val result = gemOfMonthRepository.saveGemOfTheMonth(
                    meetingId = "", // Not needed for new collection structure
                    userId = memberData.user.id,
                    year = _selectedYear.value,
                    month = _selectedMonth.value
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedGem = memberData,
                        isEditMode = false,
                        showSuccessMessage = "Successfully selected ${memberData.user.name} as Gem of the Month for ${getMonthYearString()}!"
                    )
                    // Reload data to show updated gem history
                    loadMemberData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to save Gem of the Month selection"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun enterEditMode() {
        _uiState.value = _uiState.value.copy(isEditMode = true)
    }
    
    fun exitEditMode() {
        _uiState.value = _uiState.value.copy(isEditMode = false)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            showSuccessMessage = null
        )
    }

    fun getMonthYearString(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, _selectedYear.value)
        calendar.set(Calendar.MONTH, _selectedMonth.value - 1)
        
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        return "${monthNames[_selectedMonth.value - 1]} ${_selectedYear.value}"
    }
}

data class GemOfMonthUiState(
    val isLoading: Boolean = false,
    val memberDataList: List<GemMemberData> = emptyList(),
    val selectedGem: GemMemberData? = null,
    val currentSortType: SortType = SortType.SCORE_DESC,
    val isVpEducation: Boolean = false,
    val isEditMode: Boolean = false,
    val error: String? = null,
    val showSuccessMessage: String? = null
) {
    val eligibleMembersCount: Int
        get() = memberDataList.size
        
    val hasData: Boolean
        get() = memberDataList.isNotEmpty()
        
    val canEdit: Boolean
        get() = isVpEducation
        
    val showAllMembers: Boolean
        get() = selectedGem == null || isEditMode
        
    val showEditButton: Boolean
        get() = isVpEducation && selectedGem != null && !isEditMode
}

enum class SortType(val displayName: String) {
    SCORE_DESC("Score (High to Low)"),
    SCORE_ASC("Score (Low to High)"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    ATTENDANCE_DESC("Attendance (High to Low)"),
    ROLES_DESC("Roles (Most to Least)")
}
