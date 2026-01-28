package com.augmentalis.commandmanager

/**
 * Represents a drag operation with start and end coordinates.
 *
 * @property startX The starting X coordinate
 * @property startY The starting Y coordinate
 * @property endX The ending X coordinate
 * @property endY The ending Y coordinate
 * @property duration The duration of the drag in milliseconds
 */
data class DragOperation(
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
    val duration: Long = 300L
)

/**
 * Result of a drag operation attempt.
 *
 * @property success Whether the drag was successfully created
 * @property operation The drag operation if successful
 * @property error Error message if the drag failed
 */
data class DragResult(
    val success: Boolean,
    val operation: DragOperation? = null,
    val error: String? = null
)

/**
 * Handles drag-related voice commands.
 *
 * This class parses voice commands for drag operations and creates
 * corresponding DragOperation objects that can be executed by the
 * platform-specific action executors.
 *
 * Supported commands:
 * - "drag up" / "drag down" / "drag left" / "drag right" - directional drags from center
 * - "drag up 300" / "drag down 150" etc. - directional drags with custom distance
 * - "drag from 100,200 to 300,400" - coordinate-based drag
 *
 * Usage:
 * ```
 * val handler = DragHandler()
 *
 * // Simple directional drag
 * val result = handler.handleCommand("drag up")
 * if (result.success) {
 *     val operation = result.operation!!
 *     // Execute the drag from (operation.startX, operation.startY) to (operation.endX, operation.endY)
 * }
 *
 * // Coordinate-based drag
 * val result2 = handler.handleCommand("drag from 100,200 to 300,400")
 * ```
 */
class DragHandler {

    /**
     * Handles a voice command to create a drag operation.
     *
     * Recognizes commands like:
     * - "drag up" - drags up from center by default distance
     * - "drag down" - drags down from center by default distance
     * - "drag left" - drags left from center by default distance
     * - "drag right" - drags right from center by default distance
     * - "drag up 300" - drags up from center by 300 pixels
     * - "drag from 100,200 to 300,400" - drags from coordinates to coordinates
     *
     * @param command The voice command string
     * @return DragResult indicating success/failure with the drag operation
     */
    fun handleCommand(command: String): DragResult {
        val normalizedCommand = command.lowercase().trim()

        return when {
            normalizedCommand.isEmpty() -> {
                DragResult(false, error = "Unknown drag command: $command")
            }
            normalizedCommand.startsWith("drag from ") -> {
                parseDragFromTo(command)
            }
            normalizedCommand == "drag up" -> {
                createDirectionalDrag(Direction.UP)
            }
            normalizedCommand == "drag down" -> {
                createDirectionalDrag(Direction.DOWN)
            }
            normalizedCommand == "drag left" -> {
                createDirectionalDrag(Direction.LEFT)
            }
            normalizedCommand == "drag right" -> {
                createDirectionalDrag(Direction.RIGHT)
            }
            normalizedCommand.startsWith("drag ") -> {
                parseDragDirection(normalizedCommand)
            }
            else -> {
                DragResult(false, error = "Unknown drag command: $command")
            }
        }
    }

    /**
     * Creates a drag operation with the specified coordinates.
     *
     * @param startX Starting X coordinate (must be >= 0)
     * @param startY Starting Y coordinate (must be >= 0)
     * @param endX Ending X coordinate (must be >= 0)
     * @param endY Ending Y coordinate (must be >= 0)
     * @param duration Duration of the drag in milliseconds
     * @return DragResult with the created operation or an error
     */
    fun createDrag(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = 300L): DragResult {
        if (startX < 0 || startY < 0 || endX < 0 || endY < 0) {
            return DragResult(false, error = "Invalid coordinates")
        }
        val operation = DragOperation(startX, startY, endX, endY, duration)
        return DragResult(true, operation)
    }

    /**
     * Parses a "drag from X,Y to X,Y" command.
     *
     * @param command The command string
     * @return DragResult with parsed coordinates or error
     */
    private fun parseDragFromTo(command: String): DragResult {
        val regex = Regex("""drag from (\d+),(\d+) to (\d+),(\d+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(command)
            ?: return DragResult(false, error = "Could not parse coordinates from: $command")

        val (startX, startY, endX, endY) = match.destructured
        return createDrag(startX.toInt(), startY.toInt(), endX.toInt(), endY.toInt())
    }

    /**
     * Parses a "drag [direction] [optional distance]" command.
     *
     * @param command Normalized (lowercase, trimmed) command string
     * @return DragResult with directional drag or error
     */
    private fun parseDragDirection(command: String): DragResult {
        val parts = command.substringAfter("drag ").trim().split(" ")
        val direction = Direction.fromString(parts.firstOrNull() ?: "")
            ?: return DragResult(false, error = "Unknown direction in: $command")

        val distance = parts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_DRAG_DISTANCE
        return createDirectionalDrag(direction, distance)
    }

    /**
     * Creates a directional drag from the screen center.
     *
     * @param direction The direction to drag
     * @param distance The distance to drag in pixels
     * @return DragResult with the created operation
     */
    private fun createDirectionalDrag(direction: Direction, distance: Int = DEFAULT_DRAG_DISTANCE): DragResult {
        // Assumes center of screen as starting point (540, 960 for 1080x1920)
        val centerX = 540
        val centerY = 960

        val (endX, endY) = when (direction) {
            Direction.UP -> Pair(centerX, centerY - distance)
            Direction.DOWN -> Pair(centerX, centerY + distance)
            Direction.LEFT -> Pair(centerX - distance, centerY)
            Direction.RIGHT -> Pair(centerX + distance, centerY)
        }

        return createDrag(centerX, centerY, endX, endY)
    }

    /**
     * Directions for directional drag commands.
     */
    enum class Direction {
        UP, DOWN, LEFT, RIGHT;

        companion object {
            /**
             * Parses a direction from a string (case-insensitive).
             *
             * @param s The string to parse
             * @return The matching Direction or null if not found
             */
            fun fromString(s: String): Direction? = entries.find { it.name.equals(s, ignoreCase = true) }
        }
    }

    companion object {
        /**
         * Default distance for directional drags in pixels.
         */
        const val DEFAULT_DRAG_DISTANCE = 200
    }
}
