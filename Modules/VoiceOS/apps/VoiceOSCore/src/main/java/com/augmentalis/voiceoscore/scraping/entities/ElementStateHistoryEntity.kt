/**
 * ElementStateHistoryEntity.kt - Element state change tracking
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
@Entity(
    tableName = "element_state_history",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["element_hash"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenContextEntity::class,
            parentColumns = ["screen_hash"],
            childColumns = ["screen_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("element_hash"),
        Index("screen_hash"),
        Index("state_type"),
        Index("changed_at")
    ]
)
data class ElementStateHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "element_hash")
    val elementHash: String,

    @ColumnInfo(name = "screen_hash")
    val screenHash: String,

    @ColumnInfo(name = "state_type")
    val stateType: String,  // See StateType constants

    @ColumnInfo(name = "old_value")
    val oldValue: String? = null,  // Previous state value (may be null for initial state)

    @ColumnInfo(name = "new_value")
    val newValue: String? = null,  // New state value

    @ColumnInfo(name = "changed_at")
    val changedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "triggered_by")
    val triggeredBy: String? = null  // See TriggerSource constants
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
