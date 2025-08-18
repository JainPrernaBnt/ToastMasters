package com.bntsoft.toastmasters.domain.model

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

data class LoginResult(
    val user: User? = null,
    val isNewUser: Boolean = false,
    val requiresApproval: Boolean = false
)
