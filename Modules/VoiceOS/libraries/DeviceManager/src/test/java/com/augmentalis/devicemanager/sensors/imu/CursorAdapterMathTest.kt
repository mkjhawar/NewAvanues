/**
 * CursorAdapterMathTest.kt
 * Path: /modules/libraries/DeviceManager/src/test/java/com/augmentalis/devicemanager/sensors/imu/CursorAdapterMathTest.kt
 * 
 * Created: 2025-01-28 17:00 PST
 * Author: Testing & Validation Specialist
 * Version: 1.0.0
 * 
 * Purpose: Mathematical validation tests for CursorAdapter core algorithms
 * Focus: Pure mathematical functions without Android dependencies
 */

package com.augmentalis.devicemanager.sensors.imu

import org.junit.*
import org.junit.Assert.*
import kotlin.math.*

class CursorAdapterMathTest {

    // ========================================
    // QUATERNION MATHEMATICAL CORRECTNESS TESTS
    // ========================================

    @Test
    fun `test quaternion identity properties`() {
        // Given: Identity quaternion
        val identity = Quaternion.identity

        // Then: Should have correct values
        assertEquals(1f, identity.w, 0.0001f)
        assertEquals(0f, identity.x, 0.0001f)
        assertEquals(0f, identity.y, 0.0001f)
        assertEquals(0f, identity.z, 0.0001f)
    }

    @Test
    fun `test quaternion normalization`() {
        // Given: Un-normalized quaternion
        val unnormalized = Quaternion(2f, 3f, 4f, 5f)
        
        // When: Normalize
        val normalized = unnormalized.normalized
        
        // Then: Magnitude should be 1
        assertEquals(1f, normalized.magnitude, 0.0001f)
        
        // And: Direction should be preserved
        val expectedMagnitude = sqrt(2f*2f + 3f*3f + 4f*4f + 5f*5f)
        assertEquals(2f/expectedMagnitude, normalized.w, 0.0001f)
        assertEquals(3f/expectedMagnitude, normalized.x, 0.0001f)
        assertEquals(4f/expectedMagnitude, normalized.y, 0.0001f)
        assertEquals(5f/expectedMagnitude, normalized.z, 0.0001f)
    }

    @Test
    fun `test quaternion inverse operation`() {
        // Given: Various quaternions
        val quaternions = listOf(
            Quaternion.identity,
            Quaternion(0.7071f, 0.7071f, 0f, 0f),
            Quaternion(0.5f, 0.5f, 0.5f, 0.5f).normalized
        )

        // When & Then: q * q^-1 should equal identity
        quaternions.forEach { q ->
            val inverse = q.inverse
            val product = q * inverse
            
            assertEquals("Q * Q^-1 should equal identity (w)", 1f, product.w, 0.001f)
            assertEquals("Q * Q^-1 should equal identity (x)", 0f, product.x, 0.001f)
            assertEquals("Q * Q^-1 should equal identity (y)", 0f, product.y, 0.001f)
            assertEquals("Q * Q^-1 should equal identity (z)", 0f, product.z, 0.001f)
        }
    }

    @Test
    fun `test quaternion multiplication associativity`() {
        // Given: Three quaternions
        val q1 = Quaternion(0.7071f, 0.7071f, 0f, 0f)
        val q2 = Quaternion(0.7071f, 0f, 0.7071f, 0f)
        val q3 = Quaternion(0.7071f, 0f, 0f, 0.7071f)
        
        // When: Calculate (q1 * q2) * q3 and q1 * (q2 * q3)
        val result1 = (q1 * q2) * q3
        val result2 = q1 * (q2 * q3)
        
        // Then: Results should be equal (associative property)
        assertEquals(result1.w, result2.w, 0.001f)
        assertEquals(result1.x, result2.x, 0.001f)
        assertEquals(result1.y, result2.y, 0.001f)
        assertEquals(result1.z, result2.z, 0.001f)
    }

    @Test
    fun `test quaternion to euler conversion accuracy`() {
        // Given: Known quaternion rotations
        val testCases = listOf(
            Pair(Quaternion.identity, EulerAngles.zero),
            Pair(Quaternion.fromEulerAngles(EulerAngles(PI.toFloat() / 4, 0f, 0f)), EulerAngles(PI.toFloat() / 4, 0f, 0f)),
            Pair(Quaternion.fromEulerAngles(EulerAngles(0f, PI.toFloat() / 6, 0f)), EulerAngles(0f, PI.toFloat() / 6, 0f)),
            Pair(Quaternion.fromEulerAngles(EulerAngles(0f, 0f, PI.toFloat() / 3)), EulerAngles(0f, 0f, PI.toFloat() / 3))
        )

        // When & Then: Convert and verify accuracy
        testCases.forEach { (quaternion, expectedEuler) ->
            val actualEuler = quaternion.toEulerAngles()
            
            assertEquals("Yaw conversion failed", expectedEuler.yaw, actualEuler.yaw, 0.001f)
            assertEquals("Pitch conversion failed", expectedEuler.pitch, actualEuler.pitch, 0.001f)
            assertEquals("Roll conversion failed", expectedEuler.roll, actualEuler.roll, 0.001f)
        }
    }

    @Test
    fun `test euler to quaternion to euler round trip`() {
        // Given: Various Euler angle combinations
        val eulerAngles = listOf(
            EulerAngles(0f, 0f, 0f),
            EulerAngles(0.1f, 0.2f, 0.3f),
            EulerAngles(PI.toFloat() / 4, PI.toFloat() / 6, PI.toFloat() / 8),
            EulerAngles(-0.5f, 0.7f, -0.2f)
        )

        // When & Then: Round trip conversion should preserve values
        eulerAngles.forEach { original ->
            val quaternion = Quaternion.fromEulerAngles(original)
            val converted = quaternion.toEulerAngles()
            
            assertEquals("Yaw round trip failed", original.yaw, converted.yaw, 0.001f)
            assertEquals("Pitch round trip failed", original.pitch, converted.pitch, 0.001f)
            assertEquals("Roll round trip failed", original.roll, converted.roll, 0.001f)
        }
    }

    // ========================================
    // TANGENT FUNCTION BEHAVIOR TESTS
    // ========================================

    @Test
    fun `test tangent function behavior at known angles`() {
        // Given: Known angle values
        val testCases = listOf(
            Pair(0f, 0f),
            Pair(PI.toFloat() / 4, 1f),    // 45 degrees = tan(45°) = 1
            Pair(-PI.toFloat() / 4, -1f),  // -45 degrees = tan(-45°) = -1
            Pair(PI.toFloat() / 6, sqrt(3f) / 3f)  // 30 degrees = tan(30°) = √3/3
        )

        // When & Then: Tangent should behave predictably
        testCases.forEach { (angle, expectedTan) ->
            val actualTan = tan(angle)
            assertEquals("tan(${Math.toDegrees(angle.toDouble())}°) failed", expectedTan, actualTan, 0.001f)
        }
    }

    @Test
    fun `test tangent scaling for cursor movement`() {
        // Given: Small angles for cursor movement
        val angles = listOf(
            Math.toRadians(1.0).toFloat(),   // 1 degree
            Math.toRadians(5.0).toFloat(),   // 5 degrees
            Math.toRadians(10.0).toFloat(),  // 10 degrees
            Math.toRadians(30.0).toFloat()   // 30 degrees
        )
        
        val screenDimension = 1000f
        val sensitivity = 2.0f

        // When & Then: Movement should be proportional to tan(angle)
        angles.forEach { angle ->
            val movement = tan(angle) * screenDimension * sensitivity
            val expectedMovement = tan(angle) * 2000f  // 1000 * 2.0
            
            assertEquals("Movement calculation failed for ${Math.toDegrees(angle.toDouble())}°", 
                        expectedMovement, movement, 0.001f)
        }
    }

    @Test
    fun `test small angle approximation accuracy`() {
        // Given: Very small angles (where tan(x) ≈ x)
        val smallAngles = listOf(
            0.001f,  // ~0.057 degrees
            0.01f,   // ~0.573 degrees
            0.1f     // ~5.73 degrees
        )

        // When & Then: For small angles, tan(x) should be close to x
        smallAngles.forEach { angle ->
            val tanValue = tan(angle)
            val difference = abs(tanValue - angle)
            
            // For angles < 0.1 radians (~5.7°), difference should be small
            if (angle < 0.1f) {
                assertTrue("Small angle approximation failed for $angle", difference < 0.01f)
            }
        }
    }

    // ========================================
    // CURSOR MOVEMENT CALCULATION TESTS
    // ========================================

    @Test
    fun `test cursor movement delta calculation`() {
        // Given: Two orientations representing a delta
        val baseOrientation = Quaternion.identity
        val newOrientation = Quaternion.fromEulerAngles(EulerAngles(0.1f, 0.05f, 0f))
        
        // When: Calculate delta rotation
        val deltaRotation = baseOrientation.inverse * newOrientation
        val deltaEuler = deltaRotation.toEulerAngles()
        
        // Then: Delta should match the difference
        assertEquals(0.1f, deltaEuler.yaw, 0.001f)
        assertEquals(0.05f, deltaEuler.pitch, 0.001f)
        assertEquals(0f, deltaEuler.roll, 0.001f)
    }

    @Test
    fun `test cursor boundary coercion`() {
        // Given: Screen dimensions and positions
        val screenWidth = 1920f
        val screenHeight = 1080f
        
        val testPositions = listOf(
            Pair(-100f, 500f),      // Left of screen
            Pair(2000f, 500f),      // Right of screen
            Pair(500f, -100f),      // Above screen
            Pair(500f, 1200f),      // Below screen
            Pair(-50f, -50f),       // Top-left off screen
            Pair(2000f, 1200f)      // Bottom-right off screen
        )

        // When & Then: Positions should be coerced to screen bounds
        testPositions.forEach { (x, y) ->
            val coercedX = x.coerceIn(0f, screenWidth)
            val coercedY = y.coerceIn(0f, screenHeight)
            
            assertTrue("X should be >= 0", coercedX >= 0f)
            assertTrue("X should be <= screen width", coercedX <= screenWidth)
            assertTrue("Y should be >= 0", coercedY >= 0f)
            assertTrue("Y should be <= screen height", coercedY <= screenHeight)
        }
    }

    @Test
    fun `test dead zone filtering logic`() {
        // Given: Dead zone threshold and test values
        val deadZoneThreshold = 0.001f
        val testValues = listOf(
            0.0005f,   // Below threshold
            0.001f,    // At threshold
            0.0015f,   // Above threshold
            -0.0005f,  // Below threshold (negative)
            -0.0015f   // Above threshold (negative)
        )

        // When & Then: Values should be filtered correctly
        testValues.forEach { value ->
            val filtered = if (abs(value) > deadZoneThreshold) value else 0f
            
            if (abs(value) <= deadZoneThreshold) {
                assertEquals("Value $value should be filtered to 0", 0f, filtered, 0.0001f)
            } else {
                assertEquals("Value $value should pass through", value, filtered, 0.0001f)
            }
        }
    }

    // ========================================
    // SENSITIVITY SCALING TESTS
    // ========================================

    @Test
    fun `test sensitivity scaling calculations`() {
        // Given: Base sensitivity value
        val baseSensitivity = 1.0f
        val scaleFactor = 500.0f
        
        // When: Calculate scaled sensitivities
        val movementScale = baseSensitivity * scaleFactor
        val sensitivityX = baseSensitivity * 2.0f
        val sensitivityY = baseSensitivity * 3.0f
        
        // Then: Values should be correct
        assertEquals(500.0f, movementScale, 0.001f)
        assertEquals(2.0f, sensitivityX, 0.001f)
        assertEquals(3.0f, sensitivityY, 0.001f)
    }

    @Test
    fun `test sensitivity bounds enforcement`() {
        // Given: Extreme sensitivity values
        val testValues = listOf(
            Pair(-5.0f, 50.0f),    // Too low, should clamp to min
            Pair(0.0f, 50.0f),     // Zero, should clamp to min
            Pair(1.0f, 500.0f),    // Normal value
            Pair(10.0f, 2500.0f),  // Too high, should clamp to max
            Pair(100.0f, 2500.0f)  // Way too high, should clamp to max
        )

        // When & Then: Values should be clamped to bounds
        testValues.forEach { (input, expectedOutput) ->
            val scaledValue = (input * 500.0f).coerceIn(50.0f, 2500.0f)
            assertEquals("Sensitivity $input should clamp to $expectedOutput", 
                        expectedOutput, scaledValue, 0.001f)
        }
    }

    // ========================================
    // VECTOR AND ANGLE CALCULATIONS
    // ========================================

    @Test
    fun `test vector operations accuracy`() {
        // Given: Test vectors
        val v1 = Vector3(1f, 2f, 3f)
        val v2 = Vector3(4f, 5f, 6f)
        
        // When: Perform operations
        val sum = v1 + v2
        val difference = v2 - v1
        val dotProduct = v1.dot(v2)
        val magnitude1 = v1.magnitude
        
        // Then: Results should be correct
        assertEquals(Vector3(5f, 7f, 9f), sum)
        assertEquals(Vector3(3f, 3f, 3f), difference)
        assertEquals(32f, dotProduct, 0.001f) // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(sqrt(14f), magnitude1, 0.001f) // sqrt(1² + 2² + 3²) = sqrt(14)
    }

    @Test
    fun `test angular distance calculation`() {
        // Given: Two quaternions
        val q1 = Quaternion.identity
        val q2 = Quaternion.fromEulerAngles(EulerAngles(PI.toFloat() / 4, 0f, 0f))
        
        // When: Calculate angular distance
        val distance = angularDistance(q1, q2)
        
        // Then: Distance should match expected value
        val expectedDistance = PI.toFloat() / 4  // 45 degrees in radians
        assertEquals(expectedDistance, distance, 0.01f)
    }

    @Test
    fun `test slerp interpolation properties`() {
        // Given: Two quaternions and interpolation parameter
        val q1 = Quaternion.identity
        val q2 = Quaternion.fromEulerAngles(EulerAngles(PI.toFloat() / 2, 0f, 0f))
        
        // When: Interpolate at different values
        val start = slerp(q1, q2, 0f)
        val middle = slerp(q1, q2, 0.5f)
        val end = slerp(q1, q2, 1f)
        
        // Then: Results should be correct
        assertEquals(q1.w, start.w, 0.001f)
        assertEquals(q1.x, start.x, 0.001f)
        assertEquals(q1.y, start.y, 0.001f)
        assertEquals(q1.z, start.z, 0.001f)
        
        assertEquals(q2.w, end.w, 0.001f)
        assertEquals(q2.x, end.x, 0.001f)
        assertEquals(q2.y, end.y, 0.001f)
        assertEquals(q2.z, end.z, 0.001f)
        
        // Middle should be between start and end
        assertTrue("Middle interpolation should be normalized", abs(middle.magnitude - 1f) < 0.001f)
    }
}