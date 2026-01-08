package com.augmentalis.magicui.core

/**
 * Extension functions for Kotlin Result type to enhance error handling.
 *
 * These utilities follow the functional error handling pattern:
 * - Never throw exceptions in business logic
 * - Chain operations with map/flatMap
 * - Provide clear error messages with context
 *
 * @since 3.1.0
 */

/**
 * Maps the success value if present, preserving failures.
 *
 * Example:
 * ```kotlin
 * val result: Result<Int> = Result.success(5)
 * val doubled = result.mapSuccess { it * 2 }  // Success(10)
 * ```
 */
inline fun <T, R> Result<T>.mapSuccess(transform: (T) -> R): Result<R> {
    return when {
        isSuccess -> Result.success(transform(getOrThrow()))
        else -> Result.failure(exceptionOrNull() ?: Exception("Unknown error"))
    }
}

/**
 * Flat-maps the success value, allowing chained Result-returning operations.
 *
 * Example:
 * ```kotlin
 * parseLayout(source)
 *     .flatMapSuccess { validateSchema(it) }
 *     .flatMapSuccess { buildComponents(it) }
 * ```
 */
inline fun <T, R> Result<T>.flatMapSuccess(transform: (T) -> Result<R>): Result<R> {
    return when {
        isSuccess -> transform(getOrThrow())
        else -> Result.failure(exceptionOrNull() ?: Exception("Unknown error"))
    }
}

/**
 * Executes a side effect if the result is a success, without modifying the result.
 *
 * Example:
 * ```kotlin
 * loadLayout(file)
 *     .onSuccess { logger.info("Loaded ${it.size} components") }
 *     .onFailure { logger.error("Failed to load layout: $it") }
 * ```
 */
inline fun <T> Result<T>.onFailure(action: (Throwable) -> Unit): Result<T> {
    exceptionOrNull()?.let(action)
    return this
}

/**
 * Returns the success value or a default value if failed.
 *
 * Example:
 * ```kotlin
 * val components = loadLayout(file).getOrDefault(emptyList())
 * ```
 */
fun <T> Result<T>.getOrDefault(default: T): T {
    return getOrElse { default }
}

/**
 * Converts a Result to a nullable value (null on failure).
 */
fun <T> Result<T>.getOrNull(): T? {
    return getOrElse { null }
}

/**
 * Creates a Result from a nullable value, using the provided error message if null.
 */
fun <T> T?.toResult(errorMessage: String): Result<T> {
    return this?.let { Result.success(it) } ?: Result.failure(Exception(errorMessage))
}

/**
 * Wraps a potentially throwing operation in a Result.
 *
 * Example:
 * ```kotlin
 * val result = runCatching { riskyOperation() }
 * ```
 */
inline fun <T> runCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
