package com.avanues.cockpit.core.workspace

/**
 * 3D vector for spatial positioning
 *
 * Coordinate system (right-handed):
 * - X: Left (-) to Right (+)
 * - Y: Down (-) to Up (+)
 * - Z: Forward (-) to Back (+)
 *
 * Units: meters
 */
data class Vector3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: Vector3D) = Vector3D(
        x + other.x,
        y + other.y,
        z + other.z
    )

    operator fun minus(other: Vector3D) = Vector3D(
        x - other.x,
        y - other.y,
        z - other.z
    )

    operator fun times(scalar: Float) = Vector3D(
        x * scalar,
        y * scalar,
        z * scalar
    )

    fun distance(other: Vector3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    companion object {
        val ZERO = Vector3D(0f, 0f, 0f)
        val FORWARD = Vector3D(0f, 0f, -1f)
        val UP = Vector3D(0f, 1f, 0f)
        val RIGHT = Vector3D(1f, 0f, 0f)
    }
}

/**
 * Quaternion for 3D rotation
 *
 * Used for window orientation in space
 */
data class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
) {
    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)

        fun fromEuler(pitch: Float, yaw: Float, roll: Float): Quaternion {
            val cy = kotlin.math.cos(yaw * 0.5f)
            val sy = kotlin.math.sin(yaw * 0.5f)
            val cp = kotlin.math.cos(pitch * 0.5f)
            val sp = kotlin.math.sin(pitch * 0.5f)
            val cr = kotlin.math.cos(roll * 0.5f)
            val sr = kotlin.math.sin(roll * 0.5f)

            return Quaternion(
                x = sr * cp * cy - cr * sp * sy,
                y = cr * sp * cy + sr * cp * sy,
                z = cr * cp * sy - sr * sp * cy,
                w = cr * cp * cy + sr * sp * sy
            )
        }
    }
}
