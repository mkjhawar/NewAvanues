package com.avanues.utils

import android.util.Log

/**
 * Safe nullable handling utilities to eliminate unsafe force unwraps (!!)
 *
 * Provides extension functions for safe null handling with proper error messages
 * Addresses Critical Issue #7 from accessibility code evaluation report
 *
 * ## Problem
 * The codebase contains 50+ occurrences of `!!` force unwrap operator across 22 files.
 * Force unwraps cause NullPointerException crashes when assumptions are violated.
 *
 * ## Solution
 * Replace `!!` with explicit null-safe patterns:
 * - `requireNotNull()` - Throw with descriptive error
 * - `orThrow()` - Throw custom exception
 * - `orDefault()` - Provide fallback value
 * - `orCompute()` - Lazy default computation
 * - `orLog()` - Log warning and continue
 *
 * ## Examples
 * ```kotlin
 * // Before: bluetoothAdapter!!.state (crashes if null)
 * // After: bluetoothAdapter.requireNotNull("BluetoothAdapter").state
 *
 * // Before: app!!.appName
 * // After: app?.appName.orDefault("Unknown App")
 *
 * // Before: cursor!!
 * // After: cursor.orThrow { IllegalStateException("Query returned null cursor") }
 * ```
 */

// ========================================
// Extension: requireNotNull with context
// ========================================

/**
 * Requires this value to be non-null, throwing IllegalStateException with context if null
 *
 * @param name The name of the value for error messages
 * @param reason Optional reason why the value must not be null
 * @return The non-null value
 * @throws IllegalStateException if value is null
 */
fun <T : Any> T?.requireNotNull(name: String, reason: String? = null): T {
    if (this == null) {
        val message = if (reason != null) {
            "$name must not be null: $reason. Verify the source provides a non-null value, " +
            "check initialization order, and ensure all preconditions are met before this call."
        } else {
            "$name is null but non-null value was expected. This indicates a violated precondition. " +
            "Check initialization, null-safety assumptions, and data flow to this point."
        }
        throw IllegalStateException(message)
    }
    return this
}

// ========================================
// Extension: orThrow with custom exception
// ========================================

/**
 * Returns this value if non-null, or throws custom exception
 *
 * @param exceptionProvider Lambda that creates the exception to throw
 * @return The non-null value
 * @throws Exception The exception created by exceptionProvider
 */
inline fun <T : Any> T?.orThrow(exceptionProvider: () -> Exception): T {
    if (this == null) {
        throw exceptionProvider()
    }
    return this
}

// ========================================
// Extension: orDefault with fallback
// ========================================

/**
 * Returns this value if non-null, or the default value
 *
 * @param default The value to return if this is null
 * @return This value or the default
 */
fun <T : Any> T?.orDefault(default: T): T {
    return this ?: default
}

// ========================================
// Extension: orCompute with lazy default
// ========================================

/**
 * Returns this value if non-null, or computes a default
 *
 * Useful when the default is expensive to compute and should only
 * be computed if needed (lazy evaluation).
 *
 * @param compute Lambda that computes the default value
 * @return This value or the computed default
 */
inline fun <T : Any> T?.orCompute(compute: () -> T): T {
    return this ?: compute()
}

// ========================================
// Extension: orLog with warning
// ========================================

/**
 * Returns this value if non-null, or logs warning and returns null
 *
 * @param tag The log tag
 * @param message The warning message
 * @return This value or null (with logged warning)
 */
fun <T : Any> T?.orLog(tag: String, message: String): T? {
    if (this == null) {
        Log.w(tag, message)
    }
    return this
}

/**
 * Returns this value if non-null, or logs warning and returns default
 *
 * @param tag The log tag
 * @param message The warning message
 * @param default The default value to return if null
 * @return This value or the default (with logged warning if null)
 */
fun <T : Any> T?.orLog(tag: String, message: String, default: T): T {
    if (this == null) {
        Log.w(tag, message)
        return default
    }
    return this
}

// ========================================
// Function: requireAllNotNull
// ========================================

/**
 * Requires all values to be non-null
 *
 * Useful for validating multiple related nullable values at once.
 *
 * @param values The values to check
 * @param nameProvider Lambda that provides the collective name for error messages
 * @return List of all non-null values
 * @throws IllegalStateException if any value is null
 */
fun <T : Any> requireAllNotNull(vararg values: T?, nameProvider: () -> String): List<T> {
    val nullCount = values.count { it == null }
    if (nullCount > 0) {
        throw IllegalStateException(
            "${nameProvider()} contains $nullCount null value(s) out of ${values.size} total - " +
            "all must be non-null for this operation. " +
            "Identify which values are null, verify their sources provide non-null data, " +
            "check preconditions are satisfied, and ensure initialization sequence is correct."
        )
    }
    @Suppress("UNCHECKED_CAST")
    return values.toList() as List<T>
}

// ========================================
// Common Patterns
// ========================================

/**
 * Safe access to Android nullable system services
 *
 * Example:
 * ```kotlin
 * val bluetoothManager = context.getSystemService<BluetoothManager>()
 *     .requireNotNull("BluetoothManager", "Bluetooth not supported on this device")
 * ```
 */
inline fun <reified T : Any> android.content.Context.getSystemServiceSafe(
    serviceName: String,
    errorMessage: String = "${T::class.simpleName} service not available"
): T {
    @Suppress("DEPRECATION")
    return getSystemService(serviceName) as? T
        ?: throw IllegalStateException(
            "Cannot obtain system service: $errorMessage. " +
            "Expected type: ${T::class.simpleName}. Actual returned type may be null or incompatible. " +
            "Verify: (1) device supports this service, (2) Android API level >= minimum required, " +
            "(3) required permissions are granted in AndroidManifest.xml and at runtime."
        )
}
