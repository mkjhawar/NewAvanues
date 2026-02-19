package com.augmentalis.httpavanue.core

/**
 * Sealed class representing a result of an operation that can succeed or fail
 */
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val error: Throwable) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> default
    }
}

fun <T> success(value: T): Result<T> = Result.Success(value)
fun failure(error: Throwable): Result<Nothing> = Result.Failure(error)
