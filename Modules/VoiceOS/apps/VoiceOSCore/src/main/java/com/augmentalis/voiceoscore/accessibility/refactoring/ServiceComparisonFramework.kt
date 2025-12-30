/**
 * ServiceComparisonFramework.kt - Compare outputs between legacy and refactored implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-15
 *
 * Compares method execution results, return values, and side effects between
 * legacy and refactored VoiceOSService implementations to ensure functional equivalence.
 */
package com.augmentalis.voiceoscore.accessibility.refactoring

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Service comparison framework
 *
 * Compares execution results between legacy and refactored implementations
 * to detect behavioral differences. All comparisons are async and non-blocking.
 *
 * Comparison Types:
 * 1. Return Value Comparison - Compare method return values
 * 2. State Change Comparison - Compare side effects on service state
 * 3. Timing Comparison - Compare execution performance
 * 4. Error Comparison - Compare exception behavior
 */
class ServiceComparisonFramework {

    companion object {
        private const val TAG = "ServiceComparison"
        private const val MAX_HISTORY = 1000 // Keep last 1000 comparisons
    }

    // Comparison scope for async operations
    private val comparisonScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Comparison statistics
    private val totalComparisons = AtomicLong(0)
    private val matchCount = AtomicLong(0)
    private val mismatchCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)

    // Recent comparison history (thread-safe)
    private val comparisonHistory = ConcurrentHashMap<Long, ComparisonResult>()

    /**
     * Comparison result for a single method call
     */
    data class ComparisonResult(
        val methodName: String,
        val timestamp: Long,
        val legacyResult: MethodResult,
        val refactoredResult: MethodResult,
        val matches: Boolean,
        val divergenceType: DivergenceType?,
        val executionTimeMs: Long
    ) {
        /**
         * Human-readable description of the comparison
         */
        fun getDescription(): String {
            return if (matches) {
                "✓ MATCH: $methodName (${executionTimeMs}ms)"
            } else {
                "✗ MISMATCH: $methodName - ${divergenceType?.name} (${executionTimeMs}ms)"
            }
        }
    }

    /**
     * Method execution result
     */
    data class MethodResult(
        val returnValue: Any?,
        val exception: Throwable?,
        val executionTimeMs: Long,
        val stateSnapshot: Map<String, Any>? = null
    )

    /**
     * Types of divergence that can be detected
     */
    enum class DivergenceType {
        RETURN_VALUE_MISMATCH,
        EXCEPTION_MISMATCH,
        STATE_CHANGE_MISMATCH,
        TIMING_SIGNIFICANT_DIFFERENCE,
        NULL_VS_NON_NULL,
        TYPE_MISMATCH
    }

    /**
     * Compare results from legacy and refactored implementations
     *
     * @param methodName Name of the method being compared
     * @param legacyResult Result from legacy implementation
     * @param refactoredResult Result from refactored implementation
     * @return ComparisonResult with match status and details
     */
    fun compareResults(
        methodName: String,
        legacyResult: MethodResult,
        refactoredResult: MethodResult
    ): ComparisonResult {
        val startTime = System.currentTimeMillis()

        try {
            // Compare exceptions first
            val exceptionMatch = compareExceptions(legacyResult.exception, refactoredResult.exception)
            if (!exceptionMatch.first) {
                return createMismatchResult(
                    methodName,
                    legacyResult,
                    refactoredResult,
                    DivergenceType.EXCEPTION_MISMATCH,
                    startTime
                )
            }

            // If both threw exceptions, they match
            if (legacyResult.exception != null && refactoredResult.exception != null) {
                return createMatchResult(methodName, legacyResult, refactoredResult, startTime)
            }

            // Compare return values
            val returnValueMatch = compareReturnValues(legacyResult.returnValue, refactoredResult.returnValue)
            if (!returnValueMatch.first) {
                return createMismatchResult(
                    methodName,
                    legacyResult,
                    refactoredResult,
                    returnValueMatch.second,
                    startTime
                )
            }

            // Compare state changes if available
            if (legacyResult.stateSnapshot != null && refactoredResult.stateSnapshot != null) {
                val stateMatch = compareStates(legacyResult.stateSnapshot, refactoredResult.stateSnapshot)
                if (!stateMatch) {
                    return createMismatchResult(
                        methodName,
                        legacyResult,
                        refactoredResult,
                        DivergenceType.STATE_CHANGE_MISMATCH,
                        startTime
                    )
                }
            }

            // Compare timing (warn if significant difference, but don't fail)
            val timingDifference = kotlin.math.abs(
                legacyResult.executionTimeMs - refactoredResult.executionTimeMs
            )
            if (timingDifference > 100) { // > 100ms difference
                Log.w(TAG, "Significant timing difference for $methodName: " +
                        "legacy=${legacyResult.executionTimeMs}ms, " +
                        "refactored=${refactoredResult.executionTimeMs}ms")
            }

            // All checks passed
            return createMatchResult(methodName, legacyResult, refactoredResult, startTime)

        } catch (e: Exception) {
            Log.e(TAG, "Error during comparison of $methodName", e)
            errorCount.incrementAndGet()
            return createErrorResult(methodName, legacyResult, refactoredResult, startTime, e)
        }
    }

    /**
     * Compare return values with deep equality checks
     */
    private fun compareReturnValues(
        legacyValue: Any?,
        refactoredValue: Any?
    ): Pair<Boolean, DivergenceType?> {
        // Both null - match
        if (legacyValue == null && refactoredValue == null) {
            return true to null
        }

        // One null, one not - mismatch
        if (legacyValue == null || refactoredValue == null) {
            return false to DivergenceType.NULL_VS_NON_NULL
        }

        // Type mismatch
        if (legacyValue::class != refactoredValue::class) {
            return false to DivergenceType.TYPE_MISMATCH
        }

        // Deep equality check
        val matches = when (legacyValue) {
            is Boolean, is Int, is Long, is Float, is Double, is String -> {
                legacyValue == refactoredValue
            }
            is Collection<*> -> {
                compareCollections(legacyValue, refactoredValue as Collection<*>)
            }
            is Map<*, *> -> {
                compareMaps(legacyValue, refactoredValue as Map<*, *>)
            }
            else -> {
                // For complex objects, use toString comparison
                legacyValue.toString() == refactoredValue.toString()
            }
        }

        return matches to if (matches) null else DivergenceType.RETURN_VALUE_MISMATCH
    }

    /**
     * Compare exceptions
     */
    private fun compareExceptions(
        legacyEx: Throwable?,
        refactoredEx: Throwable?
    ): Pair<Boolean, DivergenceType?> {
        // Both null - match
        if (legacyEx == null && refactoredEx == null) {
            return true to null
        }

        // One threw, one didn't - mismatch
        if (legacyEx == null || refactoredEx == null) {
            return false to DivergenceType.EXCEPTION_MISMATCH
        }

        // Both threw - check if same exception type
        val sameType = legacyEx::class == refactoredEx::class
        return sameType to if (sameType) null else DivergenceType.EXCEPTION_MISMATCH
    }

    /**
     * Compare collections for equality
     */
    private fun compareCollections(legacy: Collection<*>, refactored: Collection<*>): Boolean {
        if (legacy.size != refactored.size) return false
        return legacy.zip(refactored).all { (a, b) -> a == b }
    }

    /**
     * Compare maps for equality
     */
    private fun compareMaps(legacy: Map<*, *>, refactored: Map<*, *>): Boolean {
        if (legacy.size != refactored.size) return false
        return legacy.keys.all { key ->
            refactored.containsKey(key) && legacy[key] == refactored[key]
        }
    }

    /**
     * Compare state snapshots
     */
    private fun compareStates(legacyState: Map<String, Any>, refactoredState: Map<String, Any>): Boolean {
        // Check if all important keys match
        val importantKeys = (legacyState.keys + refactoredState.keys).toSet()
        return importantKeys.all { key ->
            legacyState[key] == refactoredState[key]
        }
    }

    /**
     * Create match result
     */
    private fun createMatchResult(
        methodName: String,
        legacyResult: MethodResult,
        refactoredResult: MethodResult,
        startTime: Long
    ): ComparisonResult {
        matchCount.incrementAndGet()
        totalComparisons.incrementAndGet()

        val result = ComparisonResult(
            methodName = methodName,
            timestamp = System.currentTimeMillis(),
            legacyResult = legacyResult,
            refactoredResult = refactoredResult,
            matches = true,
            divergenceType = null,
            executionTimeMs = System.currentTimeMillis() - startTime
        )

        storeResult(result)
        Log.v(TAG, result.getDescription())
        return result
    }

    /**
     * Create mismatch result
     */
    private fun createMismatchResult(
        methodName: String,
        legacyResult: MethodResult,
        refactoredResult: MethodResult,
        divergenceType: DivergenceType?,
        startTime: Long
    ): ComparisonResult {
        mismatchCount.incrementAndGet()
        totalComparisons.incrementAndGet()

        val result = ComparisonResult(
            methodName = methodName,
            timestamp = System.currentTimeMillis(),
            legacyResult = legacyResult,
            refactoredResult = refactoredResult,
            matches = false,
            divergenceType = divergenceType,
            executionTimeMs = System.currentTimeMillis() - startTime
        )

        storeResult(result)
        Log.w(TAG, result.getDescription())
        logDetailedMismatch(result)
        return result
    }

    /**
     * Create error result
     */
    private fun createErrorResult(
        methodName: String,
        legacyResult: MethodResult,
        refactoredResult: MethodResult,
        startTime: Long,
        error: Throwable
    ): ComparisonResult {
        errorCount.incrementAndGet()
        totalComparisons.incrementAndGet()

        val result = ComparisonResult(
            methodName = methodName,
            timestamp = System.currentTimeMillis(),
            legacyResult = legacyResult,
            refactoredResult = refactoredResult,
            matches = false,
            divergenceType = null,
            executionTimeMs = System.currentTimeMillis() - startTime
        )

        storeResult(result)
        Log.e(TAG, "Comparison error for $methodName", error)
        return result
    }

    /**
     * Store result in history (limit size)
     */
    private fun storeResult(result: ComparisonResult) {
        comparisonHistory[result.timestamp] = result

        // Trim history if too large
        if (comparisonHistory.size > MAX_HISTORY) {
            val oldestKey = comparisonHistory.keys.minOrNull()
            oldestKey?.let { comparisonHistory.remove(it) }
        }
    }

    /**
     * Log detailed mismatch information
     */
    private fun logDetailedMismatch(result: ComparisonResult) {
        Log.w(TAG, "=== DIVERGENCE DETECTED ===")
        Log.w(TAG, "Method: ${result.methodName}")
        Log.w(TAG, "Type: ${result.divergenceType}")
        Log.w(TAG, "Legacy return: ${result.legacyResult.returnValue}")
        Log.w(TAG, "Refactored return: ${result.refactoredResult.returnValue}")
        Log.w(TAG, "Legacy exception: ${result.legacyResult.exception}")
        Log.w(TAG, "Refactored exception: ${result.refactoredResult.exception}")
        Log.w(TAG, "Legacy time: ${result.legacyResult.executionTimeMs}ms")
        Log.w(TAG, "Refactored time: ${result.refactoredResult.executionTimeMs}ms")
        Log.w(TAG, "==========================")
    }

    /**
     * Get comparison statistics
     */
    fun getStatistics(): Map<String, Long> {
        return mapOf(
            "total" to totalComparisons.get(),
            "matches" to matchCount.get(),
            "mismatches" to mismatchCount.get(),
            "errors" to errorCount.get(),
            "matchRate" to if (totalComparisons.get() > 0) {
                (matchCount.get() * 100 / totalComparisons.get())
            } else 0
        )
    }

    /**
     * Get recent comparison history
     */
    fun getRecentHistory(count: Int = 100): List<ComparisonResult> {
        return comparisonHistory.values
            .sortedByDescending { it.timestamp }
            .take(count)
    }

    /**
     * Clear all statistics and history
     */
    fun reset() {
        totalComparisons.set(0)
        matchCount.set(0)
        mismatchCount.set(0)
        errorCount.set(0)
        comparisonHistory.clear()
        Log.i(TAG, "Comparison framework reset")
    }
}
