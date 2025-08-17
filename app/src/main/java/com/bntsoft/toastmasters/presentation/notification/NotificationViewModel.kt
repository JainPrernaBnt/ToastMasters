package com.bntsoft.toastmasters.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.domain.usecase.notification.NotificationUseCase
import com.bntsoft.toastmasters.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationData> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val isMarkingAsRead: Boolean = false,
    val isDeleting: Boolean = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationUseCase: NotificationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState(isLoading = true))
    val uiState: StateFlow<NotificationUiState> = _uiState

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                notificationUseCase.getUserNotifications()
                    .catch { e ->
                        Timber.e(e, "Error loading notifications")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load notifications: ${e.message}"
                        )
                    }
                    .collectLatest { notifications ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            notifications = notifications,
                            isRefreshing = false
                        )
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error in loadNotifications")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An unexpected error occurred: ${e.message}",
                    isRefreshing = false
                )
            }
        }
    }

    fun refreshNotifications() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadNotifications()
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isMarkingAsRead = true)
                
                when (val result = notificationUseCase.markNotificationAsRead(notificationId)) {
                    is Result.Success -> {
                        // The notification list will be updated automatically via the Flow
                        Timber.d("Notification $notificationId marked as read")
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.exception.message ?: "Failed to mark notification as read"
                        )
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                        Timber.d("Marking notification as read...")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error marking notification as read")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to mark notification as read: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isMarkingAsRead = false)
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isMarkingAsRead = true)
                
                when (val result = notificationUseCase.markAllNotificationsAsRead()) {
                    is Result.Success -> {
                        // The notification list will be updated automatically via the Flow
                        Timber.d("All notifications marked as read")
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.exception.message ?: "Failed to mark all notifications as read"
                        )
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                        Timber.d("Marking all notifications as read...")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error marking all notifications as read")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to mark all notifications as read: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isMarkingAsRead = false)
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDeleting = true)
                
                when (val result = notificationUseCase.deleteNotification(notificationId)) {
                    is Result.Success -> {
                        // The notification list will be updated automatically via the Flow
                        Timber.d("Notification $notificationId deleted")
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.exception.message ?: "Failed to delete notification"
                        )
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                        Timber.d("Deleting notification...")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting notification")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete notification: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = false)
            }
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDeleting = true)
                
                when (val result = notificationUseCase.deleteAllNotifications()) {
                    is Result.Success -> {
                        // The notification list will be updated automatically via the Flow
                        _uiState.value = _uiState.value.copy(notifications = emptyList())
                        Timber.d("All notifications deleted")
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.exception.message ?: "Failed to delete all notifications"
                        )
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                        Timber.d("Deleting all notifications...")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting all notifications")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete all notifications: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
