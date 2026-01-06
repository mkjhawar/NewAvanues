/**
 * ScreenFingerprinter.kt - Screen identity fingerprinting
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP version of screen fingerprinting for identity and deduplication.
 * Generates deterministic hashes for UI screens.
 */
package com.augmentalis.voiceoscoreng.fingerprinting

import com.augmentalis.voiceoscoreng.speech.currentTimeMillis

/**
 * Screen fingerprinter for generating unique screen identifiers.
 *
 * Features:
 * - Content-based fingerprinting (normal screens)
 * - Structural fingerprinting (dynamic content screens)
 * - Popup/dialog detection
 * - Stable hashing across visits
 *
 * Implementations:
 * - AndroidScreenFingerprinter (uses AccessibilityNodeInfo)
 * - IOSScreenFingerprinter (uses UIAccessibility)
 * - DesktopScreenFingerprinter (uses platform APIs)
 */
interface IScreenFingerprinter {

    /**
     * Calculate fingerprint for a screen.
     *
     * @param root Platform-specific root node
     * @return SHA-256 hash of screen content
     */
    fun calculateFingerprint(root: Any?): String

    /**
     * Calculate structural fingerprint (for dynamic content).
     *
     * Uses element types and resource IDs instead of text content.
     * Stable for screens like chat lists, feeds, notifications.
     *
     * @param root Platform-specific root node
     * @return SHA-256 hash of screen structure
     */
    fun calculateStructuralFingerprint(root: Any?): String

    /**
     * Calculate popup/dialog fingerprint.
     *
     * @param root Platform-specific root node
     * @param popupType Type of popup (dialog, menu, tooltip, etc.)
     * @return SHA-256 hash for popup
     */
    fun calculatePopupFingerprint(root: Any?, popupType: PopupType): String

    /**
     * Detect if screen has dynamic content (lists, feeds, etc.)
     *
     * @param root Platform-specific root node
     * @return True if screen has dynamic content
     */
    fun isDynamicContentScreen(root: Any?): Boolean

    /**
     * Detect if current view is a popup.
     *
     * @param root Platform-specific root node
     * @return PopupInfo with detection result
     */
    fun detectPopup(root: Any?): PopupInfo
}

/**
 * Popup types for classification.
 */
enum class PopupType {
    DIALOG,
    ALERT,
    MENU,
    DROPDOWN,
    TOOLTIP,
    BOTTOM_SHEET,
    SNACKBAR,
    TOAST,
    CONFIRMATION,
    UNKNOWN
}

/**
 * Popup detection result.
 */
data class PopupInfo(
    val isPopup: Boolean,
    val popupType: PopupType,
    val title: String?,
    val hasPositiveAction: Boolean = false,
    val hasNegativeAction: Boolean = false
)

/**
 * Common fingerprinting utilities.
 */
object FingerprintUtils {

    /**
     * Empty hash for null/invalid inputs.
     */
    const val EMPTY_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

    /**
     * Calculate SHA-256 hash of input string.
     */
    fun calculateSHA256(input: String): String {
        if (input.isEmpty()) return EMPTY_HASH

        // Platform-specific implementation via expect/actual
        return sha256(input)
    }

    /**
     * Normalize text for consistent hashing.
     * - Lowercase
     * - Remove extra whitespace
     * - Remove dynamic content (timestamps, counts, etc.)
     */
    fun normalizeText(text: String?): String {
        if (text.isNullOrBlank()) return ""

        return text
            .lowercase()
            .replace(Regex("\\d+:\\d+\\s*(am|pm)?", RegexOption.IGNORE_CASE), "[TIME]")  // Timestamps
            .replace(Regex("\\d+\\s*min(ute)?s?\\s*ago", RegexOption.IGNORE_CASE), "[RELATIVE_TIME]")
            .replace(Regex("\\d+\\s*hour?s?\\s*ago", RegexOption.IGNORE_CASE), "[RELATIVE_TIME]")
            .replace(Regex("\\d+\\s*day?s?\\s*ago", RegexOption.IGNORE_CASE), "[RELATIVE_TIME]")
            .replace(Regex("\\(\\d+\\)"), "[COUNT]")  // Badge counts like (5)
            .replace(Regex("\\b\\d+\\+\\b"), "[COUNT+]")  // 99+ style counts
            .replace(Regex("\\s+"), " ")  // Normalize whitespace
            .trim()
    }

    /**
     * Check if text contains dynamic patterns (should use structural hash).
     */
    fun containsDynamicPatterns(text: String?): Boolean {
        if (text.isNullOrBlank()) return false

        val dynamicPatterns = listOf(
            Regex("\\d+:\\d+", RegexOption.IGNORE_CASE),  // Time
            Regex("\\d+\\s*(min|hour|day|week)s?\\s*ago", RegexOption.IGNORE_CASE),
            Regex("\\(\\d+\\)"),  // Count badges
            Regex("\\d+\\+"),  // 99+ counts
            Regex("just now", RegexOption.IGNORE_CASE),
            Regex("yesterday", RegexOption.IGNORE_CASE),
            Regex("today", RegexOption.IGNORE_CASE)
        )

        return dynamicPatterns.any { it.containsMatchIn(text) }
    }
}

/**
 * Platform-specific SHA-256 implementation.
 */
expect fun sha256(input: String): String

/**
 * Screen state representation.
 */
data class ScreenState(
    val hash: String,
    val isPopup: Boolean = false,
    val popupType: PopupType = PopupType.UNKNOWN,
    val elementCount: Int = 0,
    val structuralHash: String? = null,
    val timestamp: Long = currentTimeMillis()
) {
    /**
     * Check if this represents the same screen as another state.
     */
    fun isSameScreen(other: ScreenState): Boolean {
        // For popups, compare popup hash
        if (isPopup && other.isPopup) {
            return hash == other.hash
        }

        // For regular screens, compare main hash
        if (!isPopup && !other.isPopup) {
            return hash == other.hash
        }

        // Different types (popup vs regular)
        return false
    }
}
