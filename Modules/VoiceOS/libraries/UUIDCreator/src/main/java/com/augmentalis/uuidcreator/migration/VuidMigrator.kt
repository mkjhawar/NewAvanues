/**
 * VuidMigrator.kt - Migration utilities for VUID format conversion
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-30
 *
 * Responsibility: Provide utilities to migrate legacy VUIDs to compact format
 *
 * Features:
 * - Single VUID migration
 * - Batch migration with progress tracking
 * - Lookup table maintenance (legacy → compact)
 * - Format detection and validation
 */
package com.augmentalis.uuidcreator.migration

import android.util.Log
import com.augmentalis.vuid.core.VUIDGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Migration utilities for converting legacy VUIDs to compact format
 *
 * Usage:
 * ```kotlin
 * // Single migration
 * val compact = VuidMigrator.migrate(legacyVuid, version)
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
     * In-memory lookup table for legacy → compact mappings
     * Enables fast reverse lookups during transition period
     */
    private val lookupTable = ConcurrentHashMap<String, String>()

    // ========================================================================
    // Single Migration
    // ========================================================================

    /**
     * Migrate a single legacy VUID to compact format
     *
     * @param legacyVuid Legacy VUID string
     * @param version App version (optional, used if not embedded in legacy format)
     * @return Compact VUID or original if already compact/cannot migrate
     */
    fun migrate(legacyVuid: String, version: String? = null): String {
        // Check lookup table first
        lookupTable[legacyVuid]?.let { return it }

        // Use shared VUIDGenerator's migration
        val compactVuid = VUIDGenerator.migrateToCompact(legacyVuid, version)
            ?: legacyVuid // Return original if cannot migrate

        // Cache the mapping if migration occurred
        if (compactVuid != legacyVuid) {
            lookupTable[legacyVuid] = compactVuid
            Log.d(TAG, "Migrated: $legacyVuid → $compactVuid")
        }

        return compactVuid
    }

    /**
     * Migrate if needed, returning null if already compact
     *
     * @return Compact VUID if migration needed, null if already compact
     */
    fun migrateIfNeeded(vuid: String, version: String? = null): String? {
        return if (VUIDGenerator.isCompact(vuid)) {
            null // Already compact, no migration needed
        } else {
            val compact = migrate(vuid, version)
            if (compact != vuid) compact else null
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
        val success: List<Pair<String, String>>,   // legacy → compact
        val skipped: List<String>,                  // Already compact
        val failed: List<Pair<String, String?>>     // legacy → error message
    )

    /**
     * Migrate a batch of VUIDs with progress tracking
     *
     * @param vuids List of VUIDs to migrate
     * @param version Default version for VUIDs without embedded version
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
                    VUIDGenerator.isCompact(vuid) -> {
                        skipped.add(vuid)
                    }
                    else -> {
                        val compact = VUIDGenerator.migrateToCompact(vuid, version)
                        if (compact != null && compact != vuid) {
                            success.add(vuid to compact)
                            lookupTable[vuid] = compact
                        } else {
                            failed.add(vuid to "Cannot determine format")
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
     * Get compact VUID from legacy (from lookup table)
     */
    fun lookupCompact(legacyVuid: String): String? = lookupTable[legacyVuid]

    /**
     * Get legacy VUID from compact (reverse lookup)
     */
    fun lookupLegacy(compactVuid: String): String? {
        return lookupTable.entries.find { it.value == compactVuid }?.key
    }

    /**
     * Add mapping to lookup table
     */
    fun addMapping(legacyVuid: String, compactVuid: String) {
        lookupTable[legacyVuid] = compactVuid
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
    fun detectFormat(vuid: String): VUIDGenerator.VuidFormat {
        return when {
            VUIDGenerator.isCompactApp(vuid) -> VUIDGenerator.VuidFormat.COMPACT_APP
            VUIDGenerator.isCompactModule(vuid) -> VUIDGenerator.VuidFormat.COMPACT_MODULE
            VUIDGenerator.isCompactSimple(vuid) -> VUIDGenerator.VuidFormat.COMPACT_SIMPLE
            VUIDGenerator.isLegacyUuid(vuid) -> VUIDGenerator.VuidFormat.LEGACY_UUID
            VUIDGenerator.isLegacyVoiceOS(vuid) -> VUIDGenerator.VuidFormat.LEGACY_VOICEOS
            else -> VUIDGenerator.VuidFormat.UNKNOWN
        }
    }

    /**
     * Check if VUID needs migration
     */
    fun needsMigration(vuid: String): Boolean {
        return !VUIDGenerator.isCompact(vuid) && VUIDGenerator.isValid(vuid)
    }

    /**
     * Parse VUID into components
     */
    fun parse(vuid: String) = VUIDGenerator.parse(vuid)
}
