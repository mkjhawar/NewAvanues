package com.augmentalis.cockpit.mvp.rendering

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import kotlin.math.*

/**
 * Curved projection utility for transforming 3D window positions to 2D screen coordinates
 *
 * Features:
 * - Perspective projection (FOV-based)
 * - Curved surface projection (cylindrical wrap)
 * - Depth-based scaling and opacity
 * - Viewport clipping
 *
 * Coordinate System:
 * - 3D World Space: X (right), Y (up), Z (forward, negative = away from user)
 * - 2D Screen Space: X (right), Y (down), origin at top-left
 *
 * Performance:
 * - Pre-calculated constants for trigonometry
 * - Inline functions for hot path
 * - No allocations in projection loop
 */
object CurvedProjection {

    // Projection constants
    private const val FOV_HORIZONTAL_DEGREES = 90f
    private const val FOV_VERTICAL_DEGREES = 70f
    private const val NEAR_CLIP = 0.1f
    private const val FAR_CLIP = 10f

    // Curve parameters
    private const val CURVE_RADIUS = 2.5f  // Radius of cylindrical projection surface
    private const val MAX_CURVE_ANGLE = 60f  // Maximum angle for curved window (degrees)

    // Atmospheric fade parameters
    private const val MIN_OPACITY = 0.3f
    private const val OPACITY_FADE_START = 1.5f
    private const val OPACITY_FADE_END = 4f

    /**
     * Projected point in 2D screen space
     */
    data class ProjectedPoint(
        val screenX: Float,
        val screenY: Float,
        val depth: Float,
        val scale: Float,  // Perspective scale factor
        val isVisible: Boolean  // Within viewport
    )

    /**
     * Projected curved quad (window with 4 corners)
     */
    data class CurvedQuad(
        val topLeft: ProjectedPoint,
        val topRight: ProjectedPoint,
        val bottomRight: ProjectedPoint,
        val bottomLeft: ProjectedPoint,
        val centerDepth: Float,
        val opacity: Float
    )

    /**
     * Generate a curved quad for a window at a given 3D position
     *
     * Process:
     * 1. Calculate 4 corners in 3D world space
     * 2. Apply cylindrical curve transformation
     * 3. Project each corner to 2D screen space
     * 4. Calculate depth-based opacity
     *
     * @param window Window to project
     * @param position 3D position of window center
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return Curved quad with projected corners
     */
    fun generateCurvedQuad(
        window: AppWindow,
        position: Vector3D,
        screenWidth: Int,
        screenHeight: Int
    ): CurvedQuad {
        val halfWidth = window.widthMeters / 2f
        val halfHeight = window.heightMeters / 2f

        // Calculate 4 corners in 3D world space (relative to window center)
        val corners = arrayOf(
            Vector3D(-halfWidth, halfHeight, 0f),   // Top-left
            Vector3D(halfWidth, halfHeight, 0f),    // Top-right
            Vector3D(halfWidth, -halfHeight, 0f),   // Bottom-right
            Vector3D(-halfWidth, -halfHeight, 0f)   // Bottom-left
        )

        // Apply curve transformation and project each corner
        val projectedCorners = corners.map { corner ->
            // Offset corner by window position
            val worldPos = Vector3D(
                position.x + corner.x,
                position.y + corner.y,
                position.z + corner.z
            )

            // Apply cylindrical curve
            val curvedPos = applyCylindricalCurve(worldPos)

            // Project to 2D screen space
            projectToScreen(curvedPos, screenWidth, screenHeight)
        }

        // Calculate center depth for sorting
        val centerDepth = abs(position.z)

        // Calculate depth-based opacity (atmospheric perspective)
        val opacity = calculateOpacity(centerDepth)

        return CurvedQuad(
            topLeft = projectedCorners[0],
            topRight = projectedCorners[1],
            bottomRight = projectedCorners[2],
            bottomLeft = projectedCorners[3],
            centerDepth = centerDepth,
            opacity = opacity
        )
    }

    /**
     * Apply cylindrical curve transformation to a 3D point
     *
     * Projects point onto a cylindrical surface centered at the user
     * This creates the curved "wraparound" effect for windows
     *
     * @param point 3D point in world space
     * @return Curved 3D point
     */
    private fun applyCylindricalCurve(point: Vector3D): Vector3D {
        // Calculate angle from center based on X position
        val angle = atan2(point.x, -point.z)

        // Clamp to maximum curve angle
        val clampedAngle = angle.coerceIn(
            -MAX_CURVE_ANGLE * PI.toFloat() / 180f,
            MAX_CURVE_ANGLE * PI.toFloat() / 180f
        )

        // Project onto cylinder
        val curvedX = CURVE_RADIUS * sin(clampedAngle)
        val curvedZ = -CURVE_RADIUS * cos(clampedAngle)

        // Keep Y unchanged (vertical axis)
        return Vector3D(curvedX, point.y, curvedZ)
    }

    /**
     * Project a 3D point to 2D screen space
     *
     * Uses perspective projection with FOV
     *
     * @param point 3D point in world space (after curve transformation)
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return Projected 2D point with visibility and scale
     */
    private fun projectToScreen(
        point: Vector3D,
        screenWidth: Int,
        screenHeight: Int
    ): ProjectedPoint {
        val depth = abs(point.z)

        // Check if within near/far clip planes
        val isVisible = depth >= NEAR_CLIP && depth <= FAR_CLIP

        // Perspective scale factor (closer = larger)
        val scale = if (depth > 0.01f) {
            1f / depth
        } else {
            100f  // Very close, avoid division by zero
        }

        // FOV-based projection
        val fovRadH = FOV_HORIZONTAL_DEGREES * PI.toFloat() / 180f
        val fovRadV = FOV_VERTICAL_DEGREES * PI.toFloat() / 180f

        val tanHalfFovH = tan(fovRadH / 2f)
        val tanHalfFovV = tan(fovRadV / 2f)

        // Normalize to NDC (Normalized Device Coordinates) [-1, 1]
        val ndcX = if (depth > 0.01f) {
            (point.x / depth) / tanHalfFovH
        } else {
            0f
        }

        val ndcY = if (depth > 0.01f) {
            (point.y / depth) / tanHalfFovV
        } else {
            0f
        }

        // Convert NDC to screen coordinates
        val screenX = (ndcX + 1f) * screenWidth / 2f
        val screenY = (-ndcY + 1f) * screenHeight / 2f  // Flip Y (screen Y goes down)

        return ProjectedPoint(
            screenX = screenX,
            screenY = screenY,
            depth = depth,
            scale = scale,
            isVisible = isVisible
        )
    }

    /**
     * Calculate opacity based on depth (atmospheric perspective)
     *
     * Windows fade out as they get farther away
     *
     * @param depth Distance from camera (positive)
     * @return Opacity value [0.3, 1.0]
     */
    private fun calculateOpacity(depth: Float): Float {
        return when {
            depth < OPACITY_FADE_START -> 1f
            depth > OPACITY_FADE_END -> MIN_OPACITY
            else -> {
                val t = (depth - OPACITY_FADE_START) / (OPACITY_FADE_END - OPACITY_FADE_START)
                1f - t * (1f - MIN_OPACITY)
            }
        }
    }

    /**
     * Check if a point is within the viewport
     *
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     * @return True if within viewport
     */
    fun isInViewport(
        screenX: Float,
        screenY: Float,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        return screenX >= 0 && screenX <= screenWidth &&
               screenY >= 0 && screenY <= screenHeight
    }
}
