package com.augmentalis.browseravanue.core

/**
 * Type-safe result wrapper for Browser operations
 *
 * Provides monadic operations for functional error handling
 * without throwing exceptions.
 */
sealed class BrowserResult<out T> {

    data class Success<T>(val data: T) : BrowserResult<T>()
    data class Error(val error: BrowserError) : BrowserResult<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    /**
     * Transform success value
     */
    inline fun <R> map(transform: (T) -> R): BrowserResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }

    /**
     * Flat map for chaining operations
     */
    inline fun <R> flatMap(transform: (T) -> BrowserResult<R>): BrowserResult<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> this
        }
    }

    /**
     * Execute action on success
     */
    inline fun onSuccess(action: (T) -> Unit): BrowserResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Execute action on error
     */
    inline fun onError(action: (BrowserError) -> Unit): BrowserResult<T> {
        if (this is Error) {
            action(error)
        }
        return this
    }

    /**
     * Get value or null
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            is Error -> null
        }
    }

    /**
     * Get value or default
     */
    fun getOrDefault(default: T): T {
        return when (this) {
            is Success -> data
            is Error -> default
        }
    }

    /**
     * Get error or null
     */
    fun errorOrNull(): BrowserError? {
        return when (this) {
            is Success -> null
            is Error -> error
        }
    }
}

/**
 * Wrap value in Success
 */
fun <T> T.toSuccess(): BrowserResult<T> = BrowserResult.Success(this)

/**
 * Wrap error in Error result
 */
fun BrowserError.toError(): BrowserResult<Nothing> = BrowserResult.Error(this)
