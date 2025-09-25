package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.GemMemberData
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
    private val gemOfMonthRepository: GemOfMonthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GemOfMonthUiState())
    val uiState: StateFlow<GemOfMonthUiState> = _uiState.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    init {
        loadMemberData()
    }

    fun loadMemberData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                gemOfMonthRepository.getMemberPerformanceData(
                    year = _selectedYear.value,
                    month = _selectedMonth.value
                ).catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }.collect { memberDataList ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        memberDataList = memberDataList,
                        error = null
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

    fun selectMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        loadMemberData()
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
    val error: String? = null,
    val showSuccessMessage: String? = null
) {
    val eligibleMembersCount: Int
        get() = memberDataList.size
        
    val hasData: Boolean
        get() = memberDataList.isNotEmpty()
}

enum class SortType(val displayName: String) {
    SCORE_DESC("Score (High to Low)"),
    SCORE_ASC("Score (Low to High)"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    ATTENDANCE_DESC("Attendance (High to Low)"),
    ROLES_DESC("Roles (Most to Least)")
}
