package com.avanues.cockpit.core.workspace

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/**
 * 3D Vector for spatial window positioning
 *
 * Represents a point or direction in 3D space using meters as the unit.
 * Used for window positioning in AR/spatial environments.
 *
 * **Coordinate System:**
 * - X axis: Left (-) to Right (+)
 * - Y axis: Down (-) to Up (+)
 * - Z axis: Forward (-) to Back (+)
 *
 * **Example Positions:**
 * - Vector3D(0f, 0f, -2f) → Center, 2 meters in front of user
 * - Vector3D(-0.5f, 0f, -2f) → 50cm to the left, 2 meters front
 * - Vector3D(0f, 0.3f, -2f) → Center, 30cm up, 2 meters front
 *
 * **Voice Commands:**
 * - "Move window left" → position + Vector3D(-0.1f, 0f, 0f)
 * - "Move window forward" → position + Vector3D(0f, 0f, -0.1f)
 * - "Move window up" → position + Vector3D(0f, 0.1f, 0f)
 *
 * @property x Horizontal position (left/right) in meters
 * @property y Vertical position (up/down) in meters
 * @property z Depth position (forward/back) in meters
 */
@Serializable
data class Vector3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    companion object {
        /** Origin point (0, 0, 0) */
        val ZERO = Vector3D(0f, 0f, 0f)

        /** Default window position (center, 2m in front) */
        val DEFAULT = Vector3D(0f, 0f, -2f)

        /** Unit vector pointing right */
        val RIGHT = Vector3D(1f, 0f, 0f)

        /** Unit vector pointing left */
        val LEFT = Vector3D(-1f, 0f, 0f)

        /** Unit vector pointing up */
        val UP = Vector3D(0f, 1f, 0f)

        /** Unit vector pointing down */
        val DOWN = Vector3D(0f, -1f, 0f)

        /** Unit vector pointing forward (towards user) */
        val FORWARD = Vector3D(0f, 0f, -1f)

        /** Unit vector pointing back (away from user) */
        val BACK = Vector3D(0f, 0f, 1f)

        /**
         * Creates a vector from spherical coordinates
         * @param radius Distance from origin
         * @param theta Horizontal angle (radians)
         * @param phi Vertical angle (radians)
         */
        fun fromSpherical(radius: Float, theta: Float, phi: Float): Vector3D {
            val sinPhi = kotlin.math.sin(phi)
            val cosPhi = kotlin.math.cos(phi)
            val sinTheta = kotlin.math.sin(theta)
            val cosTheta = kotlin.math.cos(theta)

            return Vector3D(
                x = radius * cosPhi * sinTheta,
                y = radius * sinPhi,
                z = radius * cosPhi * cosTheta
            )
        }
    }

    // ==================== Vector Math Operations ====================

    /**
     * Adds two vectors
     * Voice: "Move window to the right" → position + RIGHT * 0.1f
     */
    operator fun plus(other: Vector3D) = Vector3D(
        x + other.x,
        y + other.y,
        z + other.z
    )

    /**
     * Subtracts two vectors
     */
    operator fun minus(other: Vector3D) = Vector3D(
        x - other.x,
        y - other.y,
        z - other.z
    )

    /**
     * Multiplies vector by scalar
     * Voice: "Move window way left" → position + LEFT * 0.5f
     */
    operator fun times(scalar: Float) = Vector3D(
        x * scalar,
        y * scalar,
        z * scalar
    )

    /**
     * Divides vector by scalar
     */
    operator fun div(scalar: Float) = Vector3D(
        x / scalar,
        y / scalar,
        z / scalar
    )

    /**
     * Negates vector
     */
    operator fun unaryMinus() = Vector3D(-x, -y, -z)

    /**
     * Dot product with another vector
     * Used for angle calculations and projections
     */
    fun dot(other: Vector3D): Float {
        return x * other.x + y * other.y + z * other.z
    }

    /**
     * Cross product with another vector
     * Used for calculating perpendicular vectors
     */
    fun cross(other: Vector3D): Vector3D {
        return Vector3D(
            x = y * other.z - z * other.y,
            y = z * other.x - x * other.z,
            z = x * other.y - y * other.x
        )
    }

    // ==================== Geometric Properties ====================

    /**
     * Calculates length (magnitude) of the vector
     */
    fun length(): Float {
        return sqrt(x * x + y * y + z * z)
    }

    /**
     * Calculates squared length (faster than length())
     * Useful for distance comparisons without sqrt
     */
    fun lengthSquared(): Float {
        return x * x + y * y + z * z
    }

    /**
     * Returns normalized vector (length = 1)
     * Used for direction vectors
     */
    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0f) this / len else ZERO
    }

    /**
     * Calculates distance to another vector
     * Voice: "How far is the email window?" → camera.distance(emailWindow.position)
     */
    fun distance(other: Vector3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Calculates squared distance (faster than distance())
     */
    fun distanceSquared(other: Vector3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return dx * dx + dy * dy + dz * dz
    }

    /**
     * Calculates angle to another vector (in radians)
     */
    fun angleTo(other: Vector3D): Float {
        val cosAngle = dot(other) / (length() * other.length())
        return kotlin.math.acos(cosAngle.coerceIn(-1f, 1f))
    }

    // ==================== Spatial Queries ====================

    /**
     * Checks if this position is to the left of another
     * Used for voice commands: "Is the email window to the left?"
     */
    fun isLeftOf(other: Vector3D): Boolean = x < other.x

    /**
     * Checks if this position is to the right of another
     */
    fun isRightOf(other: Vector3D): Boolean = x > other.x

    /**
     * Checks if this position is above another
     */
    fun isAbove(other: Vector3D): Boolean = y > other.y

    /**
     * Checks if this position is below another
     */
    fun isBelow(other: Vector3D): Boolean = y < other.y

    /**
     * Checks if this position is in front of another (closer to user)
     */
    fun isInFrontOf(other: Vector3D): Boolean = z > other.z

    /**
     * Checks if this position is behind another (farther from user)
     */
    fun isBehind(other: Vector3D): Boolean = z < other.z

    /**
     * Checks if this position is within a certain distance of another
     * Used for proximity detection
     */
    fun isNear(other: Vector3D, threshold: Float): Boolean {
        return distanceSquared(other) <= threshold * threshold
    }

    // ==================== Interpolation ====================

    /**
     * Linear interpolation between this and another vector
     * @param other Target vector
     * @param t Interpolation factor (0.0 to 1.0)
     * @return Interpolated vector
     */
    fun lerp(other: Vector3D, t: Float): Vector3D {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector3D(
            x = x + (other.x - x) * clampedT,
            y = y + (other.y - y) * clampedT,
            z = z + (other.z - z) * clampedT
        )
    }

    /**
     * Moves this vector towards another by a maximum distance
     * @param target Target position
     * @param maxDistance Maximum distance to move
     * @return New position
     */
    fun moveTowards(target: Vector3D, maxDistance: Float): Vector3D {
        val direction = target - this
        val distance = direction.length()

        return if (distance <= maxDistance) {
            target
        } else {
            this + direction.normalized() * maxDistance
        }
    }

    // ==================== Utility ====================

    /**
     * Clamps each component to a range
     * Used to keep windows within valid bounds
     */
    fun clamp(min: Vector3D, max: Vector3D): Vector3D {
        return Vector3D(
            x = x.coerceIn(min.x, max.x),
            y = y.coerceIn(min.y, max.y),
            z = z.coerceIn(min.z, max.z)
        )
    }

    /**
     * Returns absolute value of each component
     */
    fun abs(): Vector3D {
        return Vector3D(
            x = kotlin.math.abs(x),
            y = kotlin.math.abs(y),
            z = kotlin.math.abs(z)
        )
    }

    /**
     * Projects this vector onto another vector
     * Used for calculating position along a direction
     */
    fun project(onto: Vector3D): Vector3D {
        val scale = dot(onto) / onto.lengthSquared()
        return onto * scale
    }

    /**
     * Reflects this vector across a normal
     * Used for bounce effects or mirroring
     */
    fun reflect(normal: Vector3D): Vector3D {
        val normalNorm = normal.normalized()
        return this - normalNorm * (2f * dot(normalNorm))
    }

    /**
     * Generates voice-friendly description of this position
     *
     * Example outputs:
     * - "center front"
     * - "left near"
     * - "right far up"
     */
    fun toVoiceDescription(): String {
        val parts = mutableListOf<String>()

        // Horizontal
        when {
            x < -0.3f -> parts.add("left")
            x > 0.3f -> parts.add("right")
            else -> parts.add("center")
        }

        // Depth
        when {
            z < -2.5f -> parts.add("far")
            z > -1.5f -> parts.add("near")
            else -> parts.add("front")
        }

        // Vertical (optional, only if significant)
        when {
            y > 0.3f -> parts.add("up")
            y < -0.3f -> parts.add("down")
        }

        return parts.joinToString(" ")
    }

    /**
     * Returns a copy with modified X coordinate
     * Voice: "Move window to X position"
     */
    fun withX(newX: Float) = copy(x = newX)

    /**
     * Returns a copy with modified Y coordinate
     * Voice: "Move window to Y position"
     */
    fun withY(newY: Float) = copy(y = newY)

    /**
     * Returns a copy with modified Z coordinate
     * Voice: "Move window to Z position"
     */
    fun withZ(newZ: Float) = copy(z = newZ)

    /**
     * String representation for debugging
     */
    override fun toString(): String {
        return "Vector3D(x=%.2f, y=%.2f, z=%.2f)".format(x, y, z)
    }
}
