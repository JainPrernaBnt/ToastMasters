package com.bntsoft.toastmasters.domain.model

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
    data class DeviceConflict<out T>(
        val message: String,
        val email: String,
        val password: String
    ) : AuthResult<T>()
    object Loading : AuthResult<Nothing>()
}
