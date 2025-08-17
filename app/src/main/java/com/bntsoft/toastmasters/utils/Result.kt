package com.bntsoft.toastmasters.utils

import java.lang.Exception

/**
 * A generic class that holds a value with its loading status.
 * @param T The type of the data being returned
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with the result data.
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * Represents a failed operation with an exception.
     */
    data class Error(val exception: Exception) : Result<Nothing>()

    /**
     * Represents a loading state, typically used during async operations.
     */
    object Loading : Result<Nothing>()

    /**
     * Returns the success data if this is a [Success] result, or null otherwise.
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Returns the exception if this is an [Error] result, or null otherwise.
     */
    fun exceptionOrNull(): Exception? = (this as? Error)?.exception

    /**
     * Returns the success data or throws the exception if this is an [Error].
     * @throws Exception if this is an [Error] result
     */
    @Throws(Exception::class)
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data
            is Error -> throw exception
            is Loading -> throw IllegalStateException("Result is still loading")
        }
    }

    /**
     * Executes the given [action] if this is a [Success] result.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Executes the given [action] if this is an [Error] result.
     */
    inline fun onError(action: (Exception) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    /**
     * Executes the given [action] if this is a [Loading] result.
     */
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }

    companion object {
        /**
         * Creates a [Result] from a function that may throw.
         */
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e)
            }
        }
    }
}

/**
 * Maps the success value of this [Result] using the given [transform] function.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
        is Result.Loading -> this
    }
}

/**
 * Maps the error value of this [Result] using the given [transform] function.
 */
inline fun <T> Result<T>.mapError(transform: (Exception) -> Exception): Result<T> {
    return when (this) {
        is Result.Success -> this
        is Result.Error -> Result.Error(transform(exception))
        is Result.Loading -> this
    }
}

/**
 * Returns the success value or a default value if this is an [Error] or [Loading] result.
 */
inline fun <T> Result<T>.getOrDefault(defaultValue: T): T {
    return when (this) {
        is Result.Success -> data
        else -> defaultValue
    }
}

/**
 * Returns the success value or computes a default value if this is an [Error] or [Loading] result.
 */
inline fun <T> Result<T>.getOrElse(onFailure: (Exception?) -> T): T {
    return when (this) {
        is Result.Success -> data
        is Result.Error -> onFailure(exception)
        is Result.Loading -> onFailure(null)
    }
}