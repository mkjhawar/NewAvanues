// Author: Manoj Jhawar
// Purpose: Mathematical utilities for IMU data processing

package com.augmentalis.devicemanager.math

import kotlin.math.*

/**
 * 3D Vector class for IMU calculations
 */
data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    companion object {
        val zero = Vector3(0f, 0f, 0f)
        val up = Vector3(0f, 1f, 0f)
        val forward = Vector3(0f, 0f, 1f)
        val right = Vector3(1f, 0f, 0f)
    }
    
    val magnitude: Float
        get() = sqrt(x * x + y * y + z * z)
    
    val normalized: Vector3
        get() {
            val mag = magnitude
            return if (mag > 0) Vector3(x / mag, y / mag, z / mag) else zero
        }
    
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
    
    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z
    
    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
    
    fun distanceTo(other: Vector3): Float = (this - other).magnitude
}

/**
 * Quaternion class for rotation representation
 */
data class Quaternion(
    val w: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    companion object {
        val identity = Quaternion(1f, 0f, 0f, 0f)
        
        fun fromRotationVector(rotationVector: FloatArray): Quaternion {
            if (rotationVector.size < 3) return identity
            
            return if (rotationVector.size >= 4) {
                // Use provided w component
                Quaternion(rotationVector[3], rotationVector[0], rotationVector[1], rotationVector[2])
            } else {
                // Calculate w component
                val x = rotationVector[0]
                val y = rotationVector[1] 
                val z = rotationVector[2]
                val w = sqrt(max(0f, 1f - x*x - y*y - z*z))
                Quaternion(w, x, y, z)
            }
        }
        
        fun fromAxisAngle(axis: Vector3, angle: Float): Quaternion {
            val halfAngle = angle * 0.5f
            val sinHalf = sin(halfAngle)
            val cosHalf = cos(halfAngle)
            val normalizedAxis = axis.normalized
            
            return Quaternion(
                cosHalf,
                normalizedAxis.x * sinHalf,
                normalizedAxis.y * sinHalf,
                normalizedAxis.z * sinHalf
            )
        }
        
        fun fromAxisAngle(axisAngle: Vector3): Quaternion {
            val angle = axisAngle.magnitude
            return if (angle > 0) {
                fromAxisAngle(axisAngle / angle, angle)
            } else {
                identity
            }
        }
        
        fun fromEulerAngles(euler: EulerAngles): Quaternion {
            val yaw = euler.yaw * 0.5f
            val pitch = euler.pitch * 0.5f
            val roll = euler.roll * 0.5f
            
            val cy = cos(yaw)
            val sy = sin(yaw)
            val cp = cos(pitch)
            val sp = sin(pitch)
            val cr = cos(roll)
            val sr = sin(roll)
            
            return Quaternion(
                cy * cp * cr + sy * sp * sr,
                cy * cp * sr - sy * sp * cr,
                sy * cp * sr + cy * sp * cr,
                sy * cp * cr - cy * sp * sr
            )
        }
    }
    
    val magnitude: Float
        get() = sqrt(w * w + x * x + y * y + z * z)
    
    val normalized: Quaternion
        get() {
            val mag = magnitude
            return if (mag > 0) Quaternion(w / mag, x / mag, y / mag, z / mag) else identity
        }
    
    val conjugate: Quaternion
        get() = Quaternion(w, -x, -y, -z)
    
    val inverse: Quaternion
        get() {
            val magSq = w * w + x * x + y * y + z * z
            return if (magSq > 0) {
                val invMagSq = 1f / magSq
                Quaternion(w * invMagSq, -x * invMagSq, -y * invMagSq, -z * invMagSq)
            } else {
                identity
            }
        }
    
    operator fun times(other: Quaternion): Quaternion {
        return Quaternion(
            w * other.w - x * other.x - y * other.y - z * other.z,
            w * other.x + x * other.w + y * other.z - z * other.y,
            w * other.y - x * other.z + y * other.w + z * other.x,
            w * other.z + x * other.y - y * other.x + z * other.w
        )
    }
    
    operator fun times(vector: Vector3): Vector3 {
        val qvec = Vector3(x, y, z)
        val uv = qvec.cross(vector)
        val uuv = qvec.cross(uv)
        return vector + (uv * (2f * w)) + (uuv * 2f)
    }
    
    fun dot(other: Quaternion): Float = w * other.w + x * other.x + y * other.y + z * other.z
    
    fun getAngle(): Float = 2f * acos(abs(w).coerceAtMost(1f))
    
    fun getAxis(): Vector3 {
        val sinHalfAngle = sqrt(1f - w * w)
        return if (sinHalfAngle > 0.001f) {
            Vector3(x / sinHalfAngle, y / sinHalfAngle, z / sinHalfAngle)
        } else {
            Vector3.up
        }
    }
    
    fun toEulerAngles(): EulerAngles {
        // Roll (x-axis rotation)
        val sinr_cosp = 2 * (w * x + y * z)
        val cosr_cosp = 1 - 2 * (x * x + y * y)
        val roll = atan2(sinr_cosp, cosr_cosp)
        
        // Pitch (y-axis rotation)
        val sinp = 2 * (w * y - z * x)
        val pitch = if (abs(sinp) >= 1) {
            if (sinp >= 0) PI.toFloat() / 2 else -PI.toFloat() / 2
        } else {
            asin(sinp)
        }
        
        // Yaw (z-axis rotation)
        val siny_cosp = 2 * (w * z + x * y)
        val cosy_cosp = 1 - 2 * (y * y + z * z)
        val yaw = atan2(siny_cosp, cosy_cosp)
        
        return EulerAngles(yaw, pitch, roll)
    }
    
    fun toRotationMatrix(): FloatArray {
        val matrix = FloatArray(9)
        val n = normalized
        
        val xx = n.x * n.x
        val yy = n.y * n.y
        val zz = n.z * n.z
        val xy = n.x * n.y
        val xz = n.x * n.z
        val yz = n.y * n.z
        val wx = n.w * n.x
        val wy = n.w * n.y
        val wz = n.w * n.z
        
        matrix[0] = 1f - 2f * (yy + zz)
        matrix[1] = 2f * (xy - wz)
        matrix[2] = 2f * (xz + wy)
        
        matrix[3] = 2f * (xy + wz)
        matrix[4] = 1f - 2f * (xx + zz)
        matrix[5] = 2f * (yz - wx)
        
        matrix[6] = 2f * (xz - wy)
        matrix[7] = 2f * (yz + wx)
        matrix[8] = 1f - 2f * (xx + yy)
        
        return matrix
    }
}

/**
 * Euler angles representation
 */
data class EulerAngles(
    val yaw: Float = 0f,   // Rotation around Z-axis
    val pitch: Float = 0f, // Rotation around Y-axis  
    val roll: Float = 0f   // Rotation around X-axis
) {
    companion object {
        val zero = EulerAngles(0f, 0f, 0f)
    }
    
    fun toDegrees(): EulerAngles = EulerAngles(
        (yaw * 180f / PI.toFloat()),
        (pitch * 180f / PI.toFloat()),
        (roll * 180f / PI.toFloat())
    )

    fun toRadians(): EulerAngles = EulerAngles(
        (yaw * PI.toFloat() / 180f),
        (pitch * PI.toFloat() / 180f),
        (roll * PI.toFloat() / 180f)
    )
}

/**
 * Spherical linear interpolation between quaternions
 */
fun slerp(a: Quaternion, b: Quaternion, t: Float): Quaternion {
    val clampedT = t.coerceIn(0f, 1f)
    
    var dot = a.dot(b)
    
    // If dot product is negative, slerp won't take the shorter path
    val b2 = if (dot < 0f) {
        dot = -dot
        Quaternion(-b.w, -b.x, -b.y, -b.z)
    } else {
        b
    }
    
    return if (dot > 0.9995f) {
        // Quaternions are very close, use linear interpolation
        val result = Quaternion(
            a.w + clampedT * (b2.w - a.w),
            a.x + clampedT * (b2.x - a.x),
            a.y + clampedT * (b2.y - a.y),
            a.z + clampedT * (b2.z - a.z)
        )
        result.normalized
    } else {
        val theta0 = acos(dot)
        val theta = theta0 * clampedT
        val sinTheta0 = sin(theta0)
        val sinTheta = sin(theta)
        
        val s0 = cos(theta) - dot * sinTheta / sinTheta0
        val s1 = sinTheta / sinTheta0
        
        Quaternion(
            s0 * a.w + s1 * b2.w,
            s0 * a.x + s1 * b2.x,
            s0 * a.y + s1 * b2.y,
            s0 * a.z + s1 * b2.z
        )
    }
}

/**
 * Linear interpolation between vectors
 */
fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 {
    val clampedT = t.coerceIn(0f, 1f)
    return a + (b - a) * clampedT
}

/**
 * Angular distance between two quaternions in radians
 */
fun angularDistance(a: Quaternion, b: Quaternion): Float {
    val dot = abs(a.dot(b))
    return 2f * acos(dot.coerceAtMost(1f))
}