/**
 * ElementStateHistoryEntity.kt - Element state change tracking
 *
 * Migrated from Room to SQLDelight (Phase 2)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Scraping Migration Specialist (Agent 3B)
 * Created: 2025-11-27
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Element State History Entity
 *
 * Tracks changes to element states over time including checked status,
 * selection, enabled/disabled, visibility, and focus. Records what
 * triggered the change (user action vs system event) for context.
 *
 * Use Cases:
 * - State-aware voice commands ("check the box" vs "uncheck the box")
 * - Understanding element behavior patterns
 * - Debugging UI state issues
 * - Identifying user vs system-triggered changes
 *
 * @property id Auto-generated primary key
 * @property elementHash Hash of the element whose state changed
 * @property screenHash Hash of the screen where change occurred
 * @property stateType Type of state that changed (checked, selected, enabled, etc.)
 * @property oldValue Previous state value
 * @property newValue New state value
 * @property changedAt When the state change occurred
 * @property triggeredBy What triggered the change (user_click, system, app_event, etc.)
 */
data class ElementStateHistoryEntity(
    val id: Long = 0,
    val elementHash: String,
    val screenHash: String,
    val stateType: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val changedAt: Long = System.currentTimeMillis(),
    val triggeredBy: String? = null
)

/**
 * State type constants
 *
 * Defines the types of element states that can be tracked.
 */
object StateType {
    /** Checkbox/radio button checked state */
    const val CHECKED = "checked"

    /** Element selected state */
    const val SELECTED = "selected"

    /** Element enabled/disabled state */
    const val ENABLED = "enabled"

    /** Element visibility state */
    const val VISIBLE = "visible"

    /** Element focus state */
    const val FOCUSED = "focused"

    /** Expandable element expanded/collapsed state */
    const val EXPANDED = "expanded"

    /** Text input value */
    const val TEXT_VALUE = "text_value"

    /** Progress bar value */
    const val PROGRESS = "progress"
}

/**
 * Trigger source constants
 *
 * Defines what triggered a state change.
 */
object TriggerSource {
    /** User clicked/tapped the element */
    const val USER_CLICK = "user_click"

    /** User issued a voice command */
    const val USER_VOICE = "user_voice"

    /** System-triggered change */
    const val SYSTEM = "system"

    /** App event (e.g., data load completion) */
    const val APP_EVENT = "app_event"

    /** Unknown trigger source */
    const val UNKNOWN = "unknown"

    /** User keyboard input */
    const val USER_KEYBOARD = "user_keyboard"

    /** User gesture (swipe, pinch, etc.) */
    const val USER_GESTURE = "user_gesture"
}
