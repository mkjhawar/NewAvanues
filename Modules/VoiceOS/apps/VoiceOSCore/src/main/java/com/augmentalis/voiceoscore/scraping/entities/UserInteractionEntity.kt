/**
 * UserInteractionEntity.kt - User interaction tracking
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-18
 * Migrated to SQLDelight: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * User Interaction Entity
 *
 * MIGRATION NOTE: This entity has been migrated to use SQLDelight.
 * The schema is defined in: core/database/src/commonMain/sqldelight/com/augmentalis/database/UserInteraction.sq
 *
 * Records user interactions with UI elements including clicks, long presses,
 * swipes, and focus events. Tracks visibility duration to measure user
 * decision time (how long element was visible before user interacted).
 *
 * Use Cases:
 * - Confidence scoring for voice commands
 * - Element importance weighting
 * - User journey analysis
 * - Interaction history for multi-step navigation
 *
 * @property id Auto-generated primary key
 * @property elementHash Hash of the element that was interacted with
 * @property screenHash Hash of the screen where interaction occurred
 * @property interactionType Type of interaction (click, long_press, swipe, focus)
 * @property interactionTime When the interaction occurred
 * @property visibilityStart When the element first became visible (if tracked)
 * @property visibilityDuration How long element was visible before interaction (ms)
 */
data class UserInteractionEntity(
    val id: Long = 0,
    val elementHash: String,
    val screenHash: String,
    val interactionType: String,
    val interactionTime: Long = System.currentTimeMillis(),
    val visibilityStart: Long? = null,
    val visibilityDuration: Long? = null
)

/**
 * Interaction type constants
 *
 * Defines the types of user interactions that can be tracked.
 */
object InteractionType {
    /** Standard tap/click interaction */
    const val CLICK = "click"

    /** Long press (press and hold) */
    const val LONG_PRESS = "long_press"

    /** Swipe gesture */
    const val SWIPE = "swipe"

    /** Element received focus */
    const val FOCUS = "focus"

    /** Scroll event */
    const val SCROLL = "scroll"

    /** Double tap */
    const val DOUBLE_TAP = "double_tap"

    /** Voice command executed on element */
    const val VOICE_COMMAND = "voice_command"
}
