/**
 * AvidGenerator.kt - Avanues Voice ID Generator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-01-13
 *
 * Universal identifier system for the Avanues ecosystem.
 *
 * ## Formats
 *
 * ### AVID (Global, Synced)
 * ```
 * AVID-{platform}-{sequence}
 * Example: AVID-A-000001
 * ```
 *
 * ### AVIDL (Local, Pending Sync)
 * ```
 * AVIDL-{platform}-{sequence}
 * Example: AVIDL-A-000047
 * ```
 *
 * ## Platform Codes
 * - A = Android
 * - I = iOS
 * - W = Web
 * - M = macOS
 * - X = Windows
 * - L = Linux
 */
package com.augmentalis.avid

import kotlin.random.Random

/**
 * Platform-specific current time in milliseconds.
 * Used for timestamp-based operations.
 */
internal expect fun currentTimeMillis(): Long

/**
 * Central AVID generator for the Avanues ecosystem
 */
object AvidGenerator {

    private const val AVID_PREFIX = "AVID"
    private const val AVIDL_PREFIX = "AVIDL"
    private const val SEQUENCE_PADDING = 6

    // Thread-safe sequence counters
    private var globalSequence: Long = 0L
    private var localSequence: Long = 0L
    private val sequenceLock = Any()

    // Current platform (set by platform-specific initialization)
    private var currentPlatform: Platform = Platform.ANDROID

    /**
     * Set the current platform (called during app initialization)
     */
    fun setPlatform(platform: Platform) {
        currentPlatform = platform
    }

    /**
     * Get the current platform
     */
    fun getPlatform(): Platform = currentPlatform

    // ========================================================================
    // Core Generation
    // ========================================================================

    /**
     * Generate a global AVID (synced across devices)
     *
     * @param platform Target platform
     * @param sequence Optional sequence number (auto-increments if null)
     * @return AVID string (e.g., "AVID-A-000001")
     */
    fun generate(platform: Platform, sequence: Long? = null): String {
        val seq = sequence ?: synchronized(sequenceLock) { ++globalSequence }
        val paddedSeq = seq.toString().padStart(SEQUENCE_PADDING, '0')
        return "$AVID_PREFIX-${platform.code}-$paddedSeq"
    }

    /**
     * Generate a local AVIDL (pending sync)
     *
     * @param platform Target platform
     * @param sequence Optional sequence number (auto-increments if null)
     * @return AVIDL string (e.g., "AVIDL-A-000047")
     */
    fun generateLocal(platform: Platform, sequence: Long? = null): String {
        val seq = sequence ?: synchronized(sequenceLock) { ++localSequence }
        val paddedSeq = seq.toString().padStart(SEQUENCE_PADDING, '0')
        return "$AVIDL_PREFIX-${platform.code}-$paddedSeq"
    }

    /**
     * Generate using current platform (set via setPlatform)
     */
    fun generate(sequence: Long? = null): String = generate(currentPlatform, sequence)

    /**
     * Generate local using current platform
     */
    fun generateLocal(sequence: Long? = null): String = generateLocal(currentPlatform, sequence)

    // ========================================================================
    // Convenience Methods - Messages & Conversations (AVA)
    // ========================================================================

    fun generateMessageId(): String = generate(currentPlatform)
    fun generateConversationId(): String = generate(currentPlatform)
    fun generateDocumentId(): String = generate(currentPlatform)
    fun generateChunkId(): String = generate(currentPlatform)
    fun generateMemoryId(): String = generate(currentPlatform)
    fun generateDecisionId(): String = generate(currentPlatform)
    fun generateLearningId(): String = generate(currentPlatform)
    fun generateIntentId(): String = generate(currentPlatform)
    fun generateClusterId(): String = generate(currentPlatform)
    fun generateBookmarkId(): String = generate(currentPlatform)
    fun generateAnnotationId(): String = generate(currentPlatform)
    fun generateFilterId(): String = generate(currentPlatform)
    fun generateUtteranceId(): String = generate(currentPlatform)
    fun generateDialogId(): String = generate(currentPlatform)

    // ========================================================================
    // Convenience Methods - Browser (WebAvanue)
    // ========================================================================

    fun generateTabId(): String = generate(currentPlatform)
    fun generateFavoriteId(): String = generate(currentPlatform)
    fun generateDownloadId(): String = generate(currentPlatform)
    fun generateHistoryId(): String = generate(currentPlatform)
    fun generateSessionId(): String = generate(currentPlatform)
    fun generateGroupId(): String = generate(currentPlatform)
    fun generateFolderId(): String = generate(currentPlatform)

    // ========================================================================
    // Convenience Methods - System (Cockpit, VoiceOS)
    // ========================================================================

    fun generateRequestId(): String = generate(currentPlatform)
    fun generateWindowId(): String = generate(currentPlatform)
    fun generateStreamId(): String = generate(currentPlatform)
    fun generatePresetId(): String = generate(currentPlatform)
    fun generateDeviceId(): String = generate(currentPlatform)
    fun generateSyncId(): String = generate(currentPlatform)
    fun generateElementId(): String = generate(currentPlatform)
    fun generateCommandId(): String = generate(currentPlatform)
    fun generateScreenId(): String = generate(currentPlatform)
    fun generateAppId(): String = generate(currentPlatform)

    // ========================================================================
    // Validation
    // ========================================================================

    private val AVID_PATTERN = Regex("^AVID-[AIWMXL]-\\d{6,}$")
    private val AVIDL_PATTERN = Regex("^AVIDL-[AIWMXL]-\\d{6,}$")

    /**
     * Check if string is a valid AVID (global)
     */
    fun isAvid(id: String): Boolean = AVID_PATTERN.matches(id)

    /**
     * Check if string is a valid AVIDL (local)
     */
    fun isAvidl(id: String): Boolean = AVIDL_PATTERN.matches(id)

    /**
     * Check if string is any valid AVID format
     */
    fun isValid(id: String): Boolean = isAvid(id) || isAvidl(id)

    // ========================================================================
    // Parsing
    // ========================================================================

    /**
     * Parsed AVID components
     */
    data class ParsedAvid(
        val isLocal: Boolean,
        val platform: Platform,
        val sequence: Long
    ) {
        /**
         * Convert local AVIDL to global AVID (for after sync)
         */
        fun toGlobal(): String = "$AVID_PREFIX-${platform.code}-${sequence.toString().padStart(SEQUENCE_PADDING, '0')}"

        /**
         * Reconstruct the AVID string
         */
        override fun toString(): String {
            val prefix = if (isLocal) AVIDL_PREFIX else AVID_PREFIX
            return "$prefix-${platform.code}-${sequence.toString().padStart(SEQUENCE_PADDING, '0')}"
        }
    }

    /**
     * Parse AVID/AVIDL string into components
     *
     * @param id AVID or AVIDL string
     * @return Parsed components or null if invalid
     */
    fun parse(id: String): ParsedAvid? {
        if (!isValid(id)) return null

        val parts = id.split("-")
        if (parts.size != 3) return null

        val isLocal = parts[0] == AVIDL_PREFIX
        val platform = Platform.fromCode(parts[1].firstOrNull() ?: return null) ?: return null
        val sequence = parts[2].toLongOrNull() ?: return null

        return ParsedAvid(isLocal, platform, sequence)
    }

    /**
     * Extract platform from AVID string
     */
    fun extractPlatform(id: String): Platform? = parse(id)?.platform

    /**
     * Promote local AVIDL to global AVID
     *
     * @param localId AVIDL string
     * @return AVID string or null if invalid
     */
    fun promoteToGlobal(localId: String): String? {
        val parsed = parse(localId) ?: return null
        if (!parsed.isLocal) return localId // Already global
        return parsed.toGlobal()
    }

    // ========================================================================
    // Sequence Management
    // ========================================================================

    /**
     * Set the global sequence counter (for sync restoration)
     */
    fun setGlobalSequence(value: Long) {
        synchronized(sequenceLock) {
            globalSequence = value
        }
    }

    /**
     * Set the local sequence counter
     */
    fun setLocalSequence(value: Long) {
        synchronized(sequenceLock) {
            localSequence = value
        }
    }

    /**
     * Get current global sequence value
     */
    fun getGlobalSequence(): Long = synchronized(sequenceLock) { globalSequence }

    /**
     * Get current local sequence value
     */
    fun getLocalSequence(): Long = synchronized(sequenceLock) { localSequence }

    /**
     * Reset all sequences (for testing)
     */
    fun reset() {
        synchronized(sequenceLock) {
            globalSequence = 0L
            localSequence = 0L
        }
    }
}
