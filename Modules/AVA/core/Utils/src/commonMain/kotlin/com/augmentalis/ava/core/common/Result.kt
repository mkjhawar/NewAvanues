package com.augmentalis.ava.core.common

/**
 * Result wrapper for operations that can fail
 * Inspired by Kotlin Result but with custom error types
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}
