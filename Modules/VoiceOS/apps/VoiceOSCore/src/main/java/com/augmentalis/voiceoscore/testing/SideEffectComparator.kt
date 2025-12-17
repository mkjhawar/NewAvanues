/**
 * SideEffectComparator.kt - Compare side effects between implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Testing Framework
 * Created: 2025-10-15 02:48:36 PDT
 */
package com.augmentalis.voiceoscore.testing

import android.content.Intent
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Types of side effects that can be tracked
 */
enum class SideEffectType {
    DATABASE_INSERT,
    DATABASE_UPDATE,
    DATABASE_DELETE,
    DATABASE_QUERY,
    BROADCAST_SENT,
    SERVICE_STARTED,
    SERVICE_STOPPED,
    COROUTINE_LAUNCHED,
    CACHE_UPDATED,
    FILE_WRITE,
    FILE_READ,
    FILE_DELETE,
    NETWORK_REQUEST,
    PREFERENCE_WRITE,
    PREFERENCE_READ
}

/**
 * A single side effect observation
 */
data class SideEffect(
    val type: SideEffectType,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val data: Map<String, Any?> = emptyMap()
) {
    /**
     * Check if this side effect is functionally equivalent to another
     */
    fun isEquivalentTo(other: SideEffect, ignoredFields: Set<String> = emptySet()): Boolean {
        if (type != other.type) return false
        if (description != other.description) return false

        // Compare data excluding ignored fields
        val relevantData = data.filterKeys { !ignoredFields.contains(it) }
        val otherRelevantData = other.data.filterKeys { !ignoredFields.contains(it) }

        return relevantData == otherRelevantData
    }

    override fun toString(): String {
        return "SideEffect(type=$type, desc='$description', data=$data)"
    }
}

/**
 * Collection of side effects for comparison
 */
data class SideEffectTrace(
    val effects: List<SideEffect> = emptyList(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis()
) {
    val duration: Long
        get() = endTime - startTime

    fun isEmpty(): Boolean = effects.isEmpty()
    fun size(): Int = effects.size

    /**
     * Group side effects by type
     */
    fun groupByType(): Map<SideEffectType, List<SideEffect>> {
        return effects.groupBy { it.type }
    }
}

/**
 * Tracker for capturing side effects during execution
 */
class SideEffectTracker {
    private val effects = CopyOnWriteArrayList<SideEffect>()
    private var startTime: Long = 0
    private var endTime: Long = 0

    /**
     * Start tracking side effects
     */
    fun start() {
        effects.clear()
        startTime = System.currentTimeMillis()
    }

    /**
     * Stop tracking and return trace
     */
    fun stop(): SideEffectTrace {
        endTime = System.currentTimeMillis()
        return SideEffectTrace(
            effects = effects.toList(),
            startTime = startTime,
            endTime = endTime
        )
    }

    /**
     * Track a side effect
     */
    fun track(effect: SideEffect) {
        effects.add(effect)
    }

    // Convenience methods for common side effects

    fun trackDatabaseInsert(table: String, data: Map<String, Any?>) {
        track(
            SideEffect(
                type = SideEffectType.DATABASE_INSERT,
                description = "Insert into $table",
                data = mapOf("table" to table, "data" to data)
            )
        )
    }

    fun trackDatabaseUpdate(table: String, where: String, data: Map<String, Any?>) {
        track(
            SideEffect(
                type = SideEffectType.DATABASE_UPDATE,
                description = "Update $table where $where",
                data = mapOf("table" to table, "where" to where, "data" to data)
            )
        )
    }

    fun trackDatabaseDelete(table: String, where: String) {
        track(
            SideEffect(
                type = SideEffectType.DATABASE_DELETE,
                description = "Delete from $table where $where",
                data = mapOf("table" to table, "where" to where)
            )
        )
    }

    fun trackDatabaseQuery(table: String, query: String, resultCount: Int) {
        track(
            SideEffect(
                type = SideEffectType.DATABASE_QUERY,
                description = "Query $table: $query",
                data = mapOf("table" to table, "query" to query, "resultCount" to resultCount)
            )
        )
    }

    fun trackBroadcast(intent: Intent) {
        track(
            SideEffect(
                type = SideEffectType.BROADCAST_SENT,
                description = "Broadcast: ${intent.action}",
                data = mapOf(
                    "action" to intent.action,
                    "extras" to intent.extras?.let { bundle ->
                        bundle.keySet().associateWith { bundle.get(it) }
                    }
                )
            )
        )
    }

    fun trackServiceStart(serviceName: String) {
        track(
            SideEffect(
                type = SideEffectType.SERVICE_STARTED,
                description = "Started service: $serviceName",
                data = mapOf("service" to serviceName)
            )
        )
    }

    fun trackServiceStop(serviceName: String) {
        track(
            SideEffect(
                type = SideEffectType.SERVICE_STOPPED,
                description = "Stopped service: $serviceName",
                data = mapOf("service" to serviceName)
            )
        )
    }

    fun trackCoroutineLaunch(name: String, dispatcher: String) {
        track(
            SideEffect(
                type = SideEffectType.COROUTINE_LAUNCHED,
                description = "Launched coroutine: $name",
                data = mapOf("name" to name, "dispatcher" to dispatcher)
            )
        )
    }

    fun trackCacheUpdate(cacheName: String, key: String, operation: String) {
        track(
            SideEffect(
                type = SideEffectType.CACHE_UPDATED,
                description = "Cache $operation: $cacheName[$key]",
                data = mapOf("cache" to cacheName, "key" to key, "operation" to operation)
            )
        )
    }

    fun trackFileWrite(path: String, size: Long) {
        track(
            SideEffect(
                type = SideEffectType.FILE_WRITE,
                description = "Write file: $path",
                data = mapOf("path" to path, "size" to size)
            )
        )
    }

    fun trackFileRead(path: String, size: Long) {
        track(
            SideEffect(
                type = SideEffectType.FILE_READ,
                description = "Read file: $path",
                data = mapOf("path" to path, "size" to size)
            )
        )
    }

    fun trackNetworkRequest(url: String, method: String, statusCode: Int) {
        track(
            SideEffect(
                type = SideEffectType.NETWORK_REQUEST,
                description = "$method $url",
                data = mapOf("url" to url, "method" to method, "statusCode" to statusCode)
            )
        )
    }
}

/**
 * Compare side effects between legacy and refactored implementations
 *
 * Features:
 * - Flexible comparison (order-dependent or order-independent)
 * - Missing/extra side effect detection
 * - Type-based filtering
 * - Ignored field support
 */
class SideEffectComparator {

    /**
     * Compare two side effect traces
     *
     * @param legacy Legacy implementation side effects
     * @param refactored Refactored implementation side effects
     * @param methodName Method being compared
     * @param orderMatters Whether side effect order must match
     * @param ignoredTypes Side effect types to ignore
     * @param ignoredDataFields Data fields to ignore in comparison
     * @return List of divergences
     */
    fun compare(
        legacy: SideEffectTrace,
        refactored: SideEffectTrace,
        methodName: String,
        orderMatters: Boolean = true,
        ignoredTypes: Set<SideEffectType> = DEFAULT_IGNORED_TYPES,
        ignoredDataFields: Set<String> = DEFAULT_IGNORED_DATA_FIELDS
    ): List<DivergenceDetail> {
        val divergences = mutableListOf<DivergenceDetail>()

        // Filter out ignored types
        val legacyEffects = legacy.effects.filter { it.type !in ignoredTypes }
        val refactoredEffects = refactored.effects.filter { it.type !in ignoredTypes }

        // Check count difference
        if (legacyEffects.size != refactoredEffects.size) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.SIDE_EFFECT,
                    severity = DivergenceSeverity.HIGH,
                    message = "Side effect count mismatch in $methodName: legacy=${legacyEffects.size}, refactored=${refactoredEffects.size}",
                    legacyValue = legacyEffects.size,
                    refactoredValue = refactoredEffects.size,
                    metadata = mapOf(
                        "method" to methodName,
                        "legacyTypes" to legacyEffects.map { it.type },
                        "refactoredTypes" to refactoredEffects.map { it.type }
                    )
                )
            )
        }

        // Compare based on order requirement
        if (orderMatters) {
            divergences.addAll(
                compareOrdered(
                    legacyEffects,
                    refactoredEffects,
                    methodName,
                    ignoredDataFields
                )
            )
        } else {
            divergences.addAll(
                compareUnordered(
                    legacyEffects,
                    refactoredEffects,
                    methodName,
                    ignoredDataFields
                )
            )
        }

        return divergences
    }

    /**
     * Compare side effects preserving order
     */
    private fun compareOrdered(
        legacy: List<SideEffect>,
        refactored: List<SideEffect>,
        methodName: String,
        ignoredDataFields: Set<String>
    ): List<DivergenceDetail> {
        val divergences = mutableListOf<DivergenceDetail>()

        val minSize = minOf(legacy.size, refactored.size)

        // Compare matching positions
        for (i in 0 until minSize) {
            if (!legacy[i].isEquivalentTo(refactored[i], ignoredDataFields)) {
                val severity = when (legacy[i].type) {
                    SideEffectType.DATABASE_INSERT,
                    SideEffectType.DATABASE_UPDATE,
                    SideEffectType.DATABASE_DELETE -> DivergenceSeverity.CRITICAL

                    SideEffectType.BROADCAST_SENT,
                    SideEffectType.SERVICE_STARTED,
                    SideEffectType.SERVICE_STOPPED -> DivergenceSeverity.HIGH

                    else -> DivergenceSeverity.MEDIUM
                }

                divergences.add(
                    DivergenceDetail(
                        category = DivergenceCategory.SIDE_EFFECT,
                        severity = severity,
                        message = "Side effect mismatch at position $i in $methodName",
                        legacyValue = legacy[i],
                        refactoredValue = refactored[i],
                        metadata = mapOf("method" to methodName, "position" to i)
                    )
                )
            }
        }

        // Handle extra effects
        if (legacy.size > minSize) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.SIDE_EFFECT,
                    severity = DivergenceSeverity.HIGH,
                    message = "Legacy has ${legacy.size - minSize} extra side effect(s) in $methodName",
                    legacyValue = legacy.subList(minSize, legacy.size),
                    refactoredValue = null,
                    metadata = mapOf("method" to methodName)
                )
            )
        }

        if (refactored.size > minSize) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.SIDE_EFFECT,
                    severity = DivergenceSeverity.HIGH,
                    message = "Refactored has ${refactored.size - minSize} extra side effect(s) in $methodName",
                    legacyValue = null,
                    refactoredValue = refactored.subList(minSize, refactored.size),
                    metadata = mapOf("method" to methodName)
                )
            )
        }

        return divergences
    }

    /**
     * Compare side effects ignoring order
     */
    private fun compareUnordered(
        legacy: List<SideEffect>,
        refactored: List<SideEffect>,
        methodName: String,
        ignoredDataFields: Set<String>
    ): List<DivergenceDetail> {
        val divergences = mutableListOf<DivergenceDetail>()

        // Find effects in legacy but not in refactored
        val unmatchedLegacy = legacy.filter { legacyEffect ->
            refactored.none { it.isEquivalentTo(legacyEffect, ignoredDataFields) }
        }

        if (unmatchedLegacy.isNotEmpty()) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.SIDE_EFFECT,
                    severity = DivergenceSeverity.HIGH,
                    message = "Side effects only in legacy in $methodName",
                    legacyValue = unmatchedLegacy,
                    refactoredValue = null,
                    metadata = mapOf(
                        "method" to methodName,
                        "count" to unmatchedLegacy.size,
                        "types" to unmatchedLegacy.map { it.type }
                    )
                )
            )
        }

        // Find effects in refactored but not in legacy
        val unmatchedRefactored = refactored.filter { refactoredEffect ->
            legacy.none { it.isEquivalentTo(refactoredEffect, ignoredDataFields) }
        }

        if (unmatchedRefactored.isNotEmpty()) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.SIDE_EFFECT,
                    severity = DivergenceSeverity.HIGH,
                    message = "Side effects only in refactored in $methodName",
                    legacyValue = null,
                    refactoredValue = unmatchedRefactored,
                    metadata = mapOf(
                        "method" to methodName,
                        "count" to unmatchedRefactored.size,
                        "types" to unmatchedRefactored.map { it.type }
                    )
                )
            )
        }

        return divergences
    }

    companion object {
        private const val TAG = "SideEffectComparator"

        /**
         * Side effect types to ignore by default
         */
        val DEFAULT_IGNORED_TYPES: Set<SideEffectType> = emptySet<SideEffectType>()

        /**
         * Data fields to ignore in comparison
         */
        val DEFAULT_IGNORED_DATA_FIELDS = setOf<String>(
            "timestamp",
            "threadId",
            "correlationId"
        )
    }
}
