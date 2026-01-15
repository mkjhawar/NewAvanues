/**
 * AvidLocalID.kt - Local AVID Generation (Pending Sync Identifiers)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-01-15
 *
 * Local identifiers that are pending sync to the server.
 * Once synced, these are promoted to global AVIDs.
 *
 * ## Format
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
 *
 * ## Workflow
 * 1. Create local ID: `AVIDL-A-000001`
 * 2. Store locally with pending sync flag
 * 3. On sync, server assigns global sequence
 * 4. Promote to global: `AVID-A-000001`
 */
package com.augmentalis.avid

/**
 * Local AVID generator for pending-sync identifiers
 *
 * Use this for IDs created offline that will be synced later.
 */
object AvidLocalID {

    private const val PREFIX = "AVIDL"
    private const val GLOBAL_PREFIX = "AVID"
    private const val SEQUENCE_PADDING = 6

    // Thread-safe sequence counter
    private var sequence: Long = 0L
    private val sequenceLock = Any()

    // Current platform (set by platform-specific initialization)
    private var currentPlatform: Platform = Platform.ANDROID

    // ========================================================================
    // Platform Configuration
    // ========================================================================

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
     * Generate a local AVIDL (pending sync)
     *
     * @param platform Target platform
     * @param seq Optional sequence number (auto-increments if null)
     * @return AVIDL string (e.g., "AVIDL-A-000047")
     */
    fun generate(platform: Platform, seq: Long? = null): String {
        val seqNum = seq ?: synchronized(sequenceLock) { ++sequence }
        val paddedSeq = seqNum.toString().padStart(SEQUENCE_PADDING, '0')
        return "$PREFIX-${platform.code}-$paddedSeq"
    }

    /**
     * Generate using current platform (set via setPlatform)
     */
    fun generate(seq: Long? = null): String = generate(currentPlatform, seq)

    // ========================================================================
    // Convenience Methods - Offline Creation
    // ========================================================================

    fun generateMessageId(): String = generate()
    fun generateConversationId(): String = generate()
    fun generateDocumentId(): String = generate()
    fun generateBookmarkId(): String = generate()
    fun generateAnnotationId(): String = generate()

    // ========================================================================
    // Validation
    // ========================================================================

    private val PATTERN = Regex("^AVIDL-[AIWMXL]-\\d{6,}$")

    /**
     * Check if string is a valid local AVIDL
     */
    fun isValid(id: String): Boolean = PATTERN.matches(id)

    // ========================================================================
    // Parsing
    // ========================================================================

    /**
     * Parsed local AVIDL components
     */
    data class Parsed(
        val platform: Platform,
        val sequence: Long
    ) {
        /**
         * Convert to global AVID format (after sync assigns global sequence)
         *
         * @param globalSequence The server-assigned global sequence
         * @return Global AVID string
         */
        fun toGlobal(globalSequence: Long): String {
            return "$GLOBAL_PREFIX-${platform.code}-${globalSequence.toString().padStart(SEQUENCE_PADDING, '0')}"
        }

        /**
         * Reconstruct the local AVIDL string
         */
        override fun toString(): String {
            return "$PREFIX-${platform.code}-${sequence.toString().padStart(SEQUENCE_PADDING, '0')}"
        }
    }

    /**
     * Parse local AVIDL string into components
     *
     * @param id AVIDL string
     * @return Parsed components or null if invalid
     */
    fun parse(id: String): Parsed? {
        if (!isValid(id)) return null

        val parts = id.split("-")
        if (parts.size != 3) return null

        val platform = Platform.fromCode(parts[1].firstOrNull() ?: return null) ?: return null
        val seq = parts[2].toLongOrNull() ?: return null

        return Parsed(platform, seq)
    }

    /**
     * Extract platform from AVIDL string
     */
    fun extractPlatform(id: String): Platform? = parse(id)?.platform

    // ========================================================================
    // Promotion (Local → Global)
    // ========================================================================

    /**
     * Promote local AVIDL to global AVID
     *
     * Call this after server assigns a global sequence number.
     *
     * @param localId AVIDL string
     * @param globalSequence Server-assigned global sequence
     * @return AVID string or null if invalid local ID
     */
    fun promoteToGlobal(localId: String, globalSequence: Long): String? {
        val parsed = parse(localId) ?: return null
        return parsed.toGlobal(globalSequence)
    }

    /**
     * Check if an ID needs promotion (is local)
     */
    fun needsPromotion(id: String): Boolean = isValid(id)

    // ========================================================================
    // Sequence Management
    // ========================================================================

    /**
     * Set the sequence counter
     */
    fun setSequence(value: Long) {
        synchronized(sequenceLock) {
            sequence = value
        }
    }

    /**
     * Get current sequence value
     */
    fun getSequence(): Long = synchronized(sequenceLock) { sequence }

    /**
     * Reset sequence (for testing)
     */
    fun reset() {
        synchronized(sequenceLock) {
            sequence = 0L
        }
    }
}
