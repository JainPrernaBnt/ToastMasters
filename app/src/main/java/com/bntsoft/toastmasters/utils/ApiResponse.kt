package com.bntsoft.toastmasters.utils

/**
 * Common class used by API responses.
 * @param <T> the type of the response object
 */
sealed class ApiResponse<out T> {
    /**
     * Success response with body
     */
    data class Success<T>(val body: T) : ApiResponse<T>()

    /**
     * Failure response with an error message
     */
    data class Error(val errorMessage: String) : ApiResponse<Nothing>()

    /**
     * Empty response (e.g., for 204 No Content responses)
     */
    object Empty : ApiResponse<Nothing>()
}

/**
 * Type aliases for cleaner code
 */
typealias ApiSuccessResponse<T> = ApiResponse.Success<T>
typealias ApiErrorResponse = ApiResponse.Error
typealias ApiEmptyResponse = ApiResponse.Empty

/**
 * Extension function to convert a [Result] to an [ApiResponse]
 */
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
