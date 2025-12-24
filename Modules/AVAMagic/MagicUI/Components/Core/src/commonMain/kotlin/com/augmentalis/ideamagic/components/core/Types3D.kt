package com.augmentalis.magicui.components.core

import kotlinx.serialization.Serializable

/**
 * 3D type definitions for advanced component features.
 *
 * Supports 3D transformations, camera positioning, and drag interactions.
 *
 * @since 1.1.0
 */

// ==================== 3D Vector ====================

/**
 * 3D vector for positions, directions, and transformations.
 */
@Serializable
data class Vector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)

    fun length() = kotlin.math.sqrt(x * x + y * y + z * z)
    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0) this / len else this
    }

    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z
    fun cross(other: Vector3) = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
}

// ==================== Camera 3D ====================

/**
 * 3D camera for positioning and viewing 3D scenes.
 *
 * @property position Camera position in 3D space (x, y, z)
 * @property target Point the camera is looking at (x, y, z)
 * @property up Up vector defining camera orientation (default: 0, 1, 0)
 * @property fov Field of view in degrees (default: 45)
 * @property aspect Aspect ratio width/height (default: 1.0)
 * @property near Near clipping plane (default: 0.1)
 * @property far Far clipping plane (default: 1000.0)
 * @since 1.1.0
 */
@Serializable
data class Camera3D(
    val position: Vector3 = Vector3(0f, 0f, 5f),
    val target: Vector3 = Vector3(0f, 0f, 0f),
    val up: Vector3 = Vector3(0f, 1f, 0f),
    val fov: Float = 45f,
    val aspect: Float = 1f,
    val near: Float = 0.1f,
    val far: Float = 1000f
) {
    init {
        require(fov > 0 && fov < 180) { "FOV must be between 0 and 180 degrees" }
        require(aspect > 0) { "Aspect ratio must be positive" }
        require(near > 0 && near < far) { "Near must be positive and less than far" }
    }

    fun moveTo(x: Float, y: Float, z: Float): Camera3D =
        copy(position = Vector3(x, y, z))

    fun lookAt(x: Float, y: Float, z: Float): Camera3D =
        copy(target = Vector3(x, y, z))

    fun zoom(factor: Float): Camera3D {
        val direction = (position - target).normalize() * factor
        return copy(position = target + direction)
    }

    fun getViewMatrix(): Transform3D {
        val z = (position - target).normalize()
        val x = up.cross(z).normalize()
        val y = z.cross(x)

        val matrix = floatArrayOf(
            x.x, y.x, z.x, 0f,
            x.y, y.y, z.y, 0f,
            x.z, y.z, z.z, 0f,
            -x.dot(position), -y.dot(position), -z.dot(position), 1f
        )
        return Transform3D(matrix)
    }

    fun getProjectionMatrix(): Transform3D =
        Transform3D.identity().perspective(fov, aspect, near, far)
}

// ==================== Transform 3D ====================

/**
 * 3D transformation matrix for OpenGL/WebGL rendering.
 *
 * Represents a 4x4 transformation matrix for 3D space operations including
 * translation, rotation, scaling, and perspective transformations.
 *
 * @property matrix 4x4 matrix in column-major order (OpenGL standard)
 * @since 1.1.0
 */
@Serializable
data class Transform3D(
    val matrix: FloatArray = identityMatrix()
) {
    init {
        require(matrix.size == 16) { "Matrix must have exactly 16 elements" }
    }

    fun translate(x: Float, y: Float, z: Float): Transform3D {
        val translation = translationMatrix(x, y, z)
        return Transform3D(multiplyMatrices(matrix, translation))
    }

    fun rotateX(degrees: Float): Transform3D {
        val rotation = rotationXMatrix(Math.toRadians(degrees.toDouble()).toFloat())
        return Transform3D(multiplyMatrices(matrix, rotation))
    }

    fun rotateY(degrees: Float): Transform3D {
        val rotation = rotationYMatrix(Math.toRadians(degrees.toDouble()).toFloat())
        return Transform3D(multiplyMatrices(matrix, rotation))
    }

    fun rotateZ(degrees: Float): Transform3D {
        val rotation = rotationZMatrix(Math.toRadians(degrees.toDouble()).toFloat())
        return Transform3D(multiplyMatrices(matrix, rotation))
    }

    fun scale(x: Float, y: Float, z: Float): Transform3D {
        val scaling = scaleMatrix(x, y, z)
        return Transform3D(multiplyMatrices(matrix, scaling))
    }

    fun perspective(fov: Float, aspect: Float, near: Float, far: Float): Transform3D {
        val persp = perspectiveMatrix(fov, aspect, near, far)
        return Transform3D(multiplyMatrices(matrix, persp))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transform3D) return false
        return matrix.contentEquals(other.matrix)
    }

    override fun hashCode(): Int = matrix.contentHashCode()

    companion object {
        fun identity() = Transform3D()

        private fun identityMatrix() = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        private fun translationMatrix(x: Float, y: Float, z: Float) = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            x, y, z, 1f
        )

        private fun rotationXMatrix(radians: Float): FloatArray {
            val cos = kotlin.math.cos(radians)
            val sin = kotlin.math.sin(radians)
            return floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, cos, sin, 0f,
                0f, -sin, cos, 0f,
                0f, 0f, 0f, 1f
            )
        }

        private fun rotationYMatrix(radians: Float): FloatArray {
            val cos = kotlin.math.cos(radians)
            val sin = kotlin.math.sin(radians)
            return floatArrayOf(
                cos, 0f, -sin, 0f,
                0f, 1f, 0f, 0f,
                sin, 0f, cos, 0f,
                0f, 0f, 0f, 1f
            )
        }

        private fun rotationZMatrix(radians: Float): FloatArray {
            val cos = kotlin.math.cos(radians)
            val sin = kotlin.math.sin(radians)
            return floatArrayOf(
                cos, sin, 0f, 0f,
                -sin, cos, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
        }

        private fun scaleMatrix(x: Float, y: Float, z: Float) = floatArrayOf(
            x, 0f, 0f, 0f,
            0f, y, 0f, 0f,
            0f, 0f, z, 0f,
            0f, 0f, 0f, 1f
        )

        private fun perspectiveMatrix(fov: Float, aspect: Float, near: Float, far: Float): FloatArray {
            val f = 1f / kotlin.math.tan(Math.toRadians(fov.toDouble() / 2.0).toFloat())
            val rangeInv = 1f / (near - far)
            return floatArrayOf(
                f / aspect, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, (near + far) * rangeInv, -1f,
                0f, 0f, near * far * rangeInv * 2f, 0f
            )
        }

        private fun multiplyMatrices(a: FloatArray, b: FloatArray): FloatArray {
            val result = FloatArray(16)
            for (row in 0..3) {
                for (col in 0..3) {
                    var sum = 0f
                    for (i in 0..3) {
                        sum += a[row + i * 4] * b[i + col * 4]
                    }
                    result[row + col * 4] = sum
                }
            }
            return result
        }
    }
}

// ==================== Drag Event ====================

/**
 * Represents a drag event with position and delta information.
 *
 * Used with the [Draggable] modifier to track drag gestures on components.
 * All coordinates are in density-independent pixels (dp/pt).
 *
 * @property x Current X coordinate (absolute position)
 * @property y Current Y coordinate (absolute position)
 * @property deltaX Change in X since last event
 * @property deltaY Change in Y since last event
 * @since 1.0.0
 */
@Serializable
data class DragEvent(
    val x: Float,
    val y: Float,
    val deltaX: Float,
    val deltaY: Float
) {
    /**
     * Checks if this is the start of a drag (no delta).
     *
     * @return true if deltaX and deltaY are both 0
     */
    fun isStart(): Boolean = deltaX == 0f && deltaY == 0f

    /**
     * Gets the total distance moved (Euclidean distance).
     *
     * @return Magnitude of the delta vector
     */
    val distance: Float
        get() = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

    /**
     * Gets the direction of movement in degrees (0-360).
     *
     * - 0° = right
     * - 90° = down
     * - 180° = left
     * - 270° = up
     *
     * @return Angle in degrees, or null if no movement
     */
    val angle: Float?
        get() = if (isStart()) null else {
            val radians = kotlin.math.atan2(deltaY.toDouble(), deltaX.toDouble())
            ((radians * 180 / kotlin.math.PI).toFloat() + 360f) % 360f
        }
}
