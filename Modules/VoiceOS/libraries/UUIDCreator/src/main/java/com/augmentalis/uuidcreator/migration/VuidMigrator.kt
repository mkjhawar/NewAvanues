/**
 * VuidMigrator.kt - Migration utilities for VUID format conversion
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-30
 *
 * Responsibility: Provide utilities to migrate legacy VUIDs to new format
 *
 * Features:
 * - Single VUID migration
 * - Batch migration with progress tracking
 * - Lookup table maintenance (legacy → new)
 * - Format detection and validation
 */
package com.augmentalis.uuidcreator.migration

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Migration utilities for converting legacy VUIDs to new format
 *
 * Usage:
 * ```kotlin
 * // Single migration
 * val newVuid = VuidMigrator.migrate(legacyVuid)
 *
 * // Batch migration
 * val results = VuidMigrator.migrateBatch(legacyVuids) { progress ->
 *     Log.d("Migration", "Progress: ${progress.percent}%")
 * }
 * ```
 */
object VuidMigrator {
    private const val TAG = "VuidMigrator"

    /**
     * In-memory lookup table for legacy → new mappings
     * Enables fast reverse lookups during transition period
     */
    private val lookupTable = ConcurrentHashMap<String, String>()

    /**
     * VUID format types
     */
    enum class VuidFormat {
        STANDARD_UUID,
        PREFIXED_UUID,
        CONTENT_HASH,
        SEQUENTIAL,
        UNKNOWN
    }

    // ========================================================================
    // Single Migration
    // ========================================================================

    /**
     * Migrate a single legacy VUID to new format
     *
     * @param legacyVuid Legacy VUID string
     * @param version App version (optional)
     * @return New VUID or original if already new format
     */
    fun migrate(legacyVuid: String, version: String? = null): String {
        // Check lookup table first
        lookupTable[legacyVuid]?.let { return it }

        // If already a valid format, return as-is
        if (isValid(legacyVuid)) {
            return legacyVuid
        }

        // Generate new VUID
        val newVuid = UUID.randomUUID().toString()

        // Cache the mapping
        lookupTable[legacyVuid] = newVuid
        Log.d(TAG, "Migrated: $legacyVuid → $newVuid")

        return newVuid
    }

    /**
     * Migrate if needed, returning null if already new format
     *
     * @return New VUID if migration needed, null if already new format
     */
    fun migrateIfNeeded(vuid: String, version: String? = null): String? {
        return if (isValid(vuid)) {
            null // Already valid, no migration needed
        } else {
            val newVuid = migrate(vuid, version)
            if (newVuid != vuid) newVuid else null
        }
    }

    // ========================================================================
    // Batch Migration
    // ========================================================================

    /**
     * Migration progress data
     */
    data class MigrationProgress(
        val total: Int,
        val completed: Int,
        val migrated: Int,
        val skipped: Int,
        val failed: Int
    ) {
        val percent: Int get() = if (total > 0) (completed * 100 / total) else 0
        val isComplete: Boolean get() = completed >= total
    }

    /**
     * Batch migration result
     */
    data class MigrationResult(
        val success: List<Pair<String, String>>,   // legacy → new
        val skipped: List<String>,                  // Already new format
        val failed: List<Pair<String, String?>>     // legacy → error message
    )

    /**
     * Migrate a batch of VUIDs with progress tracking
     *
     * @param vuids List of VUIDs to migrate
     * @param version Default version for VUIDs
     * @param onProgress Progress callback (optional)
     * @return Migration results
     */
    suspend fun migrateBatch(
        vuids: List<String>,
        version: String? = null,
        onProgress: ((MigrationProgress) -> Unit)? = null
    ): MigrationResult = withContext(Dispatchers.Default) {
        val success = mutableListOf<Pair<String, String>>()
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String?>>()

        vuids.forEachIndexed { index, vuid ->
            try {
                when {
                    isValid(vuid) -> {
                        skipped.add(vuid)
                    }
                    else -> {
                        val newVuid = migrate(vuid, version)
                        if (newVuid != vuid) {
                            success.add(vuid to newVuid)
                        } else {
                            failed.add(vuid to "Cannot migrate")
                        }
                    }
                }
            } catch (e: Exception) {
                failed.add(vuid to e.message)
            }

            // Report progress
            onProgress?.invoke(
                MigrationProgress(
                    total = vuids.size,
                    completed = index + 1,
                    migrated = success.size,
                    skipped = skipped.size,
                    failed = failed.size
                )
            )
        }

        MigrationResult(success, skipped, failed)
    }

    // ========================================================================
    // Lookup Table Management
    // ========================================================================

    /**
     * Get new VUID from legacy (from lookup table)
     */
    fun lookupNew(legacyVuid: String): String? = lookupTable[legacyVuid]

    /**
     * Get legacy VUID from new (reverse lookup)
     */
    fun lookupLegacy(newVuid: String): String? {
        return lookupTable.entries.find { it.value == newVuid }?.key
    }

    /**
     * Add mapping to lookup table
     */
    fun addMapping(legacyVuid: String, newVuid: String) {
        lookupTable[legacyVuid] = newVuid
    }

    /**
     * Clear lookup table
     */
    fun clearLookupTable() {
        lookupTable.clear()
    }

    /**
     * Get lookup table size
     */
    fun getLookupTableSize(): Int = lookupTable.size

    /**
     * Export lookup table as map
     */
    fun exportLookupTable(): Map<String, String> = lookupTable.toMap()

    /**
     * Import mappings into lookup table
     */
    fun importMappings(mappings: Map<String, String>) {
        lookupTable.putAll(mappings)
    }

    // ========================================================================
    // Format Detection Utilities
    // ========================================================================

    /**
     * Detect VUID format
     */
    fun detectFormat(vuid: String): VuidFormat {
        return when {
            isStandardUUID(vuid) -> VuidFormat.STANDARD_UUID
            isPrefixedUUID(vuid) -> VuidFormat.PREFIXED_UUID
            isContentHash(vuid) -> VuidFormat.CONTENT_HASH
            isSequential(vuid) -> VuidFormat.SEQUENTIAL
            else -> VuidFormat.UNKNOWN
        }
    }

    /**
     * Check if VUID is valid
     */
    fun isValid(vuid: String): Boolean {
        if (vuid.isBlank()) return false
        return isStandardUUID(vuid) || isPrefixedUUID(vuid) || isContentHash(vuid) || isSequential(vuid)
    }

    /**
     * Check if VUID needs migration
     */
    fun needsMigration(vuid: String): Boolean {
        return !isValid(vuid)
    }

    // Private format checks
    private fun isStandardUUID(vuid: String): Boolean {
        return try {
            UUID.fromString(vuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun isPrefixedUUID(vuid: String): Boolean {
        val parts = vuid.split("-", limit = 2)
        if (parts.size != 2) return false
        return isStandardUUID(parts[1])
    }

    private fun isContentHash(vuid: String): Boolean {
        return vuid.startsWith("content-") && vuid.matches(Regex("^content-[a-f0-9]+-\\d+$"))
    }

    private fun isSequential(vuid: String): Boolean {
        return vuid.startsWith("seq-") && vuid.matches(Regex("^seq-\\d+-\\d+$"))
    }
}
