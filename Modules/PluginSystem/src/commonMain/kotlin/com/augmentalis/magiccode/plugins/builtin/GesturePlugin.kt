/**
 * GesturePlugin.kt - Gesture Handler as Universal Plugin
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Migrated plugin from VoiceOSCore's GestureHandler to the Universal Plugin Architecture.
 * Handles gesture commands including tap, swipe, pinch, and rotate actions.
 *
 * Migration from: Modules/VoiceOSCore/src/commonMain/.../GestureHandler.kt
 */
package com.augmentalis.magiccode.plugins.builtin

import com.augmentalis.magiccode.plugins.sdk.BasePlugin
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.commandmanager.ActionResult
import com.augmentalis.commandmanager.QuantizedCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// =============================================================================
// Gesture Types and Configuration
// =============================================================================

/**
 * Types of gestures supported by the gesture plugin.
 *
 * Covers the full range of touch-based and accessibility gesture interactions
 * used in voice/gaze-controlled applications.
 *
 * @since 1.0.0
 */
enum class GestureType {
    /** Single tap/click at a point */
    TAP,

    /** Double tap/click at a point */
    DOUBLE_TAP,

    /** Long press/hold at a point */
    LONG_PRESS,

    /** Swipe gesture moving upward */
    SWIPE_UP,

    /** Swipe gesture moving downward */
    SWIPE_DOWN,

    /** Swipe gesture moving left */
    SWIPE_LEFT,

    /** Swipe gesture moving right */
    SWIPE_RIGHT,

    /** Pinch in gesture (zoom out) */
    PINCH_IN,

    /** Pinch out gesture (zoom in) */
    PINCH_OUT,

    /** Rotate gesture counter-clockwise */
    ROTATE_LEFT,

    /** Rotate gesture clockwise */
    ROTATE_RIGHT
}

/**
 * Configuration for a gesture to be executed.
 *
 * Contains all parameters needed to perform a gesture on the target platform.
 * Default values are suitable for common mobile screen dimensions (1080x1920).
 *
 * ## Usage
 * ```kotlin
 * // Simple tap at center
 * val tapConfig = GestureConfig(type = GestureType.TAP)
 *
 * // Tap at specific coordinates
 * val tapAt = GestureConfig(type = GestureType.TAP, x = 100, y = 200)
 *
 * // Long press with custom duration
 * val longPress = GestureConfig(
 *     type = GestureType.LONG_PRESS,
 *     x = 540,
 *     y = 960,
 *     duration = 800L
 * )
 *
 * // Swipe with custom distance
 * val swipe = GestureConfig(
 *     type = GestureType.SWIPE_UP,
 *     distance = 400
 * )
 * ```
 *
 * @property type The type of gesture to perform
 * @property x X coordinate for the gesture (default: screen center for 1080 width)
 * @property y Y coordinate for the gesture (default: screen center for 1920 height)
 * @property duration Duration of the gesture in milliseconds
 * @property distance Distance for swipe gestures in pixels
 * @property scale Scale factor for pinch gestures (1.0 = no change, < 1.0 = pinch in, > 1.0 = pinch out)
 * @property angle Rotation angle in degrees for rotate gestures
 * @since 1.0.0
 */
data class GestureConfig(
    val type: GestureType,
    val x: Int = DEFAULT_CENTER_X,
    val y: Int = DEFAULT_CENTER_Y,
    val duration: Long = DEFAULT_TAP_DURATION,
    val distance: Int = DEFAULT_SWIPE_DISTANCE,
    val scale: Float = 1.0f,
    val angle: Float = 0f
) {
    companion object {
        /** Default X coordinate (center of 1080px width) */
        const val DEFAULT_CENTER_X = 540

        /** Default Y coordinate (center of 1920px height) */
        const val DEFAULT_CENTER_Y = 960

        /** Default tap duration in milliseconds */
        const val DEFAULT_TAP_DURATION = 100L

        /** Default long press duration in milliseconds */
        const val DEFAULT_LONG_PRESS_DURATION = 500L

        /** Default swipe distance in pixels */
        const val DEFAULT_SWIPE_DISTANCE = 200

        /** Default pinch scale factor */
        const val DEFAULT_PINCH_SCALE = 0.5f

        /** Default rotation angle in degrees */
        const val DEFAULT_ROTATION_ANGLE = 45f

        /**
         * Create a tap gesture configuration.
         *
         * @param x X coordinate (default: center)
         * @param y Y coordinate (default: center)
         * @return GestureConfig for tap
         */
        fun tap(x: Int = DEFAULT_CENTER_X, y: Int = DEFAULT_CENTER_Y): GestureConfig {
            return GestureConfig(type = GestureType.TAP, x = x, y = y)
        }

        /**
         * Create a double tap gesture configuration.
         *
         * @param x X coordinate (default: center)
         * @param y Y coordinate (default: center)
         * @return GestureConfig for double tap
         */
        fun doubleTap(x: Int = DEFAULT_CENTER_X, y: Int = DEFAULT_CENTER_Y): GestureConfig {
            return GestureConfig(type = GestureType.DOUBLE_TAP, x = x, y = y)
        }

        /**
         * Create a long press gesture configuration.
         *
         * @param x X coordinate (default: center)
         * @param y Y coordinate (default: center)
         * @param duration Duration in milliseconds (default: 500ms)
         * @return GestureConfig for long press
         */
        fun longPress(
            x: Int = DEFAULT_CENTER_X,
            y: Int = DEFAULT_CENTER_Y,
            duration: Long = DEFAULT_LONG_PRESS_DURATION
        ): GestureConfig {
            return GestureConfig(type = GestureType.LONG_PRESS, x = x, y = y, duration = duration)
        }

        /**
         * Create a swipe gesture configuration.
         *
         * @param type Swipe direction type (SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT)
         * @param distance Swipe distance in pixels
         * @return GestureConfig for swipe
         */
        fun swipe(type: GestureType, distance: Int = DEFAULT_SWIPE_DISTANCE): GestureConfig {
            require(type in listOf(
                GestureType.SWIPE_UP, GestureType.SWIPE_DOWN,
                GestureType.SWIPE_LEFT, GestureType.SWIPE_RIGHT
            )) { "Invalid swipe type: $type" }
            return GestureConfig(type = type, distance = distance)
        }
    }

    /**
     * Check if this is a swipe gesture.
     */
    fun isSwipe(): Boolean = type in listOf(
        GestureType.SWIPE_UP, GestureType.SWIPE_DOWN,
        GestureType.SWIPE_LEFT, GestureType.SWIPE_RIGHT
    )

    /**
     * Check if this is a pinch gesture.
     */
    fun isPinch(): Boolean = type in listOf(GestureType.PINCH_IN, GestureType.PINCH_OUT)

    /**
     * Check if this is a rotation gesture.
     */
    fun isRotation(): Boolean = type in listOf(GestureType.ROTATE_LEFT, GestureType.ROTATE_RIGHT)
}

/**
 * Result of parsing or creating a gesture command.
 *
 * @property success Whether the gesture was successfully created
 * @property gesture The gesture configuration if successful
 * @property error Error message if unsuccessful
 * @since 1.0.0
 */
data class GestureResult(
    val success: Boolean,
    val gesture: GestureConfig? = null,
    val error: String? = null
) {
    companion object {
        /**
         * Create a successful result.
         *
         * @param gesture The gesture configuration
         * @return Success result
         */
        fun success(gesture: GestureConfig): GestureResult {
            return GestureResult(success = true, gesture = gesture)
        }

        /**
         * Create a failure result.
         *
         * @param error Error message
         * @return Failure result
         */
        fun failure(error: String): GestureResult {
            return GestureResult(success = false, error = error)
        }
    }
}

// =============================================================================
// Gesture Plugin Implementation
// =============================================================================

/**
 * Gesture Plugin - Universal Plugin for gesture-based interactions.
 *
 * Handles all gesture-related voice/gaze commands including:
 * - Tap/click gestures (single, double, long press)
 * - Swipe gestures (up, down, left, right)
 * - Pinch gestures (zoom in/out)
 * - Rotation gestures (rotate left/right)
 * - Coordinate-based taps ("tap at X,Y")
 *
 * ## Migration Notes
 * This plugin wraps the original GestureHandler logic from VoiceOSCore,
 * adapting it to the Universal Plugin interface while maintaining identical
 * behavior and adding enhanced error handling.
 *
 * ## Usage
 * ```kotlin
 * val plugin = GesturePlugin { androidGestureExecutor }
 * plugin.initialize(config, context)
 *
 * val command = QuantizedCommand(phrase = "tap", ...)
 * if (plugin.canHandle(command, handlerContext)) {
 *     val result = plugin.handle(command, handlerContext)
 * }
 * ```
 *
 * ## Supported Commands
 * - "tap", "click" - Single tap at screen center
 * - "double tap", "double click" - Double tap at screen center
 * - "long press", "hold" - Long press at screen center
 * - "swipe up/down/left/right" - Directional swipe
 * - "pinch in", "zoom out" - Pinch in gesture
 * - "pinch out", "zoom in" - Pinch out gesture
 * - "rotate left/right" - Rotation gesture
 * - "tap at X,Y" - Tap at specific coordinates
 *
 * @param executorProvider Lazy provider for the platform-specific gesture executor
 * @since 1.0.0
 * @see HandlerPlugin
 * @see BasePlugin
 * @see GestureExecutor
 */
class GesturePlugin(
    private val executorProvider: () -> GestureExecutor
) : BasePlugin(), HandlerPlugin {

    // =========================================================================
    // Identity
    // =========================================================================

    override val pluginId: String = PLUGIN_ID
    override val pluginName: String = "Gesture Handler"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.ACCESSIBILITY_HANDLER,
            name = "Gesture Handler",
            version = "1.0.0",
            interfaces = setOf("HandlerPlugin", "GestureExecutor"),
            metadata = mapOf(
                "handlerType" to "UI_INTERACTION",
                "supportsTap" to "true",
                "supportsSwipe" to "true",
                "supportsPinch" to "true",
                "supportsRotate" to "true",
                "supportsCoordinates" to "true"
            )
        )
    )

    // =========================================================================
    // Handler Properties
    // =========================================================================

    override val handlerType: HandlerType = HandlerType.UI_INTERACTION

    override val patterns: List<CommandPattern> = listOf(
        // Tap patterns
        CommandPattern(
            regex = Regex("^(tap|click)$", RegexOption.IGNORE_CASE),
            intent = "TAP",
            requiredEntities = emptySet(),
            examples = listOf("tap", "click")
        ),
        CommandPattern(
            regex = Regex("^(double tap|double click)$", RegexOption.IGNORE_CASE),
            intent = "DOUBLE_TAP",
            requiredEntities = emptySet(),
            examples = listOf("double tap", "double click")
        ),
        CommandPattern(
            regex = Regex("^(long press|hold)$", RegexOption.IGNORE_CASE),
            intent = "LONG_PRESS",
            requiredEntities = emptySet(),
            examples = listOf("long press", "hold")
        ),

        // Swipe patterns
        CommandPattern(
            regex = Regex("^swipe\\s+(up|down|left|right)$", RegexOption.IGNORE_CASE),
            intent = "SWIPE",
            requiredEntities = setOf("direction"),
            examples = listOf("swipe up", "swipe down", "swipe left", "swipe right")
        ),

        // Pinch patterns
        CommandPattern(
            regex = Regex("^(pinch in|zoom out)$", RegexOption.IGNORE_CASE),
            intent = "PINCH_IN",
            requiredEntities = emptySet(),
            examples = listOf("pinch in", "zoom out")
        ),
        CommandPattern(
            regex = Regex("^(pinch out|zoom in)$", RegexOption.IGNORE_CASE),
            intent = "PINCH_OUT",
            requiredEntities = emptySet(),
            examples = listOf("pinch out", "zoom in")
        ),

        // Rotate patterns
        CommandPattern(
            regex = Regex("^rotate\\s+(left|right)$", RegexOption.IGNORE_CASE),
            intent = "ROTATE",
            requiredEntities = setOf("direction"),
            examples = listOf("rotate left", "rotate right")
        ),

        // Coordinate-based tap
        CommandPattern(
            regex = Regex("^tap at (\\d+)[,\\s]+(\\d+)$", RegexOption.IGNORE_CASE),
            intent = "TAP_AT",
            requiredEntities = setOf("x", "y"),
            examples = listOf("tap at 100,200", "tap at 540 960")
        )
    )

    // =========================================================================
    // Supported Actions (for discovery)
    // =========================================================================

    /**
     * List of supported action phrases.
     * Used for command discovery and help systems.
     */
    val supportedActions: List<String> = listOf(
        "tap", "click",
        "double tap", "double click",
        "long press", "hold",
        "swipe up", "swipe down", "swipe left", "swipe right",
        "pinch in", "pinch out", "zoom in", "zoom out",
        "rotate left", "rotate right"
    )

    // =========================================================================
    // Executor Reference
    // =========================================================================

    private lateinit var executor: GestureExecutor

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override suspend fun onInitialize(): InitResult {
        return try {
            executor = executorProvider()
            InitResult.success("GesturePlugin initialized with executor")
        } catch (e: Exception) {
            InitResult.failure(e, recoverable = true)
        }
    }

    override suspend fun onShutdown() {
        // No resources to release
    }

    override fun getHealthDiagnostics(): Map<String, String> = mapOf(
        "supportedActions" to supportedActions.size.toString(),
        "patterns" to patterns.size.toString(),
        "executorInitialized" to (::executor.isInitialized).toString(),
        "gestureTypes" to GestureType.entries.size.toString()
    )

    // =========================================================================
    // Handler Implementation
    // =========================================================================

    override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        val phrase = command.phrase.lowercase().trim()

        // Check exact matches first
        if (supportedActions.any { phrase == it.lowercase() }) {
            return true
        }

        // Check pattern matches
        if (patterns.any { it.matches(phrase) }) {
            return true
        }

        // Check for coordinate-based tap
        if (phrase.startsWith("tap at ")) {
            return true
        }

        return false
    }

    override suspend fun handle(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        val gestureResult = parseCommand(command.phrase)

        if (!gestureResult.success || gestureResult.gesture == null) {
            return ActionResult.Error(gestureResult.error ?: "Unknown gesture command")
        }

        return executeGesture(gestureResult.gesture)
    }

    override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
        val phrase = command.phrase.lowercase().trim()

        // Exact match with supported actions
        if (supportedActions.any { it.lowercase() == phrase }) {
            return 1.0f
        }

        // Pattern match
        for (pattern in patterns) {
            if (pattern.matches(phrase)) {
                return 0.95f
            }
        }

        // Coordinate tap pattern (partial match)
        if (phrase.startsWith("tap at ")) {
            val coordPattern = Regex("^tap at (\\d+)[,\\s]+(\\d+)$", RegexOption.IGNORE_CASE)
            return if (coordPattern.matches(phrase)) 0.95f else 0.7f
        }

        // Partial matches for gesture verbs
        val gestureVerbs = listOf("tap", "click", "swipe", "pinch", "zoom", "rotate", "hold", "press")
        if (gestureVerbs.any { phrase.startsWith(it) }) {
            return 0.6f
        }

        return 0.0f
    }

    // =========================================================================
    // Gesture Parsing
    // =========================================================================

    /**
     * Parse a voice command and convert it to a gesture configuration.
     *
     * @param command The voice command string
     * @return GestureResult containing the gesture config or error
     */
    fun parseCommand(command: String): GestureResult {
        val normalizedCommand = command.lowercase().trim()

        // Handle empty command
        if (normalizedCommand.isEmpty()) {
            return GestureResult.failure("Empty command")
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
                createGesture(GestureType.LONG_PRESS, duration = GestureConfig.DEFAULT_LONG_PRESS_DURATION)

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
            else -> GestureResult.failure("Unknown gesture: $command")
        }
    }

    /**
     * Create a gesture result with the specified parameters.
     *
     * @param type The type of gesture
     * @param x X coordinate for the gesture
     * @param y Y coordinate for the gesture
     * @param duration Duration of the gesture in milliseconds
     * @param distance Distance for swipe gestures in pixels
     * @return GestureResult containing the gesture config
     */
    private fun createGesture(
        type: GestureType,
        x: Int = GestureConfig.DEFAULT_CENTER_X,
        y: Int = GestureConfig.DEFAULT_CENTER_Y,
        duration: Long = GestureConfig.DEFAULT_TAP_DURATION,
        distance: Int = GestureConfig.DEFAULT_SWIPE_DISTANCE
    ): GestureResult {
        val config = GestureConfig(
            type = type,
            x = x,
            y = y,
            duration = duration,
            distance = distance
        )
        return GestureResult.success(config)
    }

    /**
     * Parse "tap at X,Y" or "tap at X Y" command to extract coordinates.
     *
     * @param command The command containing coordinates
     * @return GestureResult with TAP gesture at specified coordinates
     */
    private fun parseTapAt(command: String): GestureResult {
        val regex = Regex("""tap at (\d+)[,\s]+(\d+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(command)
            ?: return GestureResult.failure("Could not parse coordinates from: $command")

        val (xStr, yStr) = match.destructured
        val x = xStr.toIntOrNull()
            ?: return GestureResult.failure("Invalid X coordinate: $xStr")
        val y = yStr.toIntOrNull()
            ?: return GestureResult.failure("Invalid Y coordinate: $yStr")

        return createGesture(GestureType.TAP, x = x, y = y)
    }

    // =========================================================================
    // Gesture Execution
    // =========================================================================

    /**
     * Execute a gesture using the platform-specific executor.
     *
     * @param gesture The gesture configuration to execute
     * @return ActionResult indicating success or failure
     */
    private suspend fun executeGesture(gesture: GestureConfig): ActionResult {
        return try {
            val success = when (gesture.type) {
                GestureType.TAP -> executor.tap(gesture.x, gesture.y)
                GestureType.DOUBLE_TAP -> executor.doubleTap(gesture.x, gesture.y)
                GestureType.LONG_PRESS -> executor.longPress(gesture.x, gesture.y, gesture.duration)
                GestureType.SWIPE_UP -> executor.swipeUp(gesture.distance)
                GestureType.SWIPE_DOWN -> executor.swipeDown(gesture.distance)
                GestureType.SWIPE_LEFT -> executor.swipeLeft(gesture.distance)
                GestureType.SWIPE_RIGHT -> executor.swipeRight(gesture.distance)
                GestureType.PINCH_IN -> executor.pinchIn(gesture.x, gesture.y, gesture.scale)
                GestureType.PINCH_OUT -> executor.pinchOut(gesture.x, gesture.y, gesture.scale)
                GestureType.ROTATE_LEFT -> executor.rotateLeft(gesture.x, gesture.y, gesture.angle)
                GestureType.ROTATE_RIGHT -> executor.rotateRight(gesture.x, gesture.y, gesture.angle)
            }

            if (success) {
                ActionResult.Success("Executed ${gesture.type.name.lowercase().replace('_', ' ')}")
            } else {
                ActionResult.Error("Failed to execute ${gesture.type.name.lowercase().replace('_', ' ')}")
            }
        } catch (e: Exception) {
            ActionResult.Error("Gesture execution error: ${e.message}")
        }
    }

    // =========================================================================
    // Utility Functions
    // =========================================================================

    /**
     * Get all supported gesture types.
     *
     * @return List of all GestureType values
     */
    fun getGestureTypes(): List<GestureType> = GestureType.entries

    companion object {
        /** Plugin ID for registration and discovery */
        const val PLUGIN_ID = "com.augmentalis.commandmanager.handler.gesture"
    }
}

// =============================================================================
// Executor Interface
// =============================================================================

/**
 * Platform-specific executor interface for gesture actions.
 *
 * This interface defines the contract for executing gestures on target platforms.
 * Implementations are platform-specific:
 * - Android: Uses AccessibilityService gesture dispatch or MotionEvent injection
 * - iOS: Uses UIAccessibility APIs or UIGestureRecognizer simulation
 * - Desktop: Uses native accessibility APIs or simulated mouse/touch input
 *
 * ## Thread Safety
 * All methods are suspend functions and should be thread-safe.
 * Implementations should handle dispatching to appropriate threads.
 *
 * @since 1.0.0
 * @see GesturePlugin
 */
interface GestureExecutor {

    /**
     * Perform a single tap at the specified coordinates.
     *
     * @param x X coordinate in screen pixels
     * @param y Y coordinate in screen pixels
     * @return true if tap was performed successfully
     */
    suspend fun tap(x: Int, y: Int): Boolean

    /**
     * Perform a double tap at the specified coordinates.
     *
     * @param x X coordinate in screen pixels
     * @param y Y coordinate in screen pixels
     * @return true if double tap was performed successfully
     */
    suspend fun doubleTap(x: Int, y: Int): Boolean

    /**
     * Perform a long press at the specified coordinates.
     *
     * @param x X coordinate in screen pixels
     * @param y Y coordinate in screen pixels
     * @param duration Duration of the long press in milliseconds
     * @return true if long press was performed successfully
     */
    suspend fun longPress(x: Int, y: Int, duration: Long): Boolean

    /**
     * Perform a swipe up gesture from screen center.
     *
     * @param distance Distance to swipe in pixels
     * @return true if swipe was performed successfully
     */
    suspend fun swipeUp(distance: Int): Boolean

    /**
     * Perform a swipe down gesture from screen center.
     *
     * @param distance Distance to swipe in pixels
     * @return true if swipe was performed successfully
     */
    suspend fun swipeDown(distance: Int): Boolean

    /**
     * Perform a swipe left gesture from screen center.
     *
     * @param distance Distance to swipe in pixels
     * @return true if swipe was performed successfully
     */
    suspend fun swipeLeft(distance: Int): Boolean

    /**
     * Perform a swipe right gesture from screen center.
     *
     * @param distance Distance to swipe in pixels
     * @return true if swipe was performed successfully
     */
    suspend fun swipeRight(distance: Int): Boolean

    /**
     * Perform a pinch in (zoom out) gesture.
     *
     * @param centerX Center X coordinate of the pinch
     * @param centerY Center Y coordinate of the pinch
     * @param scale Scale factor (< 1.0 for pinch in)
     * @return true if pinch was performed successfully
     */
    suspend fun pinchIn(centerX: Int, centerY: Int, scale: Float): Boolean

    /**
     * Perform a pinch out (zoom in) gesture.
     *
     * @param centerX Center X coordinate of the pinch
     * @param centerY Center Y coordinate of the pinch
     * @param scale Scale factor (> 1.0 for pinch out)
     * @return true if pinch was performed successfully
     */
    suspend fun pinchOut(centerX: Int, centerY: Int, scale: Float): Boolean

    /**
     * Perform a rotate left (counter-clockwise) gesture.
     *
     * @param centerX Center X coordinate of the rotation
     * @param centerY Center Y coordinate of the rotation
     * @param angle Angle to rotate in degrees
     * @return true if rotation was performed successfully
     */
    suspend fun rotateLeft(centerX: Int, centerY: Int, angle: Float): Boolean

    /**
     * Perform a rotate right (clockwise) gesture.
     *
     * @param centerX Center X coordinate of the rotation
     * @param centerY Center Y coordinate of the rotation
     * @param angle Angle to rotate in degrees
     * @return true if rotation was performed successfully
     */
    suspend fun rotateRight(centerX: Int, centerY: Int, angle: Float): Boolean
}

// =============================================================================
// Factory Functions
// =============================================================================

/**
 * Create a GesturePlugin with a pre-configured executor.
 *
 * @param executor The gesture executor implementation
 * @return Configured GesturePlugin
 */
fun createGesturePlugin(executor: GestureExecutor): GesturePlugin {
    return GesturePlugin { executor }
}

/**
 * Create a GesturePlugin with a lazy executor provider.
 *
 * Useful when the executor depends on platform services that may
 * not be available at plugin creation time.
 *
 * @param executorProvider Function that returns the executor when needed
 * @return Configured GesturePlugin
 */
fun createGesturePlugin(executorProvider: () -> GestureExecutor): GesturePlugin {
    return GesturePlugin(executorProvider)
}

// =============================================================================
// Testing Support
// =============================================================================

/**
 * Mock executor for testing GesturePlugin.
 *
 * Records all gesture actions and can be configured to succeed or fail.
 * Useful for unit testing without requiring actual platform gesture services.
 *
 * ## Usage
 * ```kotlin
 * val mockExecutor = MockGestureExecutor(shouldSucceed = true)
 * val plugin = createGesturePlugin(mockExecutor)
 *
 * // Execute gestures
 * plugin.handle(command, context)
 *
 * // Verify actions
 * assertEquals(listOf("tap(540, 960)"), mockExecutor.actions)
 * ```
 *
 * @param shouldSucceed Whether gesture operations should succeed
 * @since 1.0.0
 */
class MockGestureExecutor(
    private val shouldSucceed: Boolean = true
) : GestureExecutor {

    private val _actions = mutableListOf<String>()

    /** List of recorded actions in format "methodName(params)" */
    val actions: List<String> get() = _actions.toList()

    /** Clear recorded actions */
    fun clearActions() = _actions.clear()

    /** Get the last recorded action */
    fun lastAction(): String? = _actions.lastOrNull()

    override suspend fun tap(x: Int, y: Int): Boolean {
        _actions.add("tap($x, $y)")
        return shouldSucceed
    }

    override suspend fun doubleTap(x: Int, y: Int): Boolean {
        _actions.add("doubleTap($x, $y)")
        return shouldSucceed
    }

    override suspend fun longPress(x: Int, y: Int, duration: Long): Boolean {
        _actions.add("longPress($x, $y, $duration)")
        return shouldSucceed
    }

    override suspend fun swipeUp(distance: Int): Boolean {
        _actions.add("swipeUp($distance)")
        return shouldSucceed
    }

    override suspend fun swipeDown(distance: Int): Boolean {
        _actions.add("swipeDown($distance)")
        return shouldSucceed
    }

    override suspend fun swipeLeft(distance: Int): Boolean {
        _actions.add("swipeLeft($distance)")
        return shouldSucceed
    }

    override suspend fun swipeRight(distance: Int): Boolean {
        _actions.add("swipeRight($distance)")
        return shouldSucceed
    }

    override suspend fun pinchIn(centerX: Int, centerY: Int, scale: Float): Boolean {
        _actions.add("pinchIn($centerX, $centerY, $scale)")
        return shouldSucceed
    }

    override suspend fun pinchOut(centerX: Int, centerY: Int, scale: Float): Boolean {
        _actions.add("pinchOut($centerX, $centerY, $scale)")
        return shouldSucceed
    }

    override suspend fun rotateLeft(centerX: Int, centerY: Int, angle: Float): Boolean {
        _actions.add("rotateLeft($centerX, $centerY, $angle)")
        return shouldSucceed
    }

    override suspend fun rotateRight(centerX: Int, centerY: Int, angle: Float): Boolean {
        _actions.add("rotateRight($centerX, $centerY, $angle)")
        return shouldSucceed
    }
}
