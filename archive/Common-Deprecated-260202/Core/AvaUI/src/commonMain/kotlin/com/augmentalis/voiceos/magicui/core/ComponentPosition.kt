package com.augmentalis.avanues.avaui.core

import kotlinx.serialization.Serializable

/**
 * Represents the 3D position of a UI component.
 *
 * Coordinates are measured in density-independent pixels (dp) for cross-platform consistency.
 *
 * @property x Horizontal position (0 = left edge)
 * @property y Vertical position (0 = top edge)
 * @property z Depth/z-index for layering (higher values appear above lower values, default 0)
 *
 * @since 3.1.0
 */
@Serializable
data class ComponentPosition(
    val x: Float,
    val y: Float,
    val z: Float = 0f
) {
    /**
     * Creates a 2D position with z = 0.
     */
    constructor(x: Float, y: Float) : this(x, y, 0f)

    /**
     * Returns a copy of this position offset by the given deltas.
     */
    fun offset(dx: Float, dy: Float, dz: Float = 0f): ComponentPosition {
        return ComponentPosition(x + dx, y + dy, z + dz)
    }

    /**
     * Returns the Euclidean distance to another position (2D, ignoring z).
     */
    fun distanceTo(other: ComponentPosition): Float {
        val dx = other.x - x
        val dy = other.y - y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    companion object {
        /**
         * Origin position (0, 0, 0).
         */
        val ORIGIN = ComponentPosition(0f, 0f, 0f)
    }
}
