/**
 * ConfidenceOverlay.kt - Speech recognition confidence display overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * Ported from VoiceOSCore ConfidenceOverlay.kt (297 lines)
 * KMP multiplatform implementation with StateFlow for reactive UI updates.
 */
package com.augmentalis.voiceoscore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ═══════════════════════════════════════════════════════════════════════════
// Confidence Level
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Semantic confidence labels for user-friendly display.
 *
 * Reduces cognitive load by using meaningful labels instead of percentages:
 * - [HIGH_CONFIDENCE]: Recognition is reliable, safe to proceed
 * - [NEEDS_CONFIRMATION]: Recognition is uncertain, ask user to confirm
 * - [LOW_CONFIDENCE]: Recognition is poor, ask user to repeat
 *
 * @property label The user-facing label text
 */
enum class SemanticConfidenceLabel(val label: String) {
    /**
     * High confidence - recognition is reliable.
     * Display: "High" (green)
     */
    HIGH_CONFIDENCE("High"),

    /**
     * Needs confirmation - recognition is uncertain.
     * Display: "Confirm?" (yellow)
     */
    NEEDS_CONFIRMATION("Confirm?"),

    /**
     * Low confidence - recognition is poor.
     * Display: "Repeat" (red)
     */
    LOW_CONFIDENCE("Repeat")
}

/**
 * Speech recognition confidence level thresholds.
 *
 * Used to provide visual feedback about recognition quality:
 * - [HIGH] (>= 0.8): Strong match, command can be executed
 * - [MEDIUM] (0.6-0.8): Moderate match, may need confirmation
 * - [LOW] (0.4-0.6): Weak match, consider alternatives
 * - [REJECT] (< 0.4): Too low, ask user to repeat
 */
enum class ConfidenceLevel {
    /**
     * High confidence (>= 0.8).
     * Strong recognition match - safe to execute command.
     */
    HIGH,

    /**
     * Medium confidence (0.6 - 0.8).
     * Moderate match - consider confirmation or alternatives.
     */
    MEDIUM,

    /**
     * Low confidence (0.4 - 0.6).
     * Weak match - show alternatives, may need user selection.
     */
    LOW,

    /**
     * Rejected (< 0.4).
     * Recognition quality too low - ask user to repeat.
     */
    REJECT
}

// ═══════════════════════════════════════════════════════════════════════════
// Confidence Result
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Speech recognition confidence result.
 *
 * Contains the confidence score, recognized text, and optional alternatives
 * from the speech recognition engine.
 *
 * @property confidence Recognition confidence value (0.0 to 1.0)
 * @property recognizedText The primary recognized text
 * @property alternatives Alternative interpretations with lower confidence
 */
data class ConfidenceResult(
    val confidence: Float,
    val recognizedText: String,
    val alternatives: List<String> = emptyList()
) {
    /**
     * Calculated confidence level based on threshold values.
     *
     * Thresholds:
     * - HIGH: >= 0.8
     * - MEDIUM: 0.6 - 0.8
     * - LOW: 0.4 - 0.6
     * - REJECT: < 0.4
     */
    val level: ConfidenceLevel get() = when {
        confidence >= HIGH_THRESHOLD -> ConfidenceLevel.HIGH
        confidence >= MEDIUM_THRESHOLD -> ConfidenceLevel.MEDIUM
        confidence >= LOW_THRESHOLD -> ConfidenceLevel.LOW
        else -> ConfidenceLevel.REJECT
    }

    /**
     * Semantic label for user-friendly display.
     *
     * Maps [level] to a meaningful label:
     * - HIGH → [SemanticConfidenceLabel.HIGH_CONFIDENCE] ("High")
     * - MEDIUM → [SemanticConfidenceLabel.NEEDS_CONFIRMATION] ("Confirm?")
     * - LOW, REJECT → [SemanticConfidenceLabel.LOW_CONFIDENCE] ("Repeat")
     */
    val semanticLabel: SemanticConfidenceLabel get() = ConfidenceOverlay.getSemanticLabel(level)

    companion object {
        /** Threshold for HIGH confidence level */
        const val HIGH_THRESHOLD = 0.8f
        /** Threshold for MEDIUM confidence level */
        const val MEDIUM_THRESHOLD = 0.6f
        /** Threshold for LOW confidence level */
        const val LOW_THRESHOLD = 0.4f
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Confidence Overlay
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Overlay for displaying speech recognition confidence.
 *
 * Shows a visual indicator with:
 * - Color-coded confidence level (green/yellow/orange/red)
 * - Percentage display
 * - Recognized text preview
 *
 * Platform implementations render this as:
 * - Android: Compose overlay with animated color transitions
 * - iOS: SwiftUI overlay with matching animations
 * - Desktop: Platform-native floating window
 *
 * Usage:
 * ```kotlin
 * val overlay = ConfidenceOverlay()
 *
 * // Show with initial result
 * overlay.show(ConfidenceResult(0.85f, "click button"))
 *
 * // Update as recognition improves
 * overlay.updateConfidence(ConfidenceResult(0.92f, "click submit button"))
 *
 * // Hide when done
 * overlay.hide()
 *
 * // Cleanup
 * overlay.dispose()
 * ```
 *
 * @see ConfidenceLevel for confidence thresholds
 * @see ConfidenceResult for confidence data structure
 */
class ConfidenceOverlay : IOverlay {

    // ═══════════════════════════════════════════════════════════════════════
    // IOverlay Identity
    // ═══════════════════════════════════════════════════════════════════════

    override val id: String = OVERLAY_ID

    // ═══════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════

    private val _isVisible = MutableStateFlow(false)

    /**
     * Current visibility state of the overlay.
     */
    override val isVisible: Boolean get() = _isVisible.value

    private val _confidenceResult = MutableStateFlow<ConfidenceResult?>(null)

    /**
     * Current confidence result being displayed.
     *
     * Emits updates when:
     * - [show] is called with a result
     * - [updateConfidence] is called
     * - [dispose] is called (emits null)
     *
     * Platform implementations observe this flow for UI updates.
     */
    val confidenceResult: StateFlow<ConfidenceResult?> = _confidenceResult.asStateFlow()

    private var _isDisposed = false

    // ═══════════════════════════════════════════════════════════════════════
    // Show/Hide
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show overlay with a confidence result.
     *
     * Sets the confidence data and makes the overlay visible.
     *
     * @param result The confidence result to display
     */
    fun show(result: ConfidenceResult) {
        if (_isDisposed) return
        _confidenceResult.value = result
        _isVisible.value = true
    }

    /**
     * Show overlay without setting new data.
     *
     * If confidence data was previously set, it will be displayed.
     * Otherwise shows empty overlay.
     */
    override fun show() {
        if (_isDisposed) return
        _isVisible.value = true
    }

    /**
     * Hide the overlay.
     *
     * The confidence data is preserved and will be shown again
     * if [show] is called.
     */
    override fun hide() {
        _isVisible.value = false
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Update
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update the displayed confidence without changing visibility.
     *
     * Use this for real-time confidence updates during speech recognition.
     * Does not change visibility state.
     *
     * @param result The new confidence result
     */
    fun updateConfidence(result: ConfidenceResult) {
        if (_isDisposed) return
        _confidenceResult.value = result
    }

    /**
     * Update overlay with new data.
     *
     * Handles [OverlayData.Confidence] by showing the overlay with
     * the new confidence value. Other data types are ignored.
     *
     * @param data The overlay data to display
     */
    override fun update(data: OverlayData) {
        if (_isDisposed) return
        when (data) {
            is OverlayData.Confidence -> {
                _confidenceResult.value = ConfidenceResult(data.value, data.text)
                _isVisible.value = true
            }
            else -> {
                // Ignore non-confidence data
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dispose of the overlay and release resources.
     *
     * After disposal:
     * - [isVisible] returns false
     * - [confidenceResult] emits null
     * - [show] and [update] have no effect
     */
    override fun dispose() {
        _isDisposed = true
        hide()
        _confidenceResult.value = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Companion Object
    // ═══════════════════════════════════════════════════════════════════════

    companion object {
        /** Unique identifier for confidence overlays */
        const val OVERLAY_ID = "confidence"

        // ═══════════════════════════════════════════════════════════════════
        // Colors (ARGB Long format)
        // ═══════════════════════════════════════════════════════════════════

        /** Green - Material 500 - High confidence */
        const val COLOR_HIGH: Long = 0xFF4CAF50

        /** Yellow - Material 500 - Medium confidence */
        const val COLOR_MEDIUM: Long = 0xFFFFEB3B

        /** Orange - Material 500 - Low confidence */
        const val COLOR_LOW: Long = 0xFFFF9800

        /** Red - Material 500 - Rejected */
        const val COLOR_REJECT: Long = 0xFFF44336

        /**
         * Get the display color for a confidence level.
         *
         * Returns Material Design colors:
         * - [HIGH]: Green (#4CAF50)
         * - [MEDIUM]: Yellow (#FFEB3B)
         * - [LOW]: Orange (#FF9800)
         * - [REJECT]: Red (#F44336)
         *
         * @param level The confidence level
         * @return ARGB color as Long
         */
        fun getColorForLevel(level: ConfidenceLevel): Long = when (level) {
            ConfidenceLevel.HIGH -> COLOR_HIGH
            ConfidenceLevel.MEDIUM -> COLOR_MEDIUM
            ConfidenceLevel.LOW -> COLOR_LOW
            ConfidenceLevel.REJECT -> COLOR_REJECT
        }

        // ═══════════════════════════════════════════════════════════════════
        // Semantic Labels
        // ═══════════════════════════════════════════════════════════════════

        /**
         * Get the semantic confidence label for a confidence level.
         *
         * Maps confidence levels to user-friendly semantic labels:
         * - [ConfidenceLevel.HIGH] → [SemanticConfidenceLabel.HIGH_CONFIDENCE]
         * - [ConfidenceLevel.MEDIUM] → [SemanticConfidenceLabel.NEEDS_CONFIRMATION]
         * - [ConfidenceLevel.LOW] → [SemanticConfidenceLabel.LOW_CONFIDENCE]
         * - [ConfidenceLevel.REJECT] → [SemanticConfidenceLabel.LOW_CONFIDENCE]
         *
         * @param level The confidence level
         * @return The semantic label enum value
         */
        fun getSemanticLabel(level: ConfidenceLevel): SemanticConfidenceLabel = when (level) {
            ConfidenceLevel.HIGH -> SemanticConfidenceLabel.HIGH_CONFIDENCE
            ConfidenceLevel.MEDIUM -> SemanticConfidenceLabel.NEEDS_CONFIRMATION
            ConfidenceLevel.LOW -> SemanticConfidenceLabel.LOW_CONFIDENCE
            ConfidenceLevel.REJECT -> SemanticConfidenceLabel.LOW_CONFIDENCE
        }

        /**
         * Get the semantic label text for a confidence level.
         *
         * Returns user-friendly label strings:
         * - [ConfidenceLevel.HIGH] → "High"
         * - [ConfidenceLevel.MEDIUM] → "Confirm?"
         * - [ConfidenceLevel.LOW] → "Repeat"
         * - [ConfidenceLevel.REJECT] → "Repeat"
         *
         * @param level The confidence level
         * @return The user-facing label text
         */
        fun getSemanticLabelText(level: ConfidenceLevel): String =
            getSemanticLabel(level).label

        // ═══════════════════════════════════════════════════════════════════
        // Percentage Formatting
        // ═══════════════════════════════════════════════════════════════════

        /**
         * Format confidence value as percentage string.
         *
         * @param confidence Value from 0.0 to 1.0
         * @return Formatted string like "85%"
         */
        fun formatPercentage(confidence: Float): String {
            return "${(confidence * 100).toInt()}%"
        }

        /**
         * Format confidence value as percentage string with decimal.
         *
         * @param confidence Value from 0.0 to 1.0
         * @return Formatted string like "85.5%"
         */
        fun formatPercentageDecimal(confidence: Float): String {
            return "${(confidence * 1000).toInt() / 10.0}%"
        }
    }
}
