/**
 * ClickFailureReason.kt - Click failure telemetry data
 *
 * Records information about failed click attempts for debugging and analytics.
 * KMP-compatible data class.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.commandmanager

/**
 * Click failure reason tracking for telemetry.
 *
 * Records details about why a click operation failed, useful for:
 * - Debugging exploration issues
 * - Identifying problematic UI patterns
 * - Improving click strategies
 *
 * @property elementDesc Description of the element (text/contentDesc)
 * @property elementType Type of the element (class name)
 * @property reason Failure reason code
 * @property timestamp When the failure occurred (epoch millis)
 */
data class ClickFailureReason(
    val elementDesc: String,
    val elementType: String,
    val reason: ClickFailure,
    val timestamp: Long
) {
    override fun toString(): String {
        return "ClickFailure[$reason]: '$elementDesc' ($elementType)"
    }
}

/**
 * Enumeration of possible click failure reasons
 */
enum class ClickFailure {
    /** Element not visible to user */
    NOT_VISIBLE,

    /** Element is disabled */
    NOT_ENABLED,

    /** Element is off-screen and scroll failed */
    SCROLL_FAILED,

    /** AccessibilityNodeInfo.performAction() failed */
    ACTION_FAILED,

    /** Gesture dispatch failed */
    GESTURE_FAILED,

    /** Node became stale (recycled) */
    NODE_STALE,

    /** Element not found during refresh */
    NOT_FOUND,

    /** Exception during click */
    EXCEPTION,

    /** Unknown failure */
    UNKNOWN
}
