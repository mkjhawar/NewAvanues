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
package com.augmentalis.voiceoscore

import com.augmentalis.foundation.util.HashUtils
import com.augmentalis.voiceoscore.currentTimeMillis

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
        return HashUtils.sha256(input)
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
 * Default screen fingerprinter implementation.
 *
 * This implementation works with List<ElementInfo> passed as Any?.
 * For platform-specific implementations that need AccessibilityNodeInfo
 * or other platform types, create platform-specific subclasses.
 */
class ScreenFingerprinter : IScreenFingerprinter {

    override fun calculateFingerprint(root: Any?): String {
        if (root == null) return FingerprintUtils.EMPTY_HASH

        // Handle List<ElementInfo> - the typical use case from exploration
        @Suppress("UNCHECKED_CAST")
        val elements = when (root) {
            is List<*> -> root as? List<com.augmentalis.voiceoscore.ElementInfo>
            else -> null
        }

        if (elements == null || elements.isEmpty()) {
            return FingerprintUtils.EMPTY_HASH
        }

        // Build fingerprint from element properties
        val fingerprintInput = elements
            .sortedBy { "${it.bounds.left},${it.bounds.top}" }
            .joinToString("|") { element ->
                val normalizedText = FingerprintUtils.normalizeText(element.text)
                val normalizedDesc = FingerprintUtils.normalizeText(element.contentDescription)
                "${element.className}:${element.resourceId}:$normalizedText:$normalizedDesc"
            }

        return FingerprintUtils.calculateSHA256(fingerprintInput)
    }

    override fun calculateStructuralFingerprint(root: Any?): String {
        if (root == null) return FingerprintUtils.EMPTY_HASH

        @Suppress("UNCHECKED_CAST")
        val elements = when (root) {
            is List<*> -> root as? List<com.augmentalis.voiceoscore.ElementInfo>
            else -> null
        }

        if (elements == null || elements.isEmpty()) {
            return FingerprintUtils.EMPTY_HASH
        }

        // Structure only - no text content
        val structureInput = elements
            .sortedBy { "${it.bounds.left},${it.bounds.top}" }
            .joinToString("|") { element ->
                "${element.className}:${element.resourceId}:${element.isClickable}"
            }

        return FingerprintUtils.calculateSHA256(structureInput)
    }

    override fun calculatePopupFingerprint(root: Any?, popupType: PopupType): String {
        val baseHash = calculateFingerprint(root)
        return FingerprintUtils.calculateSHA256("popup:${popupType.name}:$baseHash")
    }

    override fun isDynamicContentScreen(root: Any?): Boolean {
        if (root == null) return false

        @Suppress("UNCHECKED_CAST")
        val elements = when (root) {
            is List<*> -> root as? List<com.augmentalis.voiceoscore.ElementInfo>
            else -> null
        }

        if (elements == null) return false

        // Check for dynamic patterns in element text
        val dynamicCount = elements.count { element ->
            FingerprintUtils.containsDynamicPatterns(element.text) ||
            FingerprintUtils.containsDynamicPatterns(element.contentDescription)
        }

        // Consider dynamic if >20% of elements have dynamic content
        return dynamicCount.toFloat() / elements.size.coerceAtLeast(1) > 0.2f
    }

    override fun detectPopup(root: Any?): PopupInfo {
        if (root == null) return PopupInfo(false, PopupType.UNKNOWN, null)

        @Suppress("UNCHECKED_CAST")
        val elements = when (root) {
            is List<*> -> root as? List<com.augmentalis.voiceoscore.ElementInfo>
            else -> null
        }

        if (elements == null) return PopupInfo(false, PopupType.UNKNOWN, null)

        // Look for popup indicators
        val dialogIndicators = listOf("Dialog", "AlertDialog", "PopupWindow", "BottomSheet")
        val hasDialogClass = elements.any { element ->
            dialogIndicators.any { indicator -> element.className.contains(indicator, ignoreCase = true) }
        }

        if (!hasDialogClass) {
            return PopupInfo(false, PopupType.UNKNOWN, null)
        }

        // Determine popup type
        val popupType = when {
            elements.any { it.className.contains("BottomSheet", ignoreCase = true) } -> PopupType.BOTTOM_SHEET
            elements.any { it.className.contains("AlertDialog", ignoreCase = true) } -> PopupType.ALERT
            elements.any { it.className.contains("Dialog", ignoreCase = true) } -> PopupType.DIALOG
            else -> PopupType.UNKNOWN
        }

        // Find title (usually first text element)
        val title = elements.firstOrNull { !it.text.isNullOrBlank() }?.text

        // Check for action buttons
        val hasPositive = elements.any { element ->
            val text = element.text?.lowercase() ?: ""
            text in listOf("ok", "yes", "confirm", "accept", "submit", "done", "save")
        }
        val hasNegative = elements.any { element ->
            val text = element.text?.lowercase() ?: ""
            text in listOf("cancel", "no", "dismiss", "close", "back")
        }

        return PopupInfo(
            isPopup = true,
            popupType = popupType,
            title = title,
            hasPositiveAction = hasPositive,
            hasNegativeAction = hasNegative
        )
    }
}

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
