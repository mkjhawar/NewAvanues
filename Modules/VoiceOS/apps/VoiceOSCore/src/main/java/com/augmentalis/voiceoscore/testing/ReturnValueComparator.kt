/**
 * ReturnValueComparator.kt - Deep comparison of method return values
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Testing Framework
 * Created: 2025-10-15 02:48:36 PDT
 */
package com.augmentalis.voiceoscore.testing

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.reflect.KClass

/**
 * Deep equality comparison for return values
 *
 * Handles:
 * - Primitive types and boxed primitives
 * - Strings
 * - Collections (List, Set, Map)
 * - Nullable types
 * - Custom data classes
 * - Async results (CompletableDeferred)
 */
class ReturnValueComparator {

    /**
     * Compare two return values with deep equality
     *
     * @param legacy The legacy implementation's return value
     * @param refactored The refactored implementation's return value
     * @param methodName Name of method being compared (for logging)
     * @return List of divergences (empty if equal)
     */
    suspend fun compare(
        legacy: Any?,
        refactored: Any?,
        methodName: String
    ): List<DivergenceDetail> {
        return try {
            compareValues(legacy, refactored, methodName, "return")
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing return values for $methodName", e)
            listOf(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.HIGH,
                    message = "Comparison failed: ${e.message}",
                    legacyValue = legacy,
                    refactoredValue = refactored,
                    stackTrace = e.stackTraceToString()
                )
            )
        }
    }

    /**
     * Internal recursive comparison with path tracking
     */
    private suspend fun compareValues(
        legacy: Any?,
        refactored: Any?,
        methodName: String,
        path: String
    ): List<DivergenceDetail> {
        val divergences = mutableListOf<DivergenceDetail>()

        // Handle null cases
        when {
            legacy == null && refactored == null -> return emptyList()
            legacy == null -> {
                divergences.add(
                    DivergenceDetail(
                        category = DivergenceCategory.RETURN_VALUE,
                        severity = DivergenceSeverity.CRITICAL,
                        message = "Return value mismatch at $path: legacy returned null, refactored returned non-null",
                        legacyValue = null,
                        refactoredValue = refactored,
                        metadata = mapOf("path" to path, "method" to methodName)
                    )
                )
                return divergences
            }
            refactored == null -> {
                divergences.add(
                    DivergenceDetail(
                        category = DivergenceCategory.RETURN_VALUE,
                        severity = DivergenceSeverity.CRITICAL,
                        message = "Return value mismatch at $path: legacy returned non-null, refactored returned null",
                        legacyValue = legacy,
                        refactoredValue = null,
                        metadata = mapOf("path" to path, "method" to methodName)
                    )
                )
                return divergences
            }
        }

        // Both non-null at this point (all null cases handled above)
        // Safe to assert non-null as when expression above handles all null combinations
        val legacyValue: Any = legacy
            ?: error("Internal logic error: legacy value is null at $path after null-safety checks. " +
                    "The when expression above should have handled all null cases. " +
                    "This indicates a bug in ReturnValueComparator null-handling logic. " +
                    "Verify the when expression covers: (null, null), (null, non-null), (non-null, null) cases.")
        val refactoredValue: Any = refactored
            ?: error("Internal logic error: refactored value is null at $path after null-safety checks. " +
                    "The when expression above should have handled all null cases. " +
                    "This indicates a bug in ReturnValueComparator null-handling logic. " +
                    "Verify the when expression covers: (null, null), (null, non-null), (non-null, null) cases.")

        // Check type compatibility
        if (!areTypesCompatible(legacyValue, refactoredValue)) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.CRITICAL,
                    message = "Type mismatch at $path: legacy=${legacyValue::class.simpleName}, refactored=${refactoredValue::class.simpleName}",
                    legacyValue = legacyValue,
                    refactoredValue = refactoredValue,
                    metadata = mapOf("path" to path, "method" to methodName)
                )
            )
            return divergences
        }

        // Compare by type
        return when (legacyValue) {
            // Primitive types and strings - use equals()
            is String, is Number, is Boolean, is Char -> {
                if (legacyValue != refactoredValue) {
                    listOf(
                        DivergenceDetail(
                            category = DivergenceCategory.RETURN_VALUE,
                            severity = DivergenceSeverity.CRITICAL,
                            message = "Value mismatch at $path",
                            legacyValue = legacyValue,
                            refactoredValue = refactoredValue,
                            metadata = mapOf("path" to path, "method" to methodName)
                        )
                    )
                } else emptyList()
            }

            // Collections - compare element by element
            is List<*> -> compareCollections(
                legacyValue,
                refactoredValue as List<*>,
                methodName,
                path
            )

            is Set<*> -> compareSets(
                legacyValue,
                refactoredValue as Set<*>,
                methodName,
                path
            )

            is Map<*, *> -> compareMaps(
                legacyValue,
                refactoredValue as Map<*, *>,
                methodName,
                path
            )

            // Async results
            is CompletableDeferred<*> -> compareAsync(
                legacyValue,
                refactoredValue as CompletableDeferred<*>,
                methodName,
                path
            )

            // Custom objects - try structural comparison
            else -> compareStructural(legacyValue, refactoredValue, methodName, path)
        }
    }

    /**
     * Check if two types are compatible for comparison
     */
    private fun areTypesCompatible(legacy: Any, refactored: Any): Boolean {
        // Same class - compatible
        if (legacy::class == refactored::class) return true

        // Number types are compatible with each other
        if (legacy is Number && refactored is Number) return true

        // Collection types
        if (legacy is List<*> && refactored is List<*>) return true
        if (legacy is Set<*> && refactored is Set<*>) return true
        if (legacy is Map<*, *> && refactored is Map<*, *>) return true

        return false
    }

    /**
     * Compare two lists element by element
     */
    private suspend fun compareCollections(
        legacy: List<*>,
        refactored: List<*>,
        methodName: String,
        path: String
    ): List<DivergenceDetail> {
        val divergences = mutableListOf<DivergenceDetail>()

        // Size mismatch
        if (legacy.size != refactored.size) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.HIGH,
                    message = "Collection size mismatch at $path: legacy=${legacy.size}, refactored=${refactored.size}",
                    legacyValue = legacy.size,
                    refactoredValue = refactored.size,
                    metadata = mapOf("path" to path, "method" to methodName)
                )
            )
        }

        // Compare elements up to min size
        val minSize = minOf(legacy.size, refactored.size)
        for (i in 0 until minSize) {
            val elementDivergences = compareValues(
                legacy[i],
                refactored[i],
                methodName,
                "$path[$i]"
            )
            divergences.addAll(elementDivergences)
        }

        return divergences
    }

    /**
     * Compare two sets
     */
    private suspend fun compareSets(
        legacy: Set<*>,
        refactored: Set<*>,
        methodName: String,
        path: String
    ): List<DivergenceDetail> {
        val divergences = mutableListOf<DivergenceDetail>()

        // Size mismatch
        if (legacy.size != refactored.size) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.HIGH,
                    message = "Set size mismatch at $path: legacy=${legacy.size}, refactored=${refactored.size}",
                    legacyValue = legacy.size,
                    refactoredValue = refactored.size,
                    metadata = mapOf("path" to path, "method" to methodName)
                )
            )
        }

        // Find elements only in legacy
        val onlyInLegacy = legacy - refactored
        if (onlyInLegacy.isNotEmpty()) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.HIGH,
                    message = "Elements only in legacy set at $path",
                    legacyValue = onlyInLegacy,
                    refactoredValue = null,
                    metadata = mapOf("path" to path, "method" to methodName, "count" to onlyInLegacy.size)
                )
            )
        }

        // Find elements only in refactored
        val onlyInRefactored = refactored - legacy
        if (onlyInRefactored.isNotEmpty()) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.HIGH,
                    message = "Elements only in refactored set at $path",
                    legacyValue = null,
                    refactoredValue = onlyInRefactored,
                    metadata = mapOf("path" to path, "method" to methodName, "count" to onlyInRefactored.size)
                )
            )
        }

        return divergences
    }

    /**
     * Compare two maps
     */
    private suspend fun compareMaps(
        legacy: Map<*, *>,
        refactored: Map<*, *>,
        methodName: String,
        path: String
    ): List<DivergenceDetail> {
        val divergences = mutableListOf<DivergenceDetail>()

        // Size mismatch
        if (legacy.size != refactored.size) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.HIGH,
                    message = "Map size mismatch at $path: legacy=${legacy.size}, refactored=${refactored.size}",
                    legacyValue = legacy.size,
                    refactoredValue = refactored.size,
                    metadata = mapOf("path" to path, "method" to methodName)
                )
            )
        }

        // Find keys only in legacy
        val onlyInLegacy = legacy.keys - refactored.keys
        if (onlyInLegacy.isNotEmpty()) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.HIGH,
                    message = "Keys only in legacy map at $path",
                    legacyValue = onlyInLegacy,
                    refactoredValue = null,
                    metadata = mapOf("path" to path, "method" to methodName, "count" to onlyInLegacy.size)
                )
            )
        }

        // Find keys only in refactored
        val onlyInRefactored = refactored.keys - legacy.keys
        if (onlyInRefactored.isNotEmpty()) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.HIGH,
                    message = "Keys only in refactored map at $path",
                    legacyValue = null,
                    refactoredValue = onlyInRefactored,
                    metadata = mapOf("path" to path, "method" to methodName, "count" to onlyInRefactored.size)
                )
            )
        }

        // Compare values for common keys
        val commonKeys = legacy.keys.intersect(refactored.keys)
        for (key in commonKeys) {
            val valueDivergences = compareValues(
                legacy[key],
                refactored[key],
                methodName,
                "$path[$key]"
            )
            divergences.addAll(valueDivergences)
        }

        return divergences
    }

    /**
     * Compare async results with timeout
     */
    private suspend fun compareAsync(
        legacy: CompletableDeferred<*>,
        refactored: CompletableDeferred<*>,
        methodName: String,
        path: String
    ): List<DivergenceDetail> {
        val divergences = mutableListOf<DivergenceDetail>()

        // Wait for both with timeout
        val legacyResult = withTimeoutOrNull(ASYNC_TIMEOUT_MS) {
            legacy.await()
        }

        val refactoredResult = withTimeoutOrNull(ASYNC_TIMEOUT_MS) {
            refactored.await()
        }

        // Check for timeout divergence
        when {
            legacyResult == null && refactoredResult == null -> {
                divergences.add(
                    DivergenceDetail(
                        category = DivergenceCategory.ASYNC_RESULT,
                        severity = DivergenceSeverity.HIGH,
                        message = "Both async operations timed out at $path",
                        legacyValue = "TIMEOUT",
                        refactoredValue = "TIMEOUT",
                        metadata = mapOf("path" to path, "method" to methodName, "timeoutMs" to ASYNC_TIMEOUT_MS)
                    )
                )
            }
            legacyResult == null -> {
                divergences.add(
                    DivergenceDetail(
                        category = DivergenceCategory.ASYNC_RESULT,
                        severity = DivergenceSeverity.CRITICAL,
                        message = "Legacy async operation timed out at $path",
                        legacyValue = "TIMEOUT",
                        refactoredValue = refactoredResult,
                        metadata = mapOf("path" to path, "method" to methodName, "timeoutMs" to ASYNC_TIMEOUT_MS)
                    )
                )
            }
            refactoredResult == null -> {
                divergences.add(
                    DivergenceDetail(
                        category = DivergenceCategory.ASYNC_RESULT,
                        severity = DivergenceSeverity.CRITICAL,
                        message = "Refactored async operation timed out at $path",
                        legacyValue = legacyResult,
                        refactoredValue = "TIMEOUT",
                        metadata = mapOf("path" to path, "method" to methodName, "timeoutMs" to ASYNC_TIMEOUT_MS)
                    )
                )
            }
            else -> {
                // Both completed - compare results
                val resultDivergences = compareValues(
                    legacyResult,
                    refactoredResult,
                    methodName,
                    "$path.await()"
                )
                divergences.addAll(resultDivergences)
            }
        }

        return divergences
    }

    /**
     * Compare custom objects structurally
     */
    private fun compareStructural(
        legacy: Any,
        refactored: Any,
        methodName: String,
        path: String
    ): List<DivergenceDetail> {
        // For custom objects, use toString() comparison as fallback
        // This is not ideal but better than nothing
        val legacyStr = legacy.toString()
        val refactoredStr = refactored.toString()

        return if (legacyStr != refactoredStr) {
            listOf(
                DivergenceDetail(
                    category = DivergenceCategory.RETURN_VALUE,
                    severity = DivergenceSeverity.MEDIUM,
                    message = "Structural mismatch at $path (using toString comparison)",
                    legacyValue = legacyStr,
                    refactoredValue = refactoredStr,
                    metadata = mapOf(
                        "path" to path,
                        "method" to methodName,
                        "warning" to "Used toString() for comparison - may not detect all differences"
                    )
                )
            )
        } else emptyList()
    }

    companion object {
        private const val TAG = "ReturnValueComparator"
        private const val ASYNC_TIMEOUT_MS = 5000L
    }
}
