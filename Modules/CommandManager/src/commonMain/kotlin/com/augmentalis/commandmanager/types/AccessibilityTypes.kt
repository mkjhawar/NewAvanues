/**
 * AccessibilityTypes.kt - Core accessibility type definitions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-11-17
 *
 * Pure Kotlin accessibility types for cross-platform use.
 * No platform-specific dependencies.
 *
 * Extracted from VoiceOSCore for Kotlin Multiplatform support.
 */
package com.augmentalis.commandmanager

/**
 * Anchor point for badge/overlay positioning
 *
 * Defines where UI overlays should be anchored relative to target elements.
 * Used by number overlays, voice cursors, and accessibility indicators.
 */
enum class AnchorPoint {
    /**
     * Top-left corner of the element
     */
    TOP_LEFT,

    /**
     * Top-right corner of the element
     * Default - most common in UI patterns (like notification badges)
     */
    TOP_RIGHT,

    /**
     * Bottom-left corner of the element
     */
    BOTTOM_LEFT,

    /**
     * Bottom-right corner of the element
     */
    BOTTOM_RIGHT
}

/**
 * Badge visual style variants
 *
 * Defines different visual styles for accessibility badges and overlays.
 * Supports various accessibility needs (contrast, size, shape differentiation).
 */
enum class BadgeStyle {
    /**
     * Solid filled circle (default style)
     * Best for general use and readability
     */
    FILLED_CIRCLE,

    /**
     * Outlined circle with hollow center
     * Better for high contrast or minimalist themes
     */
    OUTLINED_CIRCLE,

    /**
     * Square badge
     * Alternative shape for differentiation
     */
    SQUARE,

    /**
     * Rounded rectangle badge
     * Compromise between circle and square
     */
    ROUNDED_RECT
}

/**
 * Element voice state for accessibility feedback
 *
 * Indicates whether UI elements have voice commands and how they're configured.
 * Used for color-coding overlays and providing visual feedback.
 */
enum class ElementVoiceState {
    /**
     * Green - Element has custom command name assigned
     * Highest voice accessibility (explicit command)
     */
    ENABLED_WITH_NAME,

    /**
     * Orange - Element is voice-enabled but uses default/auto-generated name
     * Medium voice accessibility (generic command)
     */
    ENABLED_NO_NAME,

    /**
     * Grey - Element is not voice-enabled
     * No voice accessibility (requires other interaction methods)
     */
    DISABLED
}

/**
 * Connection states for service monitoring
 *
 * Tracks the health and availability of VoiceOS accessibility services.
 * Used for service lifecycle management and user feedback.
 */
enum class ConnectionState {
    /**
     * Service is connected and healthy
     * All features available, normal operation
     */
    CONNECTED,

    /**
     * Service is disconnected
     * No features available, requires reconnection
     */
    DISCONNECTED,

    /**
     * Service is attempting recovery from disconnection
     * Limited features, attempting automatic reconnection
     */
    RECOVERING,

    /**
     * Service is operating in degraded mode
     * Fallback functionality only, reduced feature set
     * (e.g., basic commands work, advanced features disabled)
     */
    DEGRADED
}

/**
 * Screen edge positions
 *
 * Identifies which edge(s) of the screen a UI element is near.
 * Used for cursor boundary detection, edge gestures, and accessibility navigation.
 */
enum class ScreenEdge {
    /**
     * Near left edge of screen
     */
    LEFT,

    /**
     * Near right edge of screen
     */
    RIGHT,

    /**
     * Near top edge of screen
     */
    TOP,

    /**
     * Near bottom edge of screen
     */
    BOTTOM,

    /**
     * Near top-left corner (both top AND left edges)
     */
    TOP_LEFT,

    /**
     * Near top-right corner (both top AND right edges)
     */
    TOP_RIGHT,

    /**
     * Near bottom-left corner (both bottom AND left edges)
     */
    BOTTOM_LEFT,

    /**
     * Near bottom-right corner (both bottom AND right edges)
     */
    BOTTOM_RIGHT,

    /**
     * Not near any edge
     * Element is in the center area of the screen
     */
    NONE
}
