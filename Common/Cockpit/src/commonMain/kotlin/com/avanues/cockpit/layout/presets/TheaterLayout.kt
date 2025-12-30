package com.avanues.cockpit.layout.presets

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * THEATER Layout Preset
 *
 * Arranges windows in an immersive cinema-style semicircular arrangement with tiered seating.
 * Larger radius than ArcFrontLayout for cinematic feel, with windows tilted upward like viewing
 * a theater screen. Supports tiered rows for stadium seating effect when many windows present.
 *
 * Recursive Design Analysis (.rot):
 *
 * Level 1: Theater Geometry Fundamentals
 * - Immersive semicircle mimicking cinema screen experience
 * - Larger radius than ArcFrontLayout (4.5m vs 3.0m) for cinematic depth
 * - Wider arc span (150° vs 120°) to utilize peripheral vision
 * - Windows tilted upward 5-10° (ergonomic neck angle for screen viewing)
 * - Creates feeling of sitting in theater looking at large screen
 *
 * Level 2: Tiered Arrangement Strategy
 * - Single row (≤6 windows): All windows on same horizontal plane
 * - Multiple rows (>6 windows): Stadium seating with front/back rows
 * - Front row: Closer (radius=4.0m), normal height (centerPoint.y)
 * - Back row: Farther (radius=5.0m), elevated (centerPoint.y + 0.4m)
 * - Distribute windows evenly: alternate or split by count
 * - Back row elevated to see over front row (stadium effect)
 *
 * Level 3: Window Scaling Compensation
 * - Distance affects perceived size (farther = appears smaller)
 * - Front row windows: 1.0x scale (baseline size)
 * - Back row windows: 1.2x scale (compensates for 25% extra distance)
 * - Maintains consistent readability across all positions
 * - Scale factor derived from distance ratio: 5.0/4.0 ≈ 1.25
 * - Implemented via calculateDimensions() method
 *
 * Level 4: Comfort Optimization
 * - Upward tilt (8°): Mimics natural theater screen viewing angle
 * - Reduces neck strain compared to flat arrangement
 * - Edge windows: Same density as center (wide arc accommodates)
 * - Center region: Primary focus area (most comfortable viewing)
 * - Peripheral windows: Accessible via slight head turn
 *
 * Formula (Single Row):
 *   For window at index i of N windows (N ≤ 6):
 *     angle = (i * 150° / (N-1) - 75°) * (PI/180)
 *     x = 4.5 * sin(angle)
 *     y = centerPoint.y + 0.2  // Slightly elevated
 *     z = centerPoint.z - 4.5 * (1 - cos(angle))
 *     rotationY = -angle * (180/PI)
 *     rotationX = 8°  // Tilted upward
 *
 * Formula (Two Rows):
 *   Front row (even indices): radius=4.0m, y=centerPoint.y
 *   Back row (odd indices): radius=5.0m, y=centerPoint.y+0.4
 *   Back row dimensions scaled 1.2x via calculateDimensions()
 *
 * Voice: "Theater mode"
 */
object TheaterLayout : LayoutPreset {

    override val id = "THEATER"
    override val voiceCommand = "Theater mode"
    override val maxWindows = 12  // 6 per row max
    override val minWindows = 1

    // Theater configuration constants
    private const val SINGLE_ROW_RADIUS_METERS = 4.5f
    private const val FRONT_ROW_RADIUS_METERS = 4.0f
    private const val BACK_ROW_RADIUS_METERS = 5.0f
    private const val ARC_SPAN_DEGREES = 150f  // Wider than ArcFrontLayout
    private const val UPWARD_TILT_DEGREES = 8f  // Ergonomic upward viewing angle
    private const val ELEVATION_OFFSET_METERS = 0.2f  // Single row slight elevation
    private const val BACK_ROW_ELEVATION_METERS = 0.4f  // Stadium seating height
    private const val BACK_ROW_SCALE_FACTOR = 1.2f  // Compensate for distance
    private const val TWO_ROW_THRESHOLD = 6  // More than this triggers two rows
    private const val DEG_TO_RAD = (PI / 180.0).toFloat()
    private const val RAD_TO_DEG = (180.0 / PI).toFloat()

    override fun calculatePositions(
        windows: List<AppWindow>,
        centerPoint: Vector3D
    ): List<WindowPosition> {
        val count = windows.size.coerceIn(minWindows, maxWindows)

        // Edge case: Single window at center (0° angle)
        if (count == 1) {
            return listOf(
                WindowPosition(
                    windowId = windows[0].id,
                    position = Vector3D(
                        x = centerPoint.x,
                        y = centerPoint.y + ELEVATION_OFFSET_METERS,
                        z = centerPoint.z - SINGLE_ROW_RADIUS_METERS
                    ),
                    rotationX = UPWARD_TILT_DEGREES,
                    rotationY = 0f,
                    rotationZ = 0f
                )
            )
        }

        // Determine if we need two rows (stadium seating)
        val useTwoRows = count > TWO_ROW_THRESHOLD

        return if (useTwoRows) {
            calculateTwoRowPositions(windows, count, centerPoint)
        } else {
            calculateSingleRowPositions(windows, count, centerPoint)
        }
    }

    /**
     * Calculate positions for single row theater arrangement (≤6 windows)
     */
    private fun calculateSingleRowPositions(
        windows: List<AppWindow>,
        count: Int,
        centerPoint: Vector3D
    ): List<WindowPosition> {
        val angleStep = ARC_SPAN_DEGREES / (count - 1)

        return windows.take(count).mapIndexed { index, window ->
            // Calculate angle for this window (-75° to +75° for 150° span)
            val angleDegrees = (index * angleStep) - (ARC_SPAN_DEGREES / 2)
            val angleRadians = angleDegrees * DEG_TO_RAD

            // Calculate position on arc perimeter using polar coordinates
            val x = centerPoint.x + (SINGLE_ROW_RADIUS_METERS * sin(angleRadians))
            val y = centerPoint.y + ELEVATION_OFFSET_METERS
            val z = centerPoint.z - SINGLE_ROW_RADIUS_METERS * (1 - cos(angleRadians))

            // Rotate window to face user, tilted upward for theater viewing
            val rotationY = -angleDegrees

            WindowPosition(
                windowId = window.id,
                position = Vector3D(x, y, z),
                rotationX = UPWARD_TILT_DEGREES,
                rotationY = rotationY,
                rotationZ = 0f
            )
        }
    }

    /**
     * Calculate positions for two-row stadium seating arrangement (>6 windows)
     *
     * Strategy: Distribute windows across front and back rows
     * - Even indices (0, 2, 4...): Front row (closer, normal height)
     * - Odd indices (1, 3, 5...): Back row (farther, elevated)
     */
    private fun calculateTwoRowPositions(
        windows: List<AppWindow>,
        count: Int,
        centerPoint: Vector3D
    ): List<WindowPosition> {
        val frontRowCount = (count + 1) / 2  // Ceiling division
        val backRowCount = count / 2         // Floor division

        val frontRowAngleStep = if (frontRowCount > 1) {
            ARC_SPAN_DEGREES / (frontRowCount - 1)
        } else {
            0f
        }
        val backRowAngleStep = if (backRowCount > 1) {
            ARC_SPAN_DEGREES / (backRowCount - 1)
        } else {
            0f
        }

        val positions = mutableListOf<WindowPosition>()
        var frontRowIndex = 0
        var backRowIndex = 0

        windows.take(count).forEachIndexed { index, window ->
            val isFrontRow = index % 2 == 0

            if (isFrontRow) {
                // Front row window (closer, normal height, normal scale)
                val angleDegrees = if (frontRowCount > 1) {
                    (frontRowIndex * frontRowAngleStep) - (ARC_SPAN_DEGREES / 2)
                } else {
                    0f
                }
                val angleRadians = angleDegrees * DEG_TO_RAD

                val x = centerPoint.x + (FRONT_ROW_RADIUS_METERS * sin(angleRadians))
                val y = centerPoint.y
                val z = centerPoint.z - FRONT_ROW_RADIUS_METERS * (1 - cos(angleRadians))

                positions.add(
                    WindowPosition(
                        windowId = window.id,
                        position = Vector3D(x, y, z),
                        rotationX = UPWARD_TILT_DEGREES,
                        rotationY = -angleDegrees,
                        rotationZ = 0f
                    )
                )
                frontRowIndex++
            } else {
                // Back row window (farther, elevated, scaled up via calculateDimensions)
                val angleDegrees = if (backRowCount > 1) {
                    (backRowIndex * backRowAngleStep) - (ARC_SPAN_DEGREES / 2)
                } else {
                    0f
                }
                val angleRadians = angleDegrees * DEG_TO_RAD

                val x = centerPoint.x + (BACK_ROW_RADIUS_METERS * sin(angleRadians))
                val y = centerPoint.y + BACK_ROW_ELEVATION_METERS
                val z = centerPoint.z - BACK_ROW_RADIUS_METERS * (1 - cos(angleRadians))

                positions.add(
                    WindowPosition(
                        windowId = window.id,
                        position = Vector3D(x, y, z),
                        rotationX = UPWARD_TILT_DEGREES,
                        rotationY = -angleDegrees,
                        rotationZ = 0f
                    )
                )
                backRowIndex++
            }
        }

        return positions
    }

    override fun calculateDimensions(
        window: AppWindow,
        index: Int,
        totalWindows: Int
    ): WindowDimensions {
        // Determine if this window is in back row
        val useTwoRows = totalWindows > TWO_ROW_THRESHOLD
        val isBackRow = useTwoRows && (index % 2 != 0)

        // Base dimensions (slightly larger than ArcFrontLayout for cinematic feel)
        val baseWidth = 0.85f   // ~33 inches at 4.5m distance
        val baseHeight = 0.65f  // ~26 inches at 4.5m distance

        // Scale up back row windows to compensate for distance
        val scaleFactor = if (isBackRow) BACK_ROW_SCALE_FACTOR else 1.0f

        return WindowDimensions(
            widthMeters = baseWidth * scaleFactor,
            heightMeters = baseHeight * scaleFactor
        )
    }
}
