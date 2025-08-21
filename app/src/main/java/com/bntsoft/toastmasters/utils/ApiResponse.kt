package com.bntsoft.toastmasters.utils

sealed class ApiResponse<out T> {

    data class Success<T>(val body: T) : ApiResponse<T>()

    data class Error(val errorMessage: String) : ApiResponse<Nothing>()

    object Empty : ApiResponse<Nothing>()
}

typealias ApiSuccessResponse<T> = ApiResponse.Success<T>
typealias ApiErrorResponse = ApiResponse.Error
typealias ApiEmptyResponse = ApiResponse.Empty

fun <T> Result<T>.toApiResponse(): ApiResponse<T> {
    return try {
        val data = this.getOrThrow()
        if (data != null) {
            ApiResponse.Success(data)
        } else {
            ApiResponse.Empty
        }
    } catch (e: Exception) {
        ApiResponse.Error(e.message ?: "Unknown error")
    }
}
