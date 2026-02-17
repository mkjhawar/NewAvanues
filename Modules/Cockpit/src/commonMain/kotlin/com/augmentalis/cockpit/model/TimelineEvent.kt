package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * An activity event in the session timeline.
 *
 * Tracks key moments for history review and session audit:
 * - Frame created/deleted
 * - Layout mode changed
 * - Content interactions (URL navigated, page turned, etc.)
 * - Manual screenshots/annotations
 *
 * Events are ordered by [timestamp] (ISO 8601) and can optionally
 * include a screenshot URI and arbitrary JSON metadata.
 */
@Serializable
data class TimelineEvent(
    /** Unique event identifier */
    val id: String,
    /** Session this event belongs to */
    val sessionId: String,
    /** Event category (e.g., "frame_created", "layout_changed", "screenshot") */
    val eventType: String,
    /** Human-readable description of what happened */
    val description: String = "",
    /** Optional screenshot URI captured at this moment */
    val screenshotUri: String? = null,
    /** Arbitrary JSON metadata (frame IDs involved, old/new values, etc.) */
    val metadata: String = "{}",
    /** ISO 8601 timestamp */
    val timestamp: String,
)
