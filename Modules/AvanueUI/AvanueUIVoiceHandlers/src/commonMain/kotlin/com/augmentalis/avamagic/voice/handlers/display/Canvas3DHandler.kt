/**
 * Canvas3DHandler.kt - Voice handler for 3D Canvas/Viewer interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven 3D canvas control with rotation, zoom, pan, and selection
 * Features:
 * - Rotate horizontally and vertically with voice commands
 * - Toggle auto-rotation mode
 * - Zoom in/out with relative or absolute percentages
 * - Reset view to default or switch to preset camera angles
 * - Pan the view in any direction
 * - Select and deselect objects in the 3D scene
 * - Switch between render modes (wireframe, solid, textured)
 * - AVID-based targeting for precise canvas selection
 * - Voice feedback for all operations
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Rotation:
 * - "rotate left" / "rotate right" - Rotate horizontally
 * - "rotate up" / "rotate down" - Rotate vertically
 * - "spin" / "auto rotate" - Toggle auto-rotation
 *
 * Zoom:
 * - "zoom in" / "zoom out" - Adjust zoom level incrementally
 * - "zoom [N] percent" - Set specific zoom level
 *
 * View Control:
 * - "reset view" / "home" - Reset to default view
 * - "front view" / "back view" - Switch to front/back preset
 * - "left view" / "right view" - Switch to side presets
 * - "top view" / "bottom view" - Switch to top/bottom presets
 * - "isometric view" / "iso view" - Switch to isometric preset
 *
 * Pan:
 * - "pan left" / "pan right" - Pan horizontally
 * - "pan up" / "pan down" - Pan vertically
 *
 * Selection:
 * - "select [object]" - Select object by name in scene
 * - "deselect" / "clear selection" - Clear current selection
 *
 * Render Mode:
 * - "wireframe" - Switch to wireframe render mode
 * - "solid" - Switch to solid render mode
 * - "textured" - Switch to textured render mode
 */

package com.augmentalis.avamagic.voice.handlers.display

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for 3D Canvas/Viewer interactions.
 *
 * Provides comprehensive voice control for 3D canvas components including:
 * - Rotation control (horizontal, vertical, auto-rotate)
 * - Zoom control (incremental and absolute)
 * - View presets (front, back, left, right, top, bottom, isometric)
 * - Pan control (directional panning)
 * - Object selection and deselection
 * - Render mode switching (wireframe, solid, textured)
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for 3D canvas operations
 */
class Canvas3DHandler(
    private val executor: Canvas3DExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "Canvas3DHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Default rotation step in degrees
        private const val DEFAULT_ROTATION_STEP = 15.0

        // Default zoom step percentage
        private const val DEFAULT_ZOOM_STEP = 10.0

        // Default pan step in units
        private const val DEFAULT_PAN_STEP = 50.0

        // Patterns for parsing commands
        private val ROTATE_PATTERN = Regex(
            """^rotate\s+(left|right|up|down)(?:\s+(\d+)(?:\s*degrees?)?)?$""",
            RegexOption.IGNORE_CASE
        )

        private val ZOOM_PERCENT_PATTERN = Regex(
            """^zoom\s+(?:to\s+)?(\d+)\s*(?:percent|%)$""",
            RegexOption.IGNORE_CASE
        )

        private val ZOOM_DIRECTION_PATTERN = Regex(
            """^zoom\s+(in|out)(?:\s+(\d+)(?:\s*(?:percent|%))?)?$""",
            RegexOption.IGNORE_CASE
        )

        private val PAN_PATTERN = Regex(
            """^pan\s+(left|right|up|down)(?:\s+(\d+))?$""",
            RegexOption.IGNORE_CASE
        )

        private val SELECT_PATTERN = Regex(
            """^select\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val VIEW_PRESET_PATTERN = Regex(
            """^(front|back|left|right|top|bottom|isometric|iso)\s*view$""",
            RegexOption.IGNORE_CASE
        )

        private val RENDER_MODE_PATTERN = Regex(
            """^(wireframe|solid|textured)(?:\s+mode)?$""",
            RegexOption.IGNORE_CASE
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Rotation
        "rotate left", "rotate right",
        "rotate up", "rotate down",
        "spin", "auto rotate", "stop spinning",
        // Zoom
        "zoom in", "zoom out",
        "zoom [N] percent",
        // View control
        "reset view", "home",
        "front view", "back view",
        "left view", "right view",
        "top view", "bottom view",
        "isometric view", "iso view",
        // Pan
        "pan left", "pan right",
        "pan up", "pan down",
        // Selection
        "select [object]", "deselect", "clear selection",
        // Render mode
        "wireframe", "solid", "textured"
    )

    /**
     * Callback for voice feedback when canvas state changes.
     */
    var onStateChanged: ((canvasName: String, operation: String, details: String) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing 3D canvas command: $normalizedAction" }

        return try {
            when {
                // Rotation commands
                ROTATE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleRotate(normalizedAction, command)
                }

                // Auto-rotate toggle
                normalizedAction in listOf("spin", "auto rotate", "auto-rotate") -> {
                    handleToggleAutoRotate(command, enable = true)
                }

                normalizedAction in listOf("stop spinning", "stop spin", "stop auto rotate") -> {
                    handleToggleAutoRotate(command, enable = false)
                }

                // Zoom to specific percentage
                ZOOM_PERCENT_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleZoomPercent(normalizedAction, command)
                }

                // Zoom in/out
                ZOOM_DIRECTION_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleZoomDirection(normalizedAction, command)
                }

                // Simple zoom commands
                normalizedAction == "zoom in" -> {
                    handleSimpleZoom(command, zoomIn = true)
                }

                normalizedAction == "zoom out" -> {
                    handleSimpleZoom(command, zoomIn = false)
                }

                // Reset view
                normalizedAction in listOf("reset view", "home", "reset") -> {
                    handleResetView(command)
                }

                // View presets
                VIEW_PRESET_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleViewPreset(normalizedAction, command)
                }

                // Pan commands
                PAN_PATTERN.containsMatchIn(normalizedAction) -> {
                    handlePan(normalizedAction, command)
                }

                // Select object
                SELECT_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSelect(normalizedAction, command)
                }

                // Deselect/clear selection
                normalizedAction in listOf("deselect", "clear selection", "clear") -> {
                    handleClearSelection(command)
                }

                // Render mode
                RENDER_MODE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleRenderMode(normalizedAction, command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing 3D canvas command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "rotate left/right/up/down [degrees]" commands.
     */
    private suspend fun handleRotate(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = ROTATE_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse rotate command")

        val direction = matchResult.groupValues[1].lowercase()
        val degrees = matchResult.groupValues[2].toDoubleOrNull() ?: DEFAULT_ROTATION_STEP

        // Find the canvas
        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        // Perform rotation based on direction
        val result = when (direction) {
            "left" -> executor.rotateHorizontal(canvasInfo, -degrees)
            "right" -> executor.rotateHorizontal(canvasInfo, degrees)
            "up" -> executor.rotateVertical(canvasInfo, -degrees)
            "down" -> executor.rotateVertical(canvasInfo, degrees)
            else -> return HandlerResult.failure("Unknown rotation direction: $direction")
        }

        return buildRotationResult(canvasInfo, direction, degrees, result)
    }

    /**
     * Handle toggle auto-rotate command.
     */
    private suspend fun handleToggleAutoRotate(
        command: QuantizedCommand,
        enable: Boolean
    ): HandlerResult {
        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        val result = executor.toggleAutoRotate(canvasInfo, enable)

        return if (result.success) {
            val action = if (enable) "enabled" else "disabled"
            val feedback = "Auto-rotation $action"

            onStateChanged?.invoke(
                canvasInfo.name.ifBlank { "Canvas" },
                "auto_rotate",
                action
            )

            Log.i { "Auto-rotate $action for canvas: ${canvasInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "canvasName" to canvasInfo.name,
                    "canvasAvid" to canvasInfo.avid,
                    "autoRotate" to enable,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not toggle auto-rotation",
                recoverable = true
            )
        }
    }

    /**
     * Handle "zoom [N] percent" command.
     */
    private suspend fun handleZoomPercent(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = ZOOM_PERCENT_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse zoom command")

        val zoomLevel = matchResult.groupValues[1].toDoubleOrNull()
            ?: return HandlerResult.Failure(
                reason = "Could not parse zoom level",
                recoverable = true,
                suggestedAction = "Try 'zoom 50 percent' or 'zoom 100%'"
            )

        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        val result = executor.setZoom(canvasInfo, zoomLevel)

        return buildZoomResult(canvasInfo, zoomLevel, result)
    }

    /**
     * Handle "zoom in/out [amount]" command.
     */
    private suspend fun handleZoomDirection(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = ZOOM_DIRECTION_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse zoom command")

        val direction = matchResult.groupValues[1].lowercase()
        val amount = matchResult.groupValues[2].toDoubleOrNull() ?: DEFAULT_ZOOM_STEP

        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        val result = when (direction) {
            "in" -> executor.zoomIn(canvasInfo, amount)
            "out" -> executor.zoomOut(canvasInfo, amount)
            else -> return HandlerResult.failure("Unknown zoom direction: $direction")
        }

        val newZoom = canvasInfo.zoomLevel + (if (direction == "in") amount else -amount)
        return buildZoomResult(canvasInfo, newZoom.coerceIn(10.0, 500.0), result)
    }

    /**
     * Handle simple "zoom in" or "zoom out" command.
     */
    private suspend fun handleSimpleZoom(
        command: QuantizedCommand,
        zoomIn: Boolean
    ): HandlerResult {
        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        val result = if (zoomIn) {
            executor.zoomIn(canvasInfo, DEFAULT_ZOOM_STEP)
        } else {
            executor.zoomOut(canvasInfo, DEFAULT_ZOOM_STEP)
        }

        val newZoom = canvasInfo.zoomLevel + (if (zoomIn) DEFAULT_ZOOM_STEP else -DEFAULT_ZOOM_STEP)
        return buildZoomResult(canvasInfo, newZoom.coerceIn(10.0, 500.0), result)
    }

    /**
     * Handle "reset view" or "home" command.
     */
    private suspend fun handleResetView(command: QuantizedCommand): HandlerResult {
        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        val result = executor.resetView(canvasInfo)

        return if (result.success) {
            val feedback = "View reset to default"

            onStateChanged?.invoke(
                canvasInfo.name.ifBlank { "Canvas" },
                "reset_view",
                "default"
            )

            Log.i { "View reset for canvas: ${canvasInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "canvasName" to canvasInfo.name,
                    "canvasAvid" to canvasInfo.avid,
                    "operation" to "reset_view",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not reset view",
                recoverable = true
            )
        }
    }

    /**
     * Handle view preset commands (front, back, left, right, top, bottom, isometric).
     */
    private suspend fun handleViewPreset(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = VIEW_PRESET_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse view preset command")

        val presetName = matchResult.groupValues[1].lowercase()
        val preset = when (presetName) {
            "front" -> ViewPreset.FRONT
            "back" -> ViewPreset.BACK
            "left" -> ViewPreset.LEFT
            "right" -> ViewPreset.RIGHT
            "top" -> ViewPreset.TOP
            "bottom" -> ViewPreset.BOTTOM
            "isometric", "iso" -> ViewPreset.ISOMETRIC
            else -> return HandlerResult.failure("Unknown view preset: $presetName")
        }

        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        // Check if preset is available
        if (preset.name.lowercase() !in canvasInfo.presetViews.map { it.lowercase() }) {
            return HandlerResult.Failure(
                reason = "View preset '$presetName' not available",
                recoverable = true,
                suggestedAction = "Available presets: ${canvasInfo.presetViews.joinToString(", ")}"
            )
        }

        val result = executor.setPresetView(canvasInfo, preset)

        return if (result.success) {
            val feedback = "Switched to $presetName view"

            onStateChanged?.invoke(
                canvasInfo.name.ifBlank { "Canvas" },
                "preset_view",
                presetName
            )

            Log.i { "Preset view $presetName set for canvas: ${canvasInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "canvasName" to canvasInfo.name,
                    "canvasAvid" to canvasInfo.avid,
                    "preset" to preset.name,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set view preset",
                recoverable = true
            )
        }
    }

    /**
     * Handle "pan left/right/up/down [amount]" commands.
     */
    private suspend fun handlePan(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = PAN_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse pan command")

        val direction = matchResult.groupValues[1].lowercase()
        val amount = matchResult.groupValues[2].toDoubleOrNull() ?: DEFAULT_PAN_STEP

        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        // Calculate pan delta based on direction
        val (deltaX, deltaY) = when (direction) {
            "left" -> -amount to 0.0
            "right" -> amount to 0.0
            "up" -> 0.0 to -amount
            "down" -> 0.0 to amount
            else -> return HandlerResult.failure("Unknown pan direction: $direction")
        }

        val result = executor.pan(canvasInfo, deltaX, deltaY)

        return if (result.success) {
            val feedback = "Panned $direction"

            onStateChanged?.invoke(
                canvasInfo.name.ifBlank { "Canvas" },
                "pan",
                direction
            )

            Log.i { "Panned $direction for canvas: ${canvasInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "canvasName" to canvasInfo.name,
                    "canvasAvid" to canvasInfo.avid,
                    "direction" to direction,
                    "deltaX" to deltaX,
                    "deltaY" to deltaY,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not pan view",
                recoverable = true
            )
        }
    }

    /**
     * Handle "select [object]" command.
     */
    private suspend fun handleSelect(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SELECT_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse select command")

        val objectName = matchResult.groupValues[1].trim()

        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        val result = executor.selectObject(canvasInfo, objectName)

        return if (result.success) {
            val feedback = "Selected '$objectName'"

            onStateChanged?.invoke(
                canvasInfo.name.ifBlank { "Canvas" },
                "select",
                objectName
            )

            Log.i { "Selected object '$objectName' in canvas: ${canvasInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "canvasName" to canvasInfo.name,
                    "canvasAvid" to canvasInfo.avid,
                    "selectedObject" to objectName,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Object '$objectName' not found",
                recoverable = true,
                suggestedAction = "Check the object name and try again"
            )
        }
    }

    /**
     * Handle "deselect" or "clear selection" command.
     */
    private suspend fun handleClearSelection(command: QuantizedCommand): HandlerResult {
        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        val result = executor.clearSelection(canvasInfo)

        return if (result.success) {
            val feedback = "Selection cleared"

            onStateChanged?.invoke(
                canvasInfo.name.ifBlank { "Canvas" },
                "clear_selection",
                ""
            )

            Log.i { "Selection cleared for canvas: ${canvasInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "canvasName" to canvasInfo.name,
                    "canvasAvid" to canvasInfo.avid,
                    "operation" to "clear_selection",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not clear selection",
                recoverable = true
            )
        }
    }

    /**
     * Handle "wireframe", "solid", "textured" commands.
     */
    private suspend fun handleRenderMode(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = RENDER_MODE_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse render mode command")

        val modeName = matchResult.groupValues[1].lowercase()
        val mode = when (modeName) {
            "wireframe" -> RenderMode.WIREFRAME
            "solid" -> RenderMode.SOLID
            "textured" -> RenderMode.TEXTURED
            else -> return HandlerResult.failure("Unknown render mode: $modeName")
        }

        val canvasInfo = findCanvas(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No 3D canvas found",
                recoverable = true,
                suggestedAction = "Focus on a 3D canvas first"
            )

        val result = executor.setRenderMode(canvasInfo, mode)

        return if (result.success) {
            val feedback = "Switched to $modeName mode"

            onStateChanged?.invoke(
                canvasInfo.name.ifBlank { "Canvas" },
                "render_mode",
                modeName
            )

            Log.i { "Render mode set to $modeName for canvas: ${canvasInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "canvasName" to canvasInfo.name,
                    "canvasAvid" to canvasInfo.avid,
                    "renderMode" to mode.name,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set render mode",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find canvas by AVID, name, or focus state.
     */
    private suspend fun findCanvas(
        name: String? = null,
        avid: String? = null
    ): Canvas3DInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val canvas = executor.findByAvid(avid)
            if (canvas != null) return canvas
        }

        // Priority 2: Name lookup
        if (name != null) {
            val canvas = executor.findByName(name)
            if (canvas != null) return canvas
        }

        // Priority 3: Focused canvas
        return executor.findFocused()
    }

    /**
     * Build result for rotation operations.
     */
    private fun buildRotationResult(
        canvasInfo: Canvas3DInfo,
        direction: String,
        degrees: Double,
        result: Canvas3DOperationResult
    ): HandlerResult {
        return if (result.success) {
            val feedback = "Rotated $direction ${degrees.toInt()} degrees"

            onStateChanged?.invoke(
                canvasInfo.name.ifBlank { "Canvas" },
                "rotate",
                "$direction ${degrees.toInt()}deg"
            )

            Log.i { "Rotated $direction ${degrees}deg for canvas: ${canvasInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "canvasName" to canvasInfo.name,
                    "canvasAvid" to canvasInfo.avid,
                    "direction" to direction,
                    "degrees" to degrees,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not rotate view",
                recoverable = true
            )
        }
    }

    /**
     * Build result for zoom operations.
     */
    private fun buildZoomResult(
        canvasInfo: Canvas3DInfo,
        zoomLevel: Double,
        result: Canvas3DOperationResult
    ): HandlerResult {
        return if (result.success) {
            val feedback = "Zoom set to ${zoomLevel.toInt()}%"

            onStateChanged?.invoke(
                canvasInfo.name.ifBlank { "Canvas" },
                "zoom",
                "${zoomLevel.toInt()}%"
            )

            Log.i { "Zoom set to $zoomLevel% for canvas: ${canvasInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "canvasName" to canvasInfo.name,
                    "canvasAvid" to canvasInfo.avid,
                    "zoomLevel" to zoomLevel,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not adjust zoom",
                recoverable = true
            )
        }
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * View preset positions for 3D canvas.
 */
enum class ViewPreset {
    /** Front-facing view (looking at -Z axis) */
    FRONT,
    /** Back-facing view (looking at +Z axis) */
    BACK,
    /** Left-facing view (looking at +X axis) */
    LEFT,
    /** Right-facing view (looking at -X axis) */
    RIGHT,
    /** Top-down view (looking at -Y axis) */
    TOP,
    /** Bottom-up view (looking at +Y axis) */
    BOTTOM,
    /** Isometric view (45-degree angle) */
    ISOMETRIC
}

/**
 * Render mode for 3D canvas.
 */
enum class RenderMode {
    /** Wireframe rendering - shows edges only */
    WIREFRAME,
    /** Solid rendering - shows filled surfaces without textures */
    SOLID,
    /** Textured rendering - shows full textures and materials */
    TEXTURED
}

/**
 * 3D rotation representation.
 *
 * @property x Rotation around X axis in degrees
 * @property y Rotation around Y axis in degrees
 * @property z Rotation around Z axis in degrees
 */
data class Rotation3D(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {
    companion object {
        val ZERO = Rotation3D(0.0, 0.0, 0.0)
    }

    /**
     * Add another rotation to this one.
     */
    operator fun plus(other: Rotation3D): Rotation3D = Rotation3D(
        x = x + other.x,
        y = y + other.y,
        z = z + other.z
    )
}

/**
 * Information about a 3D canvas component.
 *
 * @property avid AVID fingerprint for the canvas (format: C3D:{hash8})
 * @property name Display name or associated label
 * @property rotation Current rotation state
 * @property zoomLevel Current zoom level as percentage (100 = default)
 * @property isAutoRotating Whether auto-rotation is currently enabled
 * @property selectedObject Name of currently selected object, null if none
 * @property renderMode Current render mode
 * @property presetViews List of available preset view names
 * @property bounds Screen bounds for the canvas
 * @property isFocused Whether this canvas currently has focus
 * @property node Platform-specific node reference
 */
data class Canvas3DInfo(
    val avid: String,
    val name: String = "",
    val rotation: Rotation3D = Rotation3D.ZERO,
    val zoomLevel: Double = 100.0,
    val isAutoRotating: Boolean = false,
    val selectedObject: String? = null,
    val renderMode: RenderMode = RenderMode.SOLID,
    val presetViews: List<String> = listOf(
        "front", "back", "left", "right", "top", "bottom", "isometric"
    ),
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Canvas3D",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = buildString {
            append("Zoom: ${zoomLevel.toInt()}%")
            if (selectedObject != null) {
                append(", Selected: $selectedObject")
            }
            append(", Mode: ${renderMode.name.lowercase()}")
        }
    )
}

/**
 * Result of a 3D canvas operation.
 */
data class Canvas3DOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousState: Map<String, Any>? = null,
    val newState: Map<String, Any>? = null
) {
    companion object {
        /**
         * Create a successful result.
         */
        fun success(
            previousState: Map<String, Any>? = null,
            newState: Map<String, Any>? = null
        ) = Canvas3DOperationResult(
            success = true,
            previousState = previousState,
            newState = newState
        )

        /**
         * Create an error result.
         */
        fun error(message: String) = Canvas3DOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for 3D canvas operations.
 *
 * Implementations should:
 * 1. Find 3D canvas components by AVID, name, or focus state
 * 2. Read current canvas state (rotation, zoom, selection)
 * 3. Apply transformations via platform-specific APIs
 * 4. Handle various 3D rendering frameworks (OpenGL, Vulkan, SceneView, etc.)
 *
 * ## Canvas Detection Algorithm
 *
 * ```kotlin
 * fun findCanvasNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - android.opengl.GLSurfaceView
 *     // - com.google.ar.sceneform.SceneView
 *     // - Custom 3D canvas implementations
 *     // - Views with "canvas" or "3d" in contentDescription
 * }
 * ```
 *
 * ## Transformation Application
 *
 * ```kotlin
 * fun applyRotation(canvas: Canvas3DInfo, rotation: Rotation3D): Boolean {
 *     // Send rotation intent/broadcast to canvas
 *     // Or use accessibility actions if supported
 *     // Or invoke JavaScript bridge for WebGL canvases
 * }
 * ```
 */
interface Canvas3DExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Canvas Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a 3D canvas by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: C3D:{hash8})
     * @return Canvas3DInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): Canvas3DInfo?

    /**
     * Find a 3D canvas by its name or associated label.
     *
     * Searches for:
     * 1. Canvas with matching contentDescription
     * 2. Canvas with associated label text
     * 3. Canvas in view with matching title
     *
     * @param name The name to search for (case-insensitive)
     * @return Canvas3DInfo if found, null otherwise
     */
    suspend fun findByName(name: String): Canvas3DInfo?

    /**
     * Find the currently focused 3D canvas.
     *
     * @return Canvas3DInfo if a canvas has focus, null otherwise
     */
    suspend fun findFocused(): Canvas3DInfo?

    // ═══════════════════════════════════════════════════════════════════════════
    // Rotation Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Rotate the canvas horizontally (around Y axis).
     *
     * @param canvas The canvas to rotate
     * @param degrees Rotation amount in degrees (positive = right, negative = left)
     * @return Operation result
     */
    suspend fun rotateHorizontal(canvas: Canvas3DInfo, degrees: Double): Canvas3DOperationResult

    /**
     * Rotate the canvas vertically (around X axis).
     *
     * @param canvas The canvas to rotate
     * @param degrees Rotation amount in degrees (positive = down, negative = up)
     * @return Operation result
     */
    suspend fun rotateVertical(canvas: Canvas3DInfo, degrees: Double): Canvas3DOperationResult

    /**
     * Toggle auto-rotation mode.
     *
     * @param canvas The canvas to modify
     * @param enable True to enable auto-rotation, false to disable
     * @return Operation result
     */
    suspend fun toggleAutoRotate(canvas: Canvas3DInfo, enable: Boolean): Canvas3DOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Zoom Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set zoom to a specific level.
     *
     * @param canvas The canvas to zoom
     * @param zoomPercent Target zoom level as percentage (100 = default)
     * @return Operation result
     */
    suspend fun setZoom(canvas: Canvas3DInfo, zoomPercent: Double): Canvas3DOperationResult

    /**
     * Zoom in by a relative amount.
     *
     * @param canvas The canvas to zoom
     * @param amount Amount to zoom in as percentage points
     * @return Operation result
     */
    suspend fun zoomIn(canvas: Canvas3DInfo, amount: Double): Canvas3DOperationResult

    /**
     * Zoom out by a relative amount.
     *
     * @param canvas The canvas to zoom
     * @param amount Amount to zoom out as percentage points
     * @return Operation result
     */
    suspend fun zoomOut(canvas: Canvas3DInfo, amount: Double): Canvas3DOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // View Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Reset the view to default position.
     *
     * @param canvas The canvas to reset
     * @return Operation result
     */
    suspend fun resetView(canvas: Canvas3DInfo): Canvas3DOperationResult

    /**
     * Set the view to a preset camera position.
     *
     * @param canvas The canvas to modify
     * @param preset The view preset to apply
     * @return Operation result
     */
    suspend fun setPresetView(canvas: Canvas3DInfo, preset: ViewPreset): Canvas3DOperationResult

    /**
     * Pan the view by a delta offset.
     *
     * @param canvas The canvas to pan
     * @param deltaX Horizontal pan amount (positive = right)
     * @param deltaY Vertical pan amount (positive = down)
     * @return Operation result
     */
    suspend fun pan(canvas: Canvas3DInfo, deltaX: Double, deltaY: Double): Canvas3DOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Selection Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select an object in the scene by name.
     *
     * @param canvas The canvas containing the object
     * @param objectName Name of the object to select
     * @return Operation result (failure if object not found)
     */
    suspend fun selectObject(canvas: Canvas3DInfo, objectName: String): Canvas3DOperationResult

    /**
     * Clear the current selection.
     *
     * @param canvas The canvas to clear selection from
     * @return Operation result
     */
    suspend fun clearSelection(canvas: Canvas3DInfo): Canvas3DOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Render Mode Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set the render mode for the canvas.
     *
     * @param canvas The canvas to modify
     * @param mode The render mode to apply
     * @return Operation result
     */
    suspend fun setRenderMode(canvas: Canvas3DInfo, mode: RenderMode): Canvas3DOperationResult
}
