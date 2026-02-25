/**
 * GestureHandler.kt - Handles gesture commands for voice control
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 *
 * KMP handler for gesture actions (tap, swipe, pinch, rotate).
 * Part of Phase 12: Handler System.
 */
package com.augmentalis.voiceoscore

/**
 * Types of gestures supported by the system.
 */
enum class GestureType {
    TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    PINCH_IN,
    PINCH_OUT,
    ROTATE_LEFT,
    ROTATE_RIGHT
}

/**
 * Configuration for a gesture to be executed.
 *
 * @param type The type of gesture
 * @param x X coordinate for the gesture (default: screen center)
 * @param y Y coordinate for the gesture (default: screen center)
 * @param duration Duration of the gesture in milliseconds
 * @param distance Distance for swipe gestures in pixels
 */
data class GestureConfig(
    val type: GestureType,
    val x: Int = 540,  // Default center (1080/2)
    val y: Int = 960,  // Default center (1920/2)
    val duration: Long = 100L,
    val distance: Int = 200  // For swipes
)

/**
 * Result of a gesture command parsing/creation.
 *
 * @param success Whether the gesture was successfully created
 * @param gesture The gesture configuration if successful
 * @param error Error message if unsuccessful
 */
data class GestureResult(
    val success: Boolean,
    val gesture: GestureConfig? = null,
    val error: String? = null
)

/**
 * Handler for gesture commands.
 *
 * Parses voice commands and converts them to gesture configurations
 * that can be executed by platform-specific executors.
 *
 * Supports:
 * - Tap/click gestures
 * - Double tap/double click
 * - Long press/hold
 * - Swipe up/down/left/right
 * - Pinch in/out (zoom out/in)
 * - Rotate left/right
 * - Tap at specific coordinates
 */
class GestureHandler {

    /**
     * Handle a voice command and convert it to a gesture.
     *
     * @param command The voice command string
     * @return GestureResult containing the gesture config or error
     */
    fun handleCommand(command: String): GestureResult {
        val normalizedCommand = command.lowercase().trim()

        // Handle empty command
        if (normalizedCommand.isEmpty()) {
            return GestureResult(false, error = "Empty command")
        }

        return when {
            // Tap gestures
            normalizedCommand == "tap" || normalizedCommand == "click" ->
                createGesture(GestureType.TAP)

            // Double tap gestures
            normalizedCommand == "double tap" || normalizedCommand == "double click" ->
                createGesture(GestureType.DOUBLE_TAP)

            // Long press gestures
            normalizedCommand == "long press" || normalizedCommand == "hold" ->
                createGesture(GestureType.LONG_PRESS, duration = 500L)

            // Swipe gestures
            normalizedCommand == "swipe up" ->
                createGesture(GestureType.SWIPE_UP)

            normalizedCommand == "swipe down" ->
                createGesture(GestureType.SWIPE_DOWN)

            normalizedCommand == "swipe left" ->
                createGesture(GestureType.SWIPE_LEFT)

            normalizedCommand == "swipe right" ->
                createGesture(GestureType.SWIPE_RIGHT)

            // Pinch gestures
            normalizedCommand == "pinch in" || normalizedCommand == "zoom out" ->
                createGesture(GestureType.PINCH_IN)

            normalizedCommand == "pinch out" || normalizedCommand == "zoom in" ->
                createGesture(GestureType.PINCH_OUT)

            // Rotate gestures
            normalizedCommand == "rotate left" ->
                createGesture(GestureType.ROTATE_LEFT)

            normalizedCommand == "rotate right" ->
                createGesture(GestureType.ROTATE_RIGHT)

            // Tap at coordinates
            normalizedCommand.startsWith("tap at ") ->
                parseTapAt(command)

            // Unknown command
            else -> GestureResult(false, error = "Unknown gesture: $command")
        }
    }

    /**
     * Create a gesture configuration with the specified parameters.
     *
     * @param type The type of gesture
     * @param x X coordinate for the gesture
     * @param y Y coordinate for the gesture
     * @param duration Duration of the gesture in milliseconds
     * @param distance Distance for swipe gestures in pixels
     * @return GestureResult containing the gesture config
     */
    fun createGesture(
        type: GestureType,
        x: Int = 540,
        y: Int = 960,
        duration: Long = 100L,
        distance: Int = 200
    ): GestureResult {
        val config = GestureConfig(
            type = type,
            x = x,
            y = y,
            duration = duration,
            distance = distance
        )
        return GestureResult(success = true, gesture = config)
    }

    /**
     * Parse "tap at X,Y" or "tap at X Y" command to extract coordinates.
     *
     * @param command The command containing coordinates
     * @return GestureResult with TAP gesture at specified coordinates
     */
    private fun parseTapAt(command: String): GestureResult {
        // Parse "tap at X,Y" or "tap at X Y"
        val regex = Regex("""tap at (\d+)[,\s]+(\d+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(command)
            ?: return GestureResult(false, error = "Could not parse coordinates from: $command")

        val (xStr, yStr) = match.destructured
        val x = xStr.toIntOrNull()
            ?: return GestureResult(false, error = "Invalid X coordinate: $xStr")
        val y = yStr.toIntOrNull()
            ?: return GestureResult(false, error = "Invalid Y coordinate: $yStr")

        return createGesture(GestureType.TAP, x = x, y = y)
    }

    /**
     * Get all supported gesture types.
     *
     * @return List of all GestureType values
     */
    fun getGestureTypes(): List<GestureType> = GestureType.entries
}
