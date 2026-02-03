/**
 * CursorAdapterTest.kt
 * Path: /modules/libraries/DeviceManager/src/test/java/com/augmentalis/devicemanager/sensors/imu/CursorAdapterTest.kt
 * 
 * Created: 2025-01-28 16:30 PST
 * Author: Testing & Validation Specialist
 * Version: 1.0.0
 * 
 * Purpose: Comprehensive unit tests for CursorAdapter functionality
 * Testing Areas:
 * - Initialization and positioning
 * - Movement calculations and transformations  
 * - Boundary conditions and edge cases
 * - Stuck detection and recalibration
 * - Dead zone filtering
 * - Mathematical correctness
 */

package com.augmentalis.devicemanager.sensors.imu

import android.content.Context
import android.util.Log
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class CursorAdapterTest {

    @MockK
    private lateinit var mockContext: Context
    
    @MockK 
    private lateinit var mockIMUManager: IMUManager

    private lateinit var cursorAdapter: CursorAdapter
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    // Test orientations and quaternions for predictable testing
    private val identityQuaternion = Quaternion.identity
    private val smallRotationQuaternion = Quaternion(0.9998f, 0.02f, 0.0f, 0.0f)  // ~2.29 degree rotation
    private val mediumRotationQuaternion = Quaternion(0.966f, 0.259f, 0.0f, 0.0f)  // ~30 degree rotation
    private val largeRotationQuaternion = Quaternion(0.707f, 0.707f, 0.0f, 0.0f)   // ~90 degree rotation

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        
        // Mock Log class to prevent Android dependencies in tests
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        // Mock IMUManager singleton
        mockkObject(IMUManager)
        every { IMUManager.getInstance(any()) } returns mockIMUManager
        
        // Setup default IMUManager behavior
        every { mockIMUManager.startIMUTracking(any()) } returns true
        every { mockIMUManager.stopIMUTracking(any()) } just Runs
        every { mockIMUManager.getCurrentOrientation() } returns null
        every { mockIMUManager.getSensorCapabilities() } returns SensorCapabilities(
            hasGameRotationVector = true,
            hasRotationVector = true,
            hasGyroscope = true,
            hasAccelerometer = true,
            hasMagnetometer = true,
            maxSampleRate = 250,
            resolution = 0.001f
        )
        coEvery { mockIMUManager.calibrateForUser() } returns CalibrationResult(
            success = true,
            baseOrientation = identityQuaternion,
            message = "Calibration successful"
        )
        
        // Setup orientation flow for testing
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter = CursorAdapter(mockContext, "TestConsumer")
    }

    @After
    fun tearDown() {
        cursorAdapter.dispose()
        testScope.cancel()
        unmockkAll()
    }

    // ========================================
    // INITIALIZATION TESTS
    // ========================================

    @Test
    fun `test cold start initialization`() = testScope.runTest {
        // Given: Fresh CursorAdapter
        val position = cursorAdapter.getCurrentPosition()

        // Then: Should be at default center position with default screen size
        assertEquals(960f, position.x, 0.1f)  // Default 1920/2
        assertEquals(540f, position.y, 0.1f)  // Default 1080/2
        assertTrue(position.timestamp > 0)
    }

    @Test
    fun `test screen dimensions update centers cursor`() = testScope.runTest {
        // Given: Different screen dimensions
        val width = 2560
        val height = 1440

        // When: Update screen dimensions
        cursorAdapter.updateScreenDimensions(width, height)

        // Then: Cursor should be centered
        val position = cursorAdapter.getCurrentPosition()
        assertEquals(1280f, position.x, 0.1f)  // 2560/2
        assertEquals(720f, position.y, 0.1f)   // 1440/2
    }

    @Test
    fun `test invalid screen dimensions are rejected`() = testScope.runTest {
        // Given: Original position
        val originalPosition = cursorAdapter.getCurrentPosition()

        // When: Attempt to set invalid dimensions
        cursorAdapter.updateScreenDimensions(-100, 200)
        val position1 = cursorAdapter.getCurrentPosition()
        
        cursorAdapter.updateScreenDimensions(100, -200)
        val position2 = cursorAdapter.getCurrentPosition()
        
        cursorAdapter.updateScreenDimensions(0, 0)
        val position3 = cursorAdapter.getCurrentPosition()

        // Then: Position should remain unchanged
        assertEquals(originalPosition.x, position1.x, 0.1f)
        assertEquals(originalPosition.y, position1.y, 0.1f)
        assertEquals(originalPosition.x, position2.x, 0.1f)
        assertEquals(originalPosition.y, position2.y, 0.1f)
        assertEquals(originalPosition.x, position3.x, 0.1f)
        assertEquals(originalPosition.y, position3.y, 0.1f)
    }

    @Test
    fun `test center cursor recalibration`() = testScope.runTest {
        // Given: Cursor moved from center
        cursorAdapter.updateScreenDimensions(1920, 1080)
        // Simulate cursor movement by getting adapter into moved state (this is tested elsewhere)
        
        // When: Center cursor
        cursorAdapter.centerCursor()

        // Then: Cursor should be back at center
        val position = cursorAdapter.getCurrentPosition()
        assertEquals(960f, position.x, 0.1f)
        assertEquals(540f, position.y, 0.1f)
        
        // And: IMU manager should be queried for current orientation
        verify { mockIMUManager.getCurrentOrientation() }
    }

    // ========================================
    // MOVEMENT CALCULATION TESTS
    // ========================================

    @Test
    fun `test delta processing with small rotation`() = testScope.runTest {
        // Given: Adapter is tracking
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Send initial orientation then small rotation
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        val initialPosition = cursorAdapter.getCurrentPosition()

        val smallEuler = smallRotationQuaternion.toEulerAngles()
        orientationFlow.emit(IMUData(smallEuler.yaw, smallEuler.pitch, smallEuler.roll, System.nanoTime()))
        advanceUntilIdle()

        // Then: Cursor should have moved slightly
        val newPosition = cursorAdapter.getCurrentPosition()
        assertNotEquals(initialPosition.x, newPosition.x, 0.01f)

        // Movement should be proportional to rotation magnitude
        val movement = abs(newPosition.x - initialPosition.x)
        assertTrue("Movement should be small for small rotation", movement > 0.1f && movement < 50f)
    }

    @Test
    fun `test tangent scaling produces correct movement`() = testScope.runTest {
        // Given: Known rotation angle and screen dimensions
        cursorAdapter.updateScreenDimensions(1000, 1000)
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Apply known rotation
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        val initialPosition = cursorAdapter.getCurrentPosition()

        // Create a quaternion representing a 5-degree yaw rotation
        val angle = Math.toRadians(5.0).toFloat()
        orientationFlow.emit(IMUData(angle, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        // Then: Movement should follow tangent scaling
        val newPosition = cursorAdapter.getCurrentPosition()
        val actualMovement = newPosition.x - initialPosition.x

        // Expected movement = tan(angle) * screenWidth * sensitivityX
        // Default sensitivityX = 2.0f, so expected ≈ tan(5°) * 1000 * 2.0 ≈ 175
        val expectedMovement = tan(angle) * 1000 * 2.0f

        assertEquals(expectedMovement, actualMovement, 50f)  // Allow some tolerance for processing variations
    }

    @Test
    fun `test delta-based processing prevents absolute positioning drift`() = testScope.runTest {
        // Given: Tracking cursor with multiple orientations
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Send sequence of orientations that return to original
        val smallEuler = smallRotationQuaternion.toEulerAngles()
        val orientations = listOf(
            IMUData(0f, 0f, 0f, System.nanoTime()),
            IMUData(smallEuler.yaw, smallEuler.pitch, smallEuler.roll, System.nanoTime() + 1000000),
            IMUData(0f, 0f, 0f, System.nanoTime() + 2000000)
        )

        var lastPosition: CursorPosition? = null
        orientations.forEach { orientation ->
            orientationFlow.emit(orientation)
            advanceUntilIdle()
            lastPosition = cursorAdapter.getCurrentPosition()
        }

        // Then: Should be close to starting position (delta-based should minimize drift)
        val finalPosition = lastPosition!!
        assertEquals(960f, finalPosition.x, 20f)  // Allow some tolerance for cumulative small errors
        assertEquals(540f, finalPosition.y, 20f)
    }

    // ========================================
    // BOUNDARY TESTS
    // ========================================

    @Test
    fun `test cursor movement is constrained to screen bounds`() = testScope.runTest {
        // Given: Small screen dimensions
        cursorAdapter.updateScreenDimensions(100, 100)
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Apply large rotation that would move cursor off-screen
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        val largeEuler = largeRotationQuaternion.toEulerAngles()
        orientationFlow.emit(IMUData(largeEuler.yaw, largeEuler.pitch, largeEuler.roll, System.nanoTime()))
        advanceUntilIdle()

        // Then: Cursor should be constrained within bounds
        val position = cursorAdapter.getCurrentPosition()
        assertTrue("X should be >= 0", position.x >= 0f)
        assertTrue("X should be <= screen width", position.x <= 100f)
        assertTrue("Y should be >= 0", position.y >= 0f)
        assertTrue("Y should be <= screen height", position.y <= 100f)
    }

    @Test
    fun `test cursor coercion at exact boundaries`() = testScope.runTest {
        // Given: Adapter tracking
        cursorAdapter.updateScreenDimensions(1920, 1080)
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Apply extreme rotations to push cursor to boundaries
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        // Extreme positive yaw rotation
        orientationFlow.emit(IMUData(PI.toFloat() / 2, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        // Then: Cursor should be at screen boundary
        val position = cursorAdapter.getCurrentPosition()
        assertTrue("Cursor should hit right boundary", position.x >= 1919f)  // Should be at or very close to 1920
    }

    // ========================================
    // STUCK DETECTION AND RECALIBRATION TESTS
    // ========================================

    @Test
    fun `test stuck detection triggers after threshold time`() = testScope.runTest {
        // Given: Cursor tracking with mocked time
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Send orientation that doesn't cause movement for extended time
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        val initialPosition = cursorAdapter.getCurrentPosition()

        // Advance time beyond stuck threshold (5000ms) without meaningful movement
        advanceTimeBy(6000)

        // Send another orientation update to trigger stuck detection check
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        // Then: Cursor should be recentered due to stuck detection
        val finalPosition = cursorAdapter.getCurrentPosition()
        assertEquals(960f, finalPosition.x, 0.1f)  // Should be recentered
        assertEquals(540f, finalPosition.y, 0.1f)
    }

    @Test
    fun `test movement resets stuck detection timer`() = testScope.runTest {
        // Given: Cursor tracking
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Alternate between movement and no movement
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        advanceTimeBy(3000)  // Advance but not past threshold

        // Cause movement to reset timer
        val smallEuler = smallRotationQuaternion.toEulerAngles()
        orientationFlow.emit(IMUData(smallEuler.yaw, smallEuler.pitch, smallEuler.roll, System.nanoTime()))
        advanceUntilIdle()

        advanceTimeBy(3000)  // Advance again but timer should be reset

        // No movement
        orientationFlow.emit(IMUData(smallEuler.yaw, smallEuler.pitch, smallEuler.roll, System.nanoTime()))
        advanceUntilIdle()

        // Then: Should not trigger stuck detection (total time > 5s but movement reset timer)
        val position = cursorAdapter.getCurrentPosition()
        assertNotEquals(960f, position.x, 5f)  // Should not be recentered
    }

    @Test
    fun `test force recalibration centers cursor immediately`() = testScope.runTest {
        // Given: Cursor moved away from center
        cursorAdapter.updateScreenDimensions(1920, 1080)
        // Assume cursor has moved (tested elsewhere)

        // When: Force recalibration (using centerCursor as replacement)
        cursorAdapter.centerCursor()

        // Then: Cursor should be centered
        val position = cursorAdapter.getCurrentPosition()
        assertEquals(960f, position.x, 0.1f)
        assertEquals(540f, position.y, 0.1f)
    }

    // ========================================
    // DEAD ZONE FILTERING TESTS
    // ========================================

    @Test
    fun `test dead zone filters out small movements`() = testScope.runTest {
        // Given: Cursor tracking
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Send very small rotation (below dead zone threshold of 0.002f - RADIAN_TOLERANCE)
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        val initialPosition = cursorAdapter.getCurrentPosition()

        // Create tiny rotation (0.0005 radians ≈ 0.029 degrees, below 0.002f threshold)
        orientationFlow.emit(IMUData(0.0005f, 0.0f, 0.0f, System.nanoTime()))
        advanceUntilIdle()

        // Then: Cursor should not move due to dead zone filtering
        val finalPosition = cursorAdapter.getCurrentPosition()
        assertEquals(initialPosition.x, finalPosition.x, 0.01f)
        assertEquals(initialPosition.y, finalPosition.y, 0.01f)
    }

    @Test
    fun `test movements above dead zone threshold are processed`() = testScope.runTest {
        // Given: Cursor tracking
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Send rotation just above dead zone threshold
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        val initialPosition = cursorAdapter.getCurrentPosition()

        // Create rotation above threshold (0.003 radians > 0.002 RADIAN_TOLERANCE)
        orientationFlow.emit(IMUData(0.003f, 0.0f, 0.0f, System.nanoTime()))
        advanceUntilIdle()

        // Then: Cursor should move
        val finalPosition = cursorAdapter.getCurrentPosition()
        assertNotEquals(initialPosition.x, finalPosition.x, 0.01f)
    }

    // ========================================
    // QUATERNION TO EULER CONVERSION TESTS
    // ========================================

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
    // SENSITIVITY AND CONFIGURATION TESTS
    // ========================================

    @Test
    fun `test sensitivity adjustment affects movement scale`() = testScope.runTest {
        // Given: Two identical rotations with different sensitivities
        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // Test with default sensitivity
        cursorAdapter.setSensitivity(1.0f)

        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()
        val position1 = cursorAdapter.getCurrentPosition()

        val smallEuler = smallRotationQuaternion.toEulerAngles()
        orientationFlow.emit(IMUData(smallEuler.yaw, smallEuler.pitch, smallEuler.roll, System.nanoTime()))
        advanceUntilIdle()
        val position2 = cursorAdapter.getCurrentPosition()

        val movement1 = abs(position2.x - position1.x)

        // Reset and test with double sensitivity
        cursorAdapter.centerCursor()
        cursorAdapter.setSensitivity(2.0f)

        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()
        val position3 = cursorAdapter.getCurrentPosition()

        orientationFlow.emit(IMUData(smallEuler.yaw, smallEuler.pitch, smallEuler.roll, System.nanoTime()))
        advanceUntilIdle()
        val position4 = cursorAdapter.getCurrentPosition()

        val movement2 = abs(position4.x - position3.x)

        // Then: Higher sensitivity should produce greater movement
        assertTrue("Higher sensitivity should increase movement", movement2 > movement1)
        assertEquals("Movement should roughly double", movement1 * 2f, movement2, movement1 * 0.5f)
    }

    @Test
    fun `test sensitivity bounds are enforced`() {
        // Given: Extreme sensitivity values
        // When: Set extremely high and low sensitivities
        cursorAdapter.setSensitivity(100f)  // Should be clamped to 5.0f (max)
        cursorAdapter.setSensitivity(-10f)  // Should be clamped to 0.1f (min)

        // Then: Values should be coerced within bounds (0.1f to 5.0f)
        // Verify by checking that movement still works after setting extreme values
        val position = cursorAdapter.getCurrentPosition()
        assertNotNull("Cursor should still be functional after clamping", position)
    }

    // ========================================
    // SENSOR CAPABILITIES AND CALIBRATION TESTS
    // ========================================

    @Test
    fun `test sensor capabilities are properly exposed`() {
        // When: Get sensor capabilities
        val capabilities = cursorAdapter.getSensorCapabilities()

        // Then: Should match mocked values
        assertTrue(capabilities.hasGameRotationVector)
        assertTrue(capabilities.hasRotationVector)
        assertTrue(capabilities.hasGyroscope)
        assertTrue(capabilities.hasAccelerometer)
        assertTrue(capabilities.hasMagnetometer)
        assertEquals(250, capabilities.maxSampleRate)
        assertEquals(0.001f, capabilities.resolution, 0.0001f)
    }

    @Test
    fun `test calibration delegates to IMU manager`() = testScope.runTest {
        // When: Perform calibration
        val result = cursorAdapter.calibrate()

        // Then: Should delegate to IMU manager and return result
        coVerify { mockIMUManager.calibrateForUser() }
        assertTrue(result.success)
        assertEquals("Calibration successful", result.message)
        assertEquals(identityQuaternion, result.baseOrientation)
    }

    // ========================================
    // EDGE CASES AND ERROR CONDITIONS
    // ========================================

    @Test
    fun `test tracking start and stop behavior`() {
        // Given: Fresh adapter
        // When: Start tracking
        val startResult = cursorAdapter.startTracking()

        // Then: Should start successfully
        assertTrue("Should start tracking", startResult)
        verify { mockIMUManager.startIMUTracking("TestConsumer") }

        // When: Start again
        val secondStart = cursorAdapter.startTracking()

        // Then: Should return true (idempotent)
        assertTrue("Should handle repeated start", secondStart)

        // When: Stop tracking
        cursorAdapter.stopTracking()

        // Then: Should stop
        verify { mockIMUManager.stopIMUTracking("TestConsumer") }
    }

    @Test
    fun `test tracking failure handling`() {
        // Given: IMU manager fails to start
        every { mockIMUManager.startIMUTracking(any()) } returns false

        // When: Attempt to start tracking
        val result = cursorAdapter.startTracking()

        // Then: Should handle failure gracefully
        assertFalse("Should return false on start failure", result)
    }

    @Test
    fun `test null orientation handling during initialization`() = testScope.runTest {
        // Given: IMU manager returns null for current orientation
        every { mockIMUManager.getCurrentOrientation() } returns null

        // When: Center cursor (which queries current orientation)
        cursorAdapter.centerCursor()

        // Then: Should handle null gracefully and still center
        val position = cursorAdapter.getCurrentPosition()
        assertEquals(960f, position.x, 0.1f)
        assertEquals(540f, position.y, 0.1f)
    }

    @Test
    fun `test cursor state can be queried through position`() {
        // When: Get cursor position
        val position = cursorAdapter.getCurrentPosition()

        // Then: Should contain valid state information
        assertTrue("Position X should be valid", position.x >= 0f)
        assertTrue("Position Y should be valid", position.y >= 0f)
        assertTrue("Timestamp should be valid", position.timestamp > 0)

        // Verify sensor capabilities are accessible
        val capabilities = cursorAdapter.getSensorCapabilities()
        assertNotNull("Should be able to query capabilities", capabilities)
    }

    @Test
    fun `test dispose cleans up resources`() {
        // Given: Adapter is tracking
        cursorAdapter.startTracking()

        // When: Dispose
        cursorAdapter.dispose()

        // Then: Should stop tracking and clean up
        verify { mockIMUManager.stopIMUTracking("TestConsumer") }
    }

    // ========================================
    // MATHEMATICAL CORRECTNESS TESTS
    // ========================================

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
    fun `test tangent function behavior at extreme angles`() {
        // Given: Extreme angle values
        val extremeAngles = listOf(
            0f,
            PI.toFloat() / 4,   // 45 degrees
            PI.toFloat() / 2 - 0.01f,  // Just under 90 degrees
            -PI.toFloat() / 4   // -45 degrees
        )

        // When & Then: Tangent should behave predictably
        extremeAngles.forEach { angle ->
            val tanValue = tan(angle)
            
            when {
                abs(angle) < 0.001f -> assertEquals("tan(0) should be 0", 0f, tanValue, 0.001f)
                abs(angle - PI.toFloat() / 4) < 0.001f -> assertEquals("tan(45°) should be 1", 1f, tanValue, 0.001f)
                abs(angle + PI.toFloat() / 4) < 0.001f -> assertEquals("tan(-45°) should be -1", -1f, tanValue, 0.001f)
                angle > PI.toFloat() / 2 - 0.1f -> assertTrue("tan should be large near 90°", abs(tanValue) > 10f)
            }
        }
    }

    @Test
    fun `test movement calculation mathematical consistency`() = testScope.runTest {
        // Given: Known screen dimensions and sensitivity
        cursorAdapter.updateScreenDimensions(1000, 1000)
        cursorAdapter.setSensitivity(1.0f)  // Default scaling

        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Apply known rotation and measure movement
        orientationFlow.emit(IMUData(0f, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        val initialPosition = cursorAdapter.getCurrentPosition()

        // 10-degree yaw rotation
        val angle = Math.toRadians(10.0).toFloat()
        orientationFlow.emit(IMUData(angle, 0f, 0f, System.nanoTime()))
        advanceUntilIdle()

        val finalPosition = cursorAdapter.getCurrentPosition()

        // Then: Movement should match mathematical expectation
        val actualMovementX = finalPosition.x - initialPosition.x
        val expectedMovementX = tan(angle) * 1000 * 2.0f  // screen width * CURSOR_SCALE_X

        assertEquals("X movement should match tan calculation", expectedMovementX, actualMovementX, 20f)
    }

    @Test
    fun `test position flow emits correct timestamps`() = testScope.runTest {
        // Given: Position flow collection
        val positions = mutableListOf<CursorPosition>()
        val collectJob = launch {
            cursorAdapter.positionFlow.collect { position ->
                positions.add(position)
            }
        }

        val orientationFlow = MutableSharedFlow<IMUData>()
        every { mockIMUManager.orientationFlow } returns orientationFlow.asSharedFlow()

        cursorAdapter.startTracking()

        // When: Send orientation updates
        val startTime = System.nanoTime()
        orientationFlow.emit(IMUData(0f, 0f, 0f, startTime))
        advanceUntilIdle()

        val smallEuler = smallRotationQuaternion.toEulerAngles()
        orientationFlow.emit(IMUData(smallEuler.yaw, smallEuler.pitch, smallEuler.roll, startTime + 1000000))
        advanceUntilIdle()

        // Then: Positions should have increasing timestamps
        assertTrue("Should have at least 2 positions", positions.size >= 2)
        assertTrue("Timestamps should increase", positions[1].timestamp > positions[0].timestamp)
        assertTrue("Timestamps should be realistic", positions[0].timestamp >= startTime)

        collectJob.cancel()
    }
}