/**
 * UserInteractionEntity.kt - User interaction tracking
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
 * User Interaction Entity
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
 * @property success Whether the interaction was successful (default true)
 * @property createdAt Record creation timestamp
 */
@Entity(
    tableName = "user_interactions",
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
        Index("interaction_type"),
        Index("interaction_time")
    ]
)
data class UserInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "element_hash")
    val elementHash: String,

    @ColumnInfo(name = "screen_hash")
    val screenHash: String,

    @ColumnInfo(name = "interaction_type")
    val interactionType: String,  // See InteractionType constants

    @ColumnInfo(name = "interaction_time")
    val interactionTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "visibility_start")
    val visibilityStart: Long? = null,  // When element became visible

    @ColumnInfo(name = "visibility_duration")
    val visibilityDuration: Long? = null,  // Milliseconds visible before interaction

    @ColumnInfo(name = "success")
    val success: Boolean = true,  // Did the interaction succeed?

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
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
