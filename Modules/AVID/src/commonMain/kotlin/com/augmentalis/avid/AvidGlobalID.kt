/**
 * AvidGlobalID.kt - Global AVID Generation (Synced Identifiers)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-01-15
 *
 * Global identifiers that are synced across devices.
 *
 * ## Format
 * ```
 * AVID-{platform}-{sequence}
 * Example: AVID-A-000001
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

/**
 * Global AVID generator for synced identifiers
 *
 * Use this for IDs that need to be consistent across devices after sync.
 */
object AvidGlobalID {

    private const val PREFIX = "AVID"
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
     * Generate a global AVID (synced across devices)
     *
     * @param platform Target platform
     * @param seq Optional sequence number (auto-increments if null)
     * @return AVID string (e.g., "AVID-A-000001")
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
    // Convenience Methods - Messages & Conversations (AVA)
    // ========================================================================

    fun generateMessageId(): String = generate()
    fun generateConversationId(): String = generate()
    fun generateDocumentId(): String = generate()
    fun generateChunkId(): String = generate()
    fun generateMemoryId(): String = generate()
    fun generateDecisionId(): String = generate()
    fun generateLearningId(): String = generate()
    fun generateIntentId(): String = generate()
    fun generateClusterId(): String = generate()
    fun generateBookmarkId(): String = generate()
    fun generateAnnotationId(): String = generate()
    fun generateFilterId(): String = generate()
    fun generateUtteranceId(): String = generate()
    fun generateDialogId(): String = generate()

    // ========================================================================
    // Convenience Methods - Browser (WebAvanue)
    // ========================================================================

    fun generateTabId(): String = generate()
    fun generateFavoriteId(): String = generate()
    fun generateDownloadId(): String = generate()
    fun generateHistoryId(): String = generate()
    fun generateSessionId(): String = generate()
    fun generateGroupId(): String = generate()
    fun generateFolderId(): String = generate()

    // ========================================================================
    // Convenience Methods - System (Cockpit, VoiceOS)
    // ========================================================================

    fun generateRequestId(): String = generate()
    fun generateWindowId(): String = generate()
    fun generateStreamId(): String = generate()
    fun generatePresetId(): String = generate()
    fun generateDeviceId(): String = generate()
    fun generateSyncId(): String = generate()
    fun generateElementId(): String = generate()
    fun generateCommandId(): String = generate()
    fun generateScreenId(): String = generate()
    fun generateAppId(): String = generate()

    // ========================================================================
    // Validation
    // ========================================================================

    private val PATTERN = Regex("^AVID-[AIWMXL]-\\d{6,}$")

    /**
     * Check if string is a valid global AVID
     */
    fun isValid(id: String): Boolean = PATTERN.matches(id)

    // ========================================================================
    // Parsing
    // ========================================================================

    /**
     * Parsed global AVID components
     */
    data class Parsed(
        val platform: Platform,
        val sequence: Long
    ) {
        override fun toString(): String {
            return "$PREFIX-${platform.code}-${sequence.toString().padStart(SEQUENCE_PADDING, '0')}"
        }
    }

    /**
     * Parse global AVID string into components
     *
     * @param id AVID string
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
     * Extract platform from AVID string
     */
    fun extractPlatform(id: String): Platform? = parse(id)?.platform

    // ========================================================================
    // Sequence Management
    // ========================================================================

    /**
     * Set the sequence counter (for sync restoration)
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

/**
 * Backward compatibility alias
 */
@Deprecated("Use AvidGlobalID instead", ReplaceWith("AvidGlobalID"))
typealias AvidGenerator = AvidGlobalID
