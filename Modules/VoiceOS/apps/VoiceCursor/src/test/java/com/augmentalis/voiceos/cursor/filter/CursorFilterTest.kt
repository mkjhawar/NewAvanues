/**
 * CursorFilterTest.kt
 * Comprehensive test suite for CursorFilter jitter elimination
 * 
 * Created: 2025-09-05
 * Author: QA Specialist - Android Sensor Applications
 * 
 * Test Coverage:
 * - Stationary position holding (90% jitter reduction)
 * - Slow movements (menu navigation) 
 * - Fast movements (gestures)
 * - Motion state transitions
 * - Edge cases and boundary conditions
 * - Performance benchmarks
 */

package com.augmentalis.voiceos.cursor.filter

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.system.measureNanoTime

class CursorFilterTest {
    
    private lateinit var filter: CursorFilter
    
    companion object {
        // Test constants
        private const val EPSILON = 0.1f
        private const val NS_PER_SECOND = 1_000_000_000L
        private const val FRAME_TIME_16MS = 16_000_000L
        private const val FRAME_TIME_33MS = 33_000_000L
        
        // Motion thresholds (matching CursorFilter)
        private const val STATIONARY_THRESHOLD = 50f
        private const val SLOW_THRESHOLD = 200f
        
        // Filter strengths
        private const val STATIONARY_STRENGTH = 90
        private const val SLOW_STRENGTH = 50
        private const val FAST_STRENGTH = 10
        
        // Test screen dimensions
        private const val SCREEN_WIDTH = 1920f
        private const val SCREEN_HEIGHT = 1080f
    }
    
    @Before
    fun setUp() {
        filter = CursorFilter()
    }
    
    // ========== INITIALIZATION TESTS ==========
    
    @Test
    fun testFirstCallReturnsOriginalPosition() {
        val timestamp = System.nanoTime()
        val result = filter.filter(100f, 200f, timestamp)
        
        assertEquals(100f, result.first, EPSILON)
        assertEquals(200f, result.second, EPSILON)
        assertEquals(0f, filter.getMotionLevel(), EPSILON)
    }
    
    @Test
    fun testResetClearsState() {
        val timestamp = System.nanoTime()
        
        // Initialize filter with some data
        filter.filter(100f, 200f, timestamp)
        filter.filter(150f, 250f, timestamp + FRAME_TIME_16MS)
        
        assertTrue(filter.getMotionLevel() > 0f)
        
        // Reset and verify state is cleared
        filter.reset()
        assertEquals(0f, filter.getMotionLevel(), EPSILON)
        
        // Next call should behave like first call
        val result = filter.filter(300f, 400f, timestamp + FRAME_TIME_33MS)
        assertEquals(300f, result.first, EPSILON)
        assertEquals(400f, result.second, EPSILON)
    }
    
    @Test
    fun testEnabledDisabledToggle() {
        val timestamp = System.nanoTime()
        val x = 100f
        val y = 200f
        
        // Test enabled state (default)
        val enabledResult = filter.filter(x, y, timestamp)
        assertEquals(x, enabledResult.first, EPSILON)
        assertEquals(y, enabledResult.second, EPSILON)
        
        // Disable filter
        filter.setEnabled(false)
        
        // Should pass through unfiltered
        val disabledResult = filter.filter(x + 50f, y + 50f, timestamp + FRAME_TIME_16MS)
        assertEquals(x + 50f, disabledResult.first, EPSILON)
        assertEquals(y + 50f, disabledResult.second, EPSILON)
        
        // Re-enable should reset state
        filter.setEnabled(true)
        assertEquals(0f, filter.getMotionLevel(), EPSILON)
    }
    
    // ========== STATIONARY POSITION TESTS ==========
    
    @Test
    fun testStationaryPositionJitterReduction() {
        val baseX = 500f
        val baseY = 300f
        val jitterAmount = 5f // Small jitter
        var timestamp = System.nanoTime()
        
        // Initialize filter
        filter.filter(baseX, baseY, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Apply jitter over several frames
        val results = mutableListOf<Pair<Float, Float>>()
        val jitterPattern = floatArrayOf(
            jitterAmount, -jitterAmount, jitterAmount * 0.7f, 
            -jitterAmount * 0.8f, jitterAmount * 0.5f
        )
        
        jitterPattern.forEach { jitter ->
            val result = filter.filter(baseX + jitter, baseY + jitter, timestamp)
            results.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify motion level is in stationary range
        assertTrue("Motion level should be < $STATIONARY_THRESHOLD for stationary position", 
                  filter.getMotionLevel() < STATIONARY_THRESHOLD)
        
        // Verify filter strength is maximum (90%)
        assertEquals(STATIONARY_STRENGTH, filter.getCurrentStrength())
        
        // Verify jitter reduction (should be heavily filtered)
        results.forEach { (x, y) ->
            val distanceFromBase = sqrt((x - baseX) * (x - baseX) + (y - baseY) * (y - baseY))
            assertTrue("Filtered position should be very close to base position", 
                      distanceFromBase < jitterAmount * 0.2f) // Should reduce jitter by ~80%
        }
    }
    
    @Test
    fun testStationaryPositionConvergence() {
        val baseX = 500f
        val baseY = 300f
        val jitterAmount = 3f
        var timestamp = System.nanoTime()
        
        // Initialize
        filter.filter(baseX, baseY, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Apply consistent jitter and track convergence
        val positions = mutableListOf<Pair<Float, Float>>()
        
        repeat(20) { frame ->
            val jitterX = if (frame % 2 == 0) jitterAmount else -jitterAmount
            val jitterY = if (frame % 3 == 0) jitterAmount else -jitterAmount
            
            val result = filter.filter(baseX + jitterX, baseY + jitterY, timestamp)
            positions.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify positions converge toward a stable point
        val lastFive = positions.takeLast(5)
        val avgX = lastFive.map { it.first }.average().toFloat()
        val avgY = lastFive.map { it.second }.average().toFloat()
        
        lastFive.forEach { (x, y) ->
            assertTrue("Positions should converge in stationary mode", 
                      abs(x - avgX) < 1f && abs(y - avgY) < 1f)
        }
    }
    
    // ========== SLOW MOVEMENT TESTS ==========
    
    @Test
    fun testSlowMovementFiltering() {
        var x = 500f
        var y = 300f
        var timestamp = System.nanoTime()
        
        // Initialize
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Simulate slow movement (menu navigation)
        val movementStep = 2f // 2 pixels per frame = ~120 px/s at 60fps
        val results = mutableListOf<Pair<Float, Float>>()
        
        repeat(30) {
            x += movementStep
            y += movementStep * 0.5f
            val result = filter.filter(x, y, timestamp)
            results.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify motion level is in slow range after stabilization
        val finalMotionLevel = filter.getMotionLevel()
        assertTrue("Motion level should be between stationary and slow thresholds", 
                  finalMotionLevel >= STATIONARY_THRESHOLD && finalMotionLevel < SLOW_THRESHOLD)
        
        // Verify filter strength is medium (50%)
        assertEquals(SLOW_STRENGTH, filter.getCurrentStrength())
        
        // Verify movement is preserved but smoothed
        val firstResult = results.first()
        val lastResult = results.last()
        val totalMovement = sqrt(
            (lastResult.first - firstResult.first) * (lastResult.first - firstResult.first) +
            (lastResult.second - firstResult.second) * (lastResult.second - firstResult.second)
        )
        
        assertTrue("Slow movement should be preserved", totalMovement > 30f)
        
        // Verify smoothness - no large jumps between consecutive frames
        for (i in 1 until results.size) {
            val prev = results[i - 1]
            val curr = results[i]
            val frameDistance = sqrt(
                (curr.first - prev.first) * (curr.first - prev.first) +
                (curr.second - prev.second) * (curr.second - prev.second)
            )
            assertTrue("Movement should be smooth between frames", frameDistance < 10f)
        }
    }
    
    @Test
    fun testSlowMovementResponsiveness() {
        var x = 500f
        var y = 300f
        var timestamp = System.nanoTime()
        
        // Initialize
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Create slow movement with direction change
        val results = mutableListOf<Triple<Float, Float, Long>>()
        
        // Move right slowly
        repeat(10) {
            x += 3f
            val result = filter.filter(x, y, timestamp)
            results.add(Triple(result.first, result.second, timestamp))
            timestamp += FRAME_TIME_16MS
        }
        
        // Change direction - move left slowly  
        repeat(10) {
            x -= 3f
            val result = filter.filter(x, y, timestamp)
            results.add(Triple(result.first, result.second, timestamp))
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify direction change is detected within reasonable time
        val midPoint = results.size / 2
        val rightMovement = results[midPoint - 1]
        val leftMovement = results[midPoint + 3] // Give a few frames to respond
        
        assertTrue("Filter should respond to direction changes in slow movement", 
                  leftMovement.first < rightMovement.first)
    }
    
    // ========== FAST MOVEMENT TESTS ==========
    
    @Test
    fun testFastMovementMinimalFiltering() {
        var x = 500f
        var y = 300f
        var timestamp = System.nanoTime()
        
        // Initialize
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Simulate fast movement (gesture)
        val movementStep = 20f // 20 pixels per frame = ~1200 px/s at 60fps
        val results = mutableListOf<Pair<Float, Float>>()
        
        repeat(15) {
            x += movementStep
            y += movementStep * 0.7f
            val result = filter.filter(x, y, timestamp)
            results.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify motion level exceeds slow threshold
        assertTrue("Motion level should exceed slow threshold for fast movement", 
                  filter.getMotionLevel() >= SLOW_THRESHOLD)
        
        // Verify filter strength is minimal (10%)
        assertEquals(FAST_STRENGTH, filter.getCurrentStrength())
        
        // Verify fast movement is preserved with minimal filtering
        val firstResult = results.first()
        val lastResult = results.last()
        val totalMovement = sqrt(
            (lastResult.first - firstResult.first) * (lastResult.first - firstResult.first) +
            (lastResult.second - firstResult.second) * (lastResult.second - firstResult.second)
        )
        
        // Movement should be mostly preserved (>90% of input movement)
        val expectedMovement = movementStep * results.size * sqrt(1f + 0.7f * 0.7f)
        assertTrue("Fast movement should be mostly preserved", 
                  totalMovement > expectedMovement * 0.9f)
    }
    
    @Test
    fun testFastMovementResponsiveness() {
        var x = 500f
        var y = 300f  
        var timestamp = System.nanoTime()
        
        // Initialize
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Test rapid direction change (gesture reversal)
        val positions = mutableListOf<Pair<Float, Float>>()
        
        // Fast movement right
        repeat(5) {
            x += 25f
            val result = filter.filter(x, y, timestamp)
            positions.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Immediate reversal - fast movement left
        repeat(5) {
            x -= 25f
            val result = filter.filter(x, y, timestamp)
            positions.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify rapid response to direction change
        val rightmostPos = positions[4].first
        val afterReversalPos = positions[6].first
        
        assertTrue("Filter should respond immediately to fast direction changes", 
                  afterReversalPos < rightmostPos)
        
        // Verify minimal lag (position should change within 1-2 frames)
        val firstReversalPos = positions[5].first
        assertTrue("Direction change should be detected within one frame", 
                  firstReversalPos <= rightmostPos)
    }
    
    // ========== MOTION STATE TRANSITION TESTS ==========
    
    @Test
    fun testStationaryToSlowTransition() {
        var x = 500f
        var y = 300f
        var timestamp = System.nanoTime()
        
        // Initialize and establish stationary state
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Remain stationary with small jitter
        repeat(10) {
            val jitter = if (it % 2 == 0) 2f else -2f
            filter.filter(x + jitter, y + jitter, timestamp)
            timestamp += FRAME_TIME_16MS
        }
        
        assertEquals(STATIONARY_STRENGTH, filter.getCurrentStrength())
        
        // Begin slow movement
        repeat(15) {
            x += 2.5f
            y += 1.5f
            filter.filter(x, y, timestamp)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify transition to slow movement
        assertEquals(SLOW_STRENGTH, filter.getCurrentStrength())
        assertTrue("Motion level should transition to slow range", 
                  filter.getMotionLevel() >= STATIONARY_THRESHOLD)
        assertTrue("Motion level should not exceed slow threshold", 
                  filter.getMotionLevel() < SLOW_THRESHOLD)
    }
    
    @Test
    fun testSlowToFastTransition() {
        var x = 500f
        var y = 300f
        var timestamp = System.nanoTime()
        
        // Initialize with slow movement
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        repeat(10) {
            x += 3f
            y += 2f
            filter.filter(x, y, timestamp)
            timestamp += FRAME_TIME_16MS
        }
        
        assertEquals(SLOW_STRENGTH, filter.getCurrentStrength())
        
        // Accelerate to fast movement
        repeat(10) {
            x += 15f
            y += 12f
            filter.filter(x, y, timestamp)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify transition to fast movement
        assertEquals(FAST_STRENGTH, filter.getCurrentStrength())
        assertTrue("Motion level should exceed slow threshold for fast movement", 
                  filter.getMotionLevel() >= SLOW_THRESHOLD)
    }
    
    @Test
    fun testFastToStationaryTransition() {
        var x = 500f
        var y = 300f
        var timestamp = System.nanoTime()
        
        // Initialize with fast movement
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        repeat(8) {
            x += 20f
            y += 15f
            filter.filter(x, y, timestamp)
            timestamp += FRAME_TIME_16MS
        }
        
        assertEquals(FAST_STRENGTH, filter.getCurrentStrength())
        
        // Stop abruptly and become stationary
        repeat(20) {
            val jitter = if (it % 2 == 0) 1f else -1f
            filter.filter(x + jitter, y + jitter, timestamp)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify eventual transition to stationary (may take time to settle)
        assertTrue("Motion level should eventually drop below stationary threshold", 
                  filter.getMotionLevel() < STATIONARY_THRESHOLD * 1.5f) // Allow some tolerance
    }
    
    // ========== EDGE CASES AND BOUNDARY CONDITIONS ==========
    
    @Test
    fun testScreenBoundaryHandling() {
        var timestamp = System.nanoTime()
        
        // Test at screen edges
        val edgePositions = listOf(
            Pair(0f, 0f),           // Top-left corner
            Pair(SCREEN_WIDTH, 0f), // Top-right corner
            Pair(0f, SCREEN_HEIGHT), // Bottom-left corner
            Pair(SCREEN_WIDTH, SCREEN_HEIGHT), // Bottom-right corner
            Pair(SCREEN_WIDTH / 2, 0f),        // Top edge
            Pair(SCREEN_WIDTH / 2, SCREEN_HEIGHT), // Bottom edge
            Pair(0f, SCREEN_HEIGHT / 2),       // Left edge
            Pair(SCREEN_WIDTH, SCREEN_HEIGHT / 2)  // Right edge
        )
        
        edgePositions.forEach { (x, y) ->
            filter.reset()
            val result = filter.filter(x, y, timestamp)
            
            // Filter should handle edge positions without issues
            assertEquals(x, result.first, EPSILON)
            assertEquals(y, result.second, EPSILON)
            
            // Add some movement and verify stability
            val result2 = filter.filter(x + 1f, y + 1f, timestamp + FRAME_TIME_16MS)
            assertTrue("Filter should remain stable at screen boundaries", 
                      result2.first >= -1f && result2.second >= -1f)
            
            timestamp += FRAME_TIME_33MS
        }
    }
    
    @Test
    fun testRapidDirectionChanges() {
        var x = 500f
        var y = 300f
        var timestamp = System.nanoTime()
        
        // Initialize
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Rapid zigzag pattern
        val directions = arrayOf(
            Pair(10f, 0f),   // Right
            Pair(-10f, 0f),  // Left  
            Pair(0f, 10f),   // Down
            Pair(0f, -10f),  // Up
            Pair(7f, 7f),    // Diagonal right-down
            Pair(-7f, -7f),  // Diagonal left-up
        )
        
        val results = mutableListOf<Pair<Float, Float>>()
        
        repeat(20) { frame ->
            val direction = directions[frame % directions.size]
            x += direction.first
            y += direction.second
            
            val result = filter.filter(x, y, timestamp)
            results.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Filter should remain stable despite rapid changes
        assertFalse("Filter should not produce NaN values", 
                   results.any { it.first.isNaN() || it.second.isNaN() })
        
        // Verify motion level adapts appropriately
        assertTrue("Motion level should reflect rapid movement", 
                  filter.getMotionLevel() > STATIONARY_THRESHOLD)
        
        // Check for overshooting or instability
        for (i in 1 until results.size) {
            val prev = results[i - 1]
            val curr = results[i]
            val movement = sqrt(
                (curr.first - prev.first) * (curr.first - prev.first) +
                (curr.second - prev.second) * (curr.second - prev.second)
            )
            assertTrue("Movement should not exceed reasonable bounds", movement < 50f)
        }
    }
    
    @Test
    fun testHighFrequencyUpdates() {
        var x = 500f
        var y = 300f
        var timestamp = System.nanoTime()
        
        // Initialize
        filter.filter(x, y, timestamp)
        timestamp += 1_000_000L // 1ms
        
        // Test high frequency updates (should be throttled)
        val results = mutableListOf<Pair<Float, Float>>()
        
        repeat(50) {
            x += 0.5f
            val result = filter.filter(x, y, timestamp)
            results.add(result)
            timestamp += 1_000_000L // 1ms intervals
        }
        
        // Verify throttling - many results should be identical
        val uniquePositions = results.toSet()
        assertTrue("High frequency updates should be throttled", 
                  uniquePositions.size < results.size * 0.5)
        
        // Verify final position eventually catches up
        val finalResult = filter.filter(x, y, timestamp + FRAME_TIME_16MS)
        assertTrue("Filter should eventually track to current position", 
                  abs(finalResult.first - x) < 5f)
    }
    
    @Test
    fun testExtremeValues() {
        var timestamp = System.nanoTime()
        
        val extremeValues = listOf(
            Pair(Float.MAX_VALUE, Float.MAX_VALUE),
            Pair(Float.MIN_VALUE, Float.MIN_VALUE),
            Pair(-100000f, -100000f),
            Pair(100000f, 100000f)
        )
        
        extremeValues.forEach { (x, y) ->
            filter.reset()
            val result = filter.filter(x, y, timestamp)
            
            // Should handle extreme values gracefully
            assertFalse("Filter should not produce NaN for extreme inputs", 
                       result.first.isNaN() || result.second.isNaN())
            assertFalse("Filter should not produce infinite values", 
                       result.first.isInfinite() || result.second.isInfinite())
            
            timestamp += FRAME_TIME_33MS
        }
    }
    
    @Test
    fun testZeroTimestampHandling() {
        // Test behavior with zero or invalid timestamps
        val result1 = filter.filter(100f, 200f, 0L)
        assertEquals(100f, result1.first, EPSILON)
        assertEquals(200f, result1.second, EPSILON)
        
        // Second call with zero timestamp should not crash
        val result2 = filter.filter(150f, 250f, 0L)
        // Should handle gracefully without crashing
        assertFalse(result2.first.isNaN() || result2.second.isNaN())
    }
    
    // ========== FILTER MATH UNIT TESTS ==========
    
    @Test
    fun testFilterMathPrecision() {
        var timestamp = System.nanoTime()
        
        // Initialize with known position
        filter.filter(100f, 100f, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Apply known movement that should trigger specific filter strength
        val result = filter.filter(105f, 105f, timestamp + FRAME_TIME_16MS)
        
        // Verify integer math produces expected results
        // With stationary filtering (90% strength):
        // filtered = (newPos * 10 + oldPos * 90) / 100
        val expectedX = ((105f * 10 + 100f * 90) / 100).toInt().toFloat()
        val expectedY = ((105f * 10 + 100f * 90) / 100).toInt().toFloat()
        
        // Allow for motion level calculation affecting the result
        val tolerance = 5f // Reasonable tolerance for motion-dependent filtering
        assertTrue("Filter math should be consistent", 
                  abs(result.first - expectedX) < tolerance)
        assertTrue("Filter math should be consistent", 
                  abs(result.second - expectedY) < tolerance)
    }
    
    @Test
    fun testMotionLevelCalculation() {
        var timestamp = System.nanoTime()
        
        // Initialize
        filter.filter(0f, 0f, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Move known distance in known time
        val distance = 100f
        filter.filter(distance, 0f, timestamp)
        
        // Calculate expected motion level
        val deltaTimeSeconds = FRAME_TIME_16MS / 1_000_000_000f
        val expectedMotion = distance / deltaTimeSeconds
        
        // Motion level uses smoothing (90% old, 10% new), starting from 0
        val expectedSmoothedMotion = expectedMotion * 0.1f
        
        val actualMotion = filter.getMotionLevel()
        assertTrue("Motion level calculation should be accurate", 
                  abs(actualMotion - expectedSmoothedMotion) < 10f)
    }
    
    @Test
    fun testMotionLevelSmoothing() {
        var timestamp = System.nanoTime()
        var x = 0f
        
        // Initialize
        filter.filter(x, 0f, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Apply consistent movement
        val movementStep = 50f
        val motionLevels = mutableListOf<Float>()
        
        repeat(10) {
            x += movementStep
            filter.filter(x, 0f, timestamp)
            motionLevels.add(filter.getMotionLevel())
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify motion level converges (smoothing effect)
        assertTrue("Motion level should increase initially", 
                  motionLevels[1] > motionLevels[0])
        
        // Later values should stabilize
        val lastThree = motionLevels.takeLast(3)
        val variance = lastThree.map { it - lastThree.average() }.map { it * it }.average()
        assertTrue("Motion level should stabilize with consistent input", variance < 100f)
    }
    
    // ========== INTEGRATION TESTS WITH MOCK SENSOR DATA ==========
    
    @Test
    fun testRealisticSensorNoise() {
        var timestamp = System.nanoTime()
        val baseX = 500f
        val baseY = 300f
        
        // Initialize
        filter.filter(baseX, baseY, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Simulate realistic sensor noise pattern
        val results = mutableListOf<Pair<Float, Float>>()
        val random = kotlin.random.Random(42) // Deterministic for testing
        
        repeat(100) {
            // Add realistic sensor noise (±2 pixels typical)
            val noiseX = random.nextFloat() * 4f - 2f
            val noiseY = random.nextFloat() * 4f - 2f
            
            val result = filter.filter(baseX + noiseX, baseY + noiseY, timestamp)
            results.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify noise reduction
        val avgX = results.map { it.first }.average().toFloat()
        val avgY = results.map { it.second }.average().toFloat()
        
        // Filtered average should be close to base position
        assertTrue("Noise should be filtered out", abs(avgX - baseX) < 1f)
        assertTrue("Noise should be filtered out", abs(avgY - baseY) < 1f)
        
        // Verify stability - filtered positions should have lower variance
        val filteredVarianceX = results.map { (it.first - avgX) * (it.first - avgX) }.average()
        assertTrue("Filtered signal should be more stable", filteredVarianceX < 2f)
    }
    
    @Test
    fun testHandTremorSimulation() {
        var timestamp = System.nanoTime()
        var x = 500f
        var y = 300f
        
        // Initialize
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Simulate hand tremor during attempted stationary positioning
        val results = mutableListOf<Pair<Float, Float>>()
        
        repeat(60) { frame ->
            // Tremor pattern: higher frequency, lower amplitude variations
            val tremorX = kotlin.math.sin((frame * 0.5).toDouble()).toFloat() * 3f + kotlin.math.sin((frame * 1.2).toDouble()).toFloat() * 1.5f
            val tremorY = kotlin.math.cos((frame * 0.7).toDouble()).toFloat() * 2.5f + kotlin.math.sin((frame * 0.9).toDouble()).toFloat() * 1.8f
            
            val result = filter.filter(x + tremorX, y + tremorY, timestamp)
            results.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify tremor is significantly reduced
        val maxDeviation = results.map { 
            kotlin.math.max(abs(it.first - x), abs(it.second - y))
        }.maxOrNull() ?: 0f
        
        assertTrue("Hand tremor should be reduced by filter", maxDeviation < 2f)
        
        // Verify motion level indicates stationary state
        assertTrue("Tremor should be classified as stationary", 
                  filter.getMotionLevel() < STATIONARY_THRESHOLD)
    }
    
    @Test
    fun testVaryingFrameRates() {
        var timestamp = System.nanoTime()
        var x = 100f
        var y = 100f
        
        // Initialize
        filter.filter(x, y, timestamp)
        
        // Test different frame rates
        val frameIntervals = longArrayOf(
            8_000_000L,  // 125 Hz
            16_000_000L, // 62.5 Hz  
            33_000_000L, // 30 Hz
            50_000_000L  // 20 Hz
        )
        
        frameIntervals.forEach { interval ->
            filter.reset()
            x = 200f
            y = 200f
            timestamp = System.nanoTime()
            
            // Initialize
            filter.filter(x, y, timestamp)
            timestamp += interval
            
            // Apply movement at this frame rate
            repeat(10) {
                x += 5f
                y += 3f
                val result = filter.filter(x, y, timestamp)
                
                // Filter should work consistently across frame rates
                assertFalse("Filter should handle varying frame rates", 
                           result.first.isNaN() || result.second.isNaN())
                
                timestamp += interval
            }
            
            // Motion detection should adapt to frame rate
            assertTrue("Motion level should be reasonable for frame rate", 
                      filter.getMotionLevel() > 0f && filter.getMotionLevel() < 10000f)
        }
    }
    
    // ========== PERFORMANCE BENCHMARK TESTS ==========
    
    @Test
    fun testFilterPerformance() {
        val iterations = 10000
        var timestamp = System.nanoTime()
        var x = 500f
        var y = 300f
        
        // Warm up
        repeat(100) {
            filter.filter(x, y, timestamp)
            x += 0.1f
            y += 0.1f
            timestamp += FRAME_TIME_16MS
        }
        
        // Benchmark filtering operation
        val elapsed = measureNanoTime {
            repeat(iterations) {
                filter.filter(x, y, timestamp)
                x += 0.1f
                y += 0.1f
                timestamp += FRAME_TIME_16MS
            }
        }
        
        val avgTimePerCall = elapsed / iterations
        val targetTime = 100_000L // 0.1ms in nanoseconds
        
        assertTrue("Filter should process in <0.1ms per call (actual: ${avgTimePerCall}ns)", 
                  avgTimePerCall < targetTime)
        
        println("CursorFilter Performance: ${avgTimePerCall}ns per call (${iterations} iterations)")
    }
    
    @Test
    fun testMemoryUsage() {
        val runtime = Runtime.getRuntime()
        
        // Force garbage collection and measure baseline
        System.gc()
        Thread.sleep(100)
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Create many filter instances
        val filters = mutableListOf<CursorFilter>()
        repeat(1000) {
            filters.add(CursorFilter())
        }
        
        // Use filters to ensure they're not optimized away
        filters.forEachIndexed { index, filter ->
            filter.filter(index.toFloat(), index.toFloat(), System.nanoTime())
        }
        
        System.gc()
        Thread.sleep(100)
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val memoryPerFilter = (afterMemory - beforeMemory) / filters.size
        val targetMemory = 1024L // 1KB per filter instance
        
        assertTrue("Each filter should use <1KB memory (actual: ${memoryPerFilter}B)", 
                  memoryPerFilter < targetMemory)
        
        println("CursorFilter Memory Usage: ${memoryPerFilter}B per instance")
    }
    
    @Test
    fun testConcurrentAccess() {
        val filter = CursorFilter()
        val results = mutableListOf<Pair<Float, Float>>()
        val threads = mutableListOf<Thread>()
        val baseTimestamp = System.nanoTime()
        
        // Create multiple threads accessing the filter
        repeat(4) { threadIndex ->
            val thread = Thread {
                repeat(100) { iteration ->
                    val x = threadIndex * 100f + iteration
                    val y = threadIndex * 100f + iteration
                    val timestamp = baseTimestamp + (threadIndex * 1000 + iteration) * FRAME_TIME_16MS
                    
                    val result = filter.filter(x, y, timestamp)
                    synchronized(results) {
                        results.add(result)
                    }
                    
                    Thread.sleep(1) // Small delay to encourage race conditions
                }
            }
            threads.add(thread)
        }
        
        // Start all threads
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        // Verify no crashes or invalid results
        assertEquals(400, results.size)
        results.forEach { (x, y) ->
            assertFalse("Concurrent access should not produce NaN", x.isNaN() || y.isNaN())
            assertFalse("Concurrent access should not produce infinite values", 
                       x.isInfinite() || y.isInfinite())
        }
        
        println("CursorFilter Concurrent Test: ${results.size} operations completed successfully")
    }
    
    // ========== UTILITY METHODS FOR TEST DATA GENERATION ==========
    
    private fun generateSensorNoisePattern(baseValue: Float, amplitude: Float, frames: Int): FloatArray {
        val random = kotlin.random.Random(12345) // Deterministic
        return FloatArray(frames) { baseValue + (random.nextFloat() - 0.5f) * 2 * amplitude }
    }
    
    private fun generateSmoothMovement(start: Float, end: Float, frames: Int): FloatArray {
        val step = (end - start) / frames
        return FloatArray(frames) { start + it * step }
    }
    
    private fun calculateJitterReduction(originalPositions: FloatArray, filteredPositions: FloatArray): Float {
        val originalVariance = originalPositions.map { it - originalPositions.average() }
                                                .map { it * it }
                                                .average()
        val filteredVariance = filteredPositions.map { it - filteredPositions.average() }
                                                .map { it * it }
                                                .average()
        return (1f - (filteredVariance / originalVariance).toFloat()) * 100f
    }
    
    // ========== COMPREHENSIVE SCENARIO TESTS ==========
    
    @Test
    fun testCompleteUserScenario_MenuNavigation() {
        var timestamp = System.nanoTime()
        var x = 500f
        var y = 300f
        
        // User starts with hand slightly shaking (stationary)
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        val scenario = mutableListOf<Triple<Float, Float, String>>()
        
        // 1. Stationary with tremor (menu visible)
        repeat(30) { frame ->
            val tremor = kotlin.math.sin((frame * 0.8).toDouble()).toFloat() * 2f
            val result = filter.filter(x + tremor, y + tremor, timestamp)
            scenario.add(Triple(result.first, result.second, "stationary"))
            timestamp += FRAME_TIME_16MS
        }
        
        // 2. Slow movement to menu item (navigation)
        repeat(20) {
            x += 2f
            y += 1f
            val result = filter.filter(x, y, timestamp)
            scenario.add(Triple(result.first, result.second, "slow_movement"))
            timestamp += FRAME_TIME_16MS
        }
        
        // 3. Stationary at menu item (hovering)
        repeat(15) { frame ->
            val microMovement = kotlin.math.sin((frame * 1.2).toDouble()).toFloat() * 1f
            val result = filter.filter(x + microMovement, y + microMovement, timestamp)
            scenario.add(Triple(result.first, result.second, "hover"))
            timestamp += FRAME_TIME_16MS
        }
        
        // Validate scenario behavior
        val stationaryPhase = scenario.filter { it.third == "stationary" }
        val movementPhase = scenario.filter { it.third == "slow_movement" }
        val hoverPhase = scenario.filter { it.third == "hover" }
        
        // Stationary phase should show minimal position variation
        val stationaryVariation = stationaryPhase.map { it.first }.let { positions ->
            positions.map { it - positions.average() }.map { it * it }.average()
        }
        assertTrue("Stationary phase should have minimal variation", stationaryVariation < 5f)
        
        // Movement phase should show progression
        val firstMovement = movementPhase.first()
        val lastMovement = movementPhase.last()
        assertTrue("Movement phase should show progression", 
                  lastMovement.first > firstMovement.first)
        
        // Hover phase should be stable
        val hoverVariation = hoverPhase.map { it.second }.let { positions ->
            positions.map { it - positions.average() }.map { it * it }.average()
        }
        assertTrue("Hover phase should be stable", hoverVariation < 3f)
    }
    
    @Test
    fun testCompleteUserScenario_GestureDrawing() {
        var timestamp = System.nanoTime()
        var x = 200f
        var y = 200f
        
        // User draws a circle gesture
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        val scenario = mutableListOf<Pair<Float, Float>>()
        val radius = 100f
        val steps = 60
        
        repeat(steps) { step ->
            val angle = (step.toFloat() / steps) * 2 * kotlin.math.PI.toFloat()
            val targetX = 200f + (kotlin.math.cos(angle.toDouble()).toFloat() * radius)
            val targetY = 200f + (kotlin.math.sin(angle.toDouble()).toFloat() * radius)
            
            // Add some natural hand shake during drawing
            val shake = kotlin.random.Random(step).nextFloat() * 2f - 1f
            
            val result = filter.filter(targetX + shake, targetY + shake, timestamp)
            scenario.add(result)
            timestamp += FRAME_TIME_16MS
        }
        
        // Verify circle shape is preserved (should be smooth)
        val centerX = scenario.map { it.first }.average().toFloat()
        val centerY = scenario.map { it.second }.average().toFloat()
        
        // Check that most points are approximately on the circle
        val radialDistances = scenario.map { (x, y) ->
            kotlin.math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY))
        }
        
        val avgRadius = radialDistances.average().toFloat()
        val radiusVariation = radialDistances.map { (it - avgRadius) * (it - avgRadius) }.average()
        
        assertTrue("Gesture should maintain consistent shape", radiusVariation < 100f)
        assertTrue("Average radius should be close to target", abs(avgRadius - radius) < 10f)
    }
}

/**
 * Test utilities for CursorFilter testing
 */
object CursorFilterTestUtils {
    
    /**
     * Creates realistic sensor data with controlled noise
     */
    fun createSensorDataSequence(
        baseX: Float,
        baseY: Float,
        frames: Int,
        noiseAmplitude: Float = 2f,
        movementPattern: MovementPattern = MovementPattern.STATIONARY
    ): List<Triple<Float, Float, Long>> {
        val random = kotlin.random.Random(42)
        val data = mutableListOf<Triple<Float, Float, Long>>()
        var currentX = baseX
        var currentY = baseY
        var timestamp = System.nanoTime()
        
        repeat(frames) { frame ->
            // Apply movement pattern
            when (movementPattern) {
                MovementPattern.STATIONARY -> {
                    // Small random drift
                    currentX += (random.nextFloat() - 0.5f) * 0.1f
                    currentY += (random.nextFloat() - 0.5f) * 0.1f
                }
                MovementPattern.LINEAR -> {
                    currentX += 2f
                    currentY += 1f
                }
                MovementPattern.CIRCULAR -> {
                    val angle = (frame.toFloat() / frames) * 2 * kotlin.math.PI.toFloat()
                    currentX = baseX + kotlin.math.cos(angle.toDouble()).toFloat() * 50f
                    currentY = baseY + kotlin.math.sin(angle.toDouble()).toFloat() * 50f
                }
                MovementPattern.ZIGZAG -> {
                    if (frame % 10 < 5) {
                        currentX += 3f
                    } else {
                        currentX -= 3f
                    }
                    currentY += 1f
                }
            }
            
            // Add sensor noise
            val noiseX = (random.nextFloat() - 0.5f) * 2 * noiseAmplitude
            val noiseY = (random.nextFloat() - 0.5f) * 2 * noiseAmplitude
            
            data.add(Triple(currentX + noiseX, currentY + noiseY, timestamp))
            timestamp += 16_000_000L // 16ms
        }
        
        return data
    }
    
    enum class MovementPattern {
        STATIONARY,
        LINEAR,
        CIRCULAR,
        ZIGZAG
    }
    
    /**
     * Calculates signal-to-noise improvement ratio
     */
    fun calculateSNRImprovement(
        originalData: List<Pair<Float, Float>>,
        filteredData: List<Pair<Float, Float>>
    ): Float {
        require(originalData.size == filteredData.size) { "Data sets must be same size" }
        
        // Calculate variance for X coordinates
        val originalVarianceX = originalData.map { it.first }.let { values ->
            val mean = values.average()
            values.map { (it - mean) * (it - mean) }.average()
        }
        
        val filteredVarianceX = filteredData.map { it.first }.let { values ->
            val mean = values.average()
            values.map { (it - mean) * (it - mean) }.average()
        }
        
        return if (filteredVarianceX > 0) {
            10f * kotlin.math.log10((originalVarianceX / filteredVarianceX).toDouble()).toFloat()
        } else {
            Float.POSITIVE_INFINITY
        }
    }
    
    /**
     * Measures filter responsiveness to step changes
     */
    fun measureStepResponse(filter: CursorFilter, stepSize: Float): StepResponse {
        var timestamp = System.nanoTime()
        
        // Initialize at origin
        filter.reset()
        filter.filter(0f, 0f, timestamp)
        timestamp += 16_000_000L
        
        // Apply step input and measure response
        val responses = mutableListOf<Float>()
        
        repeat(50) {
            val result = filter.filter(stepSize, 0f, timestamp)
            responses.add(result.first)
            timestamp += 16_000_000L
        }
        
        // Find 10% and 90% response times
        val finalValue = responses.last()
        val target10 = finalValue * 0.1f
        val target90 = finalValue * 0.9f
        
        val time10 = responses.indexOfFirst { it >= target10 } * 16
        val time90 = responses.indexOfFirst { it >= target90 } * 16
        
        return StepResponse(
            riseTime = (time90 - time10),
            settlingTime = responses.indexOfLast { 
                abs(it - finalValue) > finalValue * 0.02f 
            } * 16,
            overshoot = (responses.maxOrNull() ?: 0f) - finalValue,
            finalValue = finalValue
        )
    }
    
    data class StepResponse(
        val riseTime: Int,      // Time from 10% to 90% response (ms)
        val settlingTime: Int,  // Time to settle within 2% of final value (ms)
        val overshoot: Float,   // Maximum overshoot beyond final value
        val finalValue: Float   // Final settled value
    )
}