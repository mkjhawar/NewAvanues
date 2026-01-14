/**
 * VOS4 Cursor Plugin Interface
 *
 * Allows third-party plugins to provide custom cursor implementations,
 * control cursor movement, and respond to cursor events.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-10-26
 */
package com.augmentalis.avacode.plugins.vos4

import android.graphics.Point
import com.augmentalis.avacode.plugins.core.PluginManifest

/**
 * Plugin interface for cursor control features
 *
 * Implementations can:
 * - Provide custom cursor modes (IMU, gaze, touch, etc.)
 * - Control cursor position and movement
 * - Customize cursor appearance
 * - React to cursor events (click, move, edge detection)
 */
interface CursorPluginInterface {

    /**
     * Plugin manifest with metadata
     */
    val manifest: PluginManifest

    /**
     * Provide custom cursor mode
     *
     * @return Cursor mode definition
     */
    fun provideCursorMode(): CursorModeDefinition

    /**
     * Called when cursor mode is activated
     */
    fun onCursorModeActivated()

    /**
     * Called when cursor mode is deactivated
     */
    fun onCursorModeDeactivated()

    /**
     * Called on each cursor position update
     *
     * @param position Current cursor position
     * @param delta Movement delta from last position
     * @param velocity Current velocity (pixels per second)
     */
    fun onCursorMove(position: Point, delta: Point, velocity: Float)

    /**
     * Called when cursor reaches screen edge
     *
     * @param edge The edge that was reached
     * @param position Current cursor position
     */
    fun onEdgeDetected(edge: ScreenEdge, position: Point)

    /**
     * Called when cursor click is detected
     *
     * @param position Cursor position at click
     * @param clickType Type of click (single, double, long)
     * @return true if click was handled, false to pass to system
     */
    fun onCursorClick(position: Point, clickType: ClickType): Boolean

    /**
     * Provide cursor visual customization
     *
     * @return Cursor appearance definition
     */
    fun provideCursorAppearance(): CursorAppearance?
}

/**
 * Cursor mode definition
 */
data class CursorModeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val inputSource: CursorInputSource,
    val smoothingEnabled: Boolean = true,
    val edgeDetectionEnabled: Boolean = true,
    val customSettings: Map<String, Any>? = null
)

/**
 * Cursor input sources
 */
enum class CursorInputSource {
    IMU,          // Accelerometer/gyroscope
    GAZE,         // Eye tracking
    TOUCH,        // Touch gestures
    MOUSE,        // External mouse
    GAMEPAD,      // Gamepad/joystick
    VOICE,        // Voice commands
    CUSTOM        // Plugin-defined
}

/**
 * Screen edge types
 */
enum class ScreenEdge {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

/**
 * Click types
 */
enum class ClickType {
    SINGLE,
    DOUBLE,
    LONG_PRESS,
    RIGHT_CLICK  // For accessibility
}

/**
 * Cursor appearance customization
 */
data class CursorAppearance(
    val cursorType: CursorType,
    val size: Int = 48,  // pixels
    val color: Int = 0xFFFFFFFF.toInt(),  // ARGB
    val opacity: Float = 1.0f,  // 0.0 - 1.0
    val customDrawable: String? = null  // Asset path in plugin
)

/**
 * Cursor visual types
 */
enum class CursorType {
    CIRCLE,
    HAND,
    CROSSHAIR,
    ARROW,
    CUSTOM
}
