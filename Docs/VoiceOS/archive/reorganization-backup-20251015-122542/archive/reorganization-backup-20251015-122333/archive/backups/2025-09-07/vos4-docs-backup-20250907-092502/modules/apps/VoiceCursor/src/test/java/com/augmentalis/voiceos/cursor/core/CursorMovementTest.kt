/**
 * CursorMovementTest.kt
 * Test suite for cursor movement and screen edge accessibility
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * 
 * Test Coverage:
 * - Full screen cursor movement
 * - Screen edge accessibility
 * - Boundary checking fixes
 * - Movement scaling validation
 */

package com.augmentalis.voiceos.cursor.core

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class CursorMovementTest {
    
    private lateinit var positionManager: CursorPositionManager
    
    /**
     * Data class to hold test case parameters for cursor movement tests
     */
    data class MovementTestCase(
        val alpha: Float,
        val beta: Float,
        val description: String,
        val expectedX: Float,
        val expectedY: Float
    )
    
    companion object {
        private const val EPSILON = 0.1f
        private const val SCREEN_WIDTH = 1920
        private const val SCREEN_HEIGHT = 1080
        private const val TEST_TIMESTAMP = 16_000_000L // 16ms frame time
    }
    
    @Before
    fun setUp() {
        positionManager = CursorPositionManager(SCREEN_WIDTH, SCREEN_HEIGHT)
        positionManager.centerCursor()
    }
    
    @Test
    fun testCursorCanReachAllScreenEdges() {
        // Test movement to all four screen edges
        val testCases = listOf(
            // (alpha, beta, description, expected near x, expected near y)
            MovementTestCase(10f, 0f, "Right edge", SCREEN_WIDTH.toFloat(), SCREEN_HEIGHT / 2f),
            MovementTestCase(-10f, 0f, "Left edge", 0f, SCREEN_HEIGHT / 2f),
            MovementTestCase(0f, 10f, "Bottom edge", SCREEN_WIDTH / 2f, SCREEN_HEIGHT.toFloat()),
            MovementTestCase(0f, -10f, "Top edge", SCREEN_WIDTH / 2f, 0f)
        )
        
        testCases.forEach { (alpha, beta, description, expectedX, expectedY) ->
            // Reset to center before each test
            positionManager.centerCursor()
            var timestamp = System.nanoTime()
            
            // Apply movement repeatedly to reach edge
            repeat(200) { // More iterations to ensure we can reach edges
                val result = positionManager.calculatePosition(alpha, beta, 0f, timestamp, 10)
                timestamp += TEST_TIMESTAMP
                
                // Stop when we're close to the edge
                if (abs(result.x - expectedX) < 50f || abs(result.y - expectedY) < 50f) {
                    return@repeat
                }
            }
            
            val finalPosition = positionManager.getCurrentPosition()
            
            // Verify we can get reasonably close to the edge (within 100px)
            val distanceFromExpected = when {
                description.contains("Right") -> abs(finalPosition.x - SCREEN_WIDTH.toFloat())
                description.contains("Left") -> abs(finalPosition.x - 0f)
                description.contains("Bottom") -> abs(finalPosition.y - SCREEN_HEIGHT.toFloat())
                description.contains("Top") -> abs(finalPosition.y - 0f)
                else -> 999f
            }
            
            assertTrue("Cursor should reach $description (distance: $distanceFromExpected)", 
                      distanceFromExpected < 100f)
        }
    }
    
    @Test
    fun testBoundaryConstraints() {
        positionManager.centerCursor()
        var timestamp = System.nanoTime()
        
        // Apply extreme movement that would go beyond screen bounds
        repeat(500) {
            val result = positionManager.calculatePosition(50f, 50f, 0f, timestamp, 15)
            timestamp += TEST_TIMESTAMP
            
            // Verify position stays within bounds
            assertTrue("X position should be within screen bounds", 
                      result.x >= 0f && result.x <= SCREEN_WIDTH.toFloat())
            assertTrue("Y position should be within screen bounds", 
                      result.y >= 0f && result.y <= SCREEN_HEIGHT.toFloat())
        }
    }
    
    @Test
    fun testMovementScaling() {
        positionManager.centerCursor()
        val startPosition = positionManager.getCurrentPosition()
        var timestamp = System.nanoTime()
        
        // Test different speed factors
        val speedFactors = listOf(1, 5, 10, 15)
        
        speedFactors.forEach { speedFactor ->
            positionManager.centerCursor()
            
            // Apply same orientation change with different speed factors
            val result = positionManager.calculatePosition(1f, 1f, 0f, timestamp, speedFactor)
            timestamp += TEST_TIMESTAMP
            
            val movementDistance = kotlin.math.sqrt(
                (result.x - startPosition.x) * (result.x - startPosition.x) +
                (result.y - startPosition.y) * (result.y - startPosition.y)
            )
            
            // Higher speed factors should produce more movement (though not necessarily linear)
            assertTrue("Speed factor $speedFactor should enable movement", movementDistance > 0f)
            
            println("Speed factor $speedFactor: movement distance = $movementDistance")
        }
    }
    
    @Test
    fun testCenteringFunctionality() {
        // Move cursor away from center
        var timestamp = System.nanoTime()
        repeat(50) {
            positionManager.calculatePosition(5f, 5f, 0f, timestamp, 10)
            timestamp += TEST_TIMESTAMP
        }
        
        val movedPosition = positionManager.getCurrentPosition()
        val centerX = SCREEN_WIDTH / 2f
        val centerY = SCREEN_HEIGHT / 2f
        
        // Verify cursor moved away from center
        val distanceFromCenter = kotlin.math.sqrt(
            (movedPosition.x - centerX) * (movedPosition.x - centerX) +
            (movedPosition.y - centerY) * (movedPosition.y - centerY)
        )
        assertTrue("Cursor should have moved away from center", distanceFromCenter > 10f)
        
        // Center cursor
        positionManager.centerCursor()
        val recenteredPosition = positionManager.getCurrentPosition()
        
        // Verify cursor is back at center
        assertEquals("X should be at center after centering", centerX, recenteredPosition.x, EPSILON)
        assertEquals("Y should be at center after centering", centerY, recenteredPosition.y, EPSILON)
    }
    
    @Test
    fun testScreenDimensionUpdate() {
        val newWidth = 2560
        val newHeight = 1440
        
        // Update screen dimensions
        positionManager.updateScreenDimensions(newWidth, newHeight)
        
        // Test movement with new dimensions
        var timestamp = System.nanoTime()
        repeat(100) {
            val result = positionManager.calculatePosition(10f, 10f, 0f, timestamp, 10)
            timestamp += TEST_TIMESTAMP
            
            // Verify new boundaries are respected
            assertTrue("X should be within new screen width", 
                      result.x >= 0f && result.x <= newWidth.toFloat())
            assertTrue("Y should be within new screen height", 
                      result.y >= 0f && result.y <= newHeight.toFloat())
        }
    }
    
    @Test
    fun testMovementResponsiveness() {
        positionManager.centerCursor()
        val initialPosition = positionManager.getCurrentPosition()
        var timestamp = System.nanoTime()
        
        // Apply small orientation change
        val result = positionManager.calculatePosition(0.1f, 0.1f, 0f, timestamp, 8)
        
        // Verify some movement occurred (even for small inputs)
        val distance = kotlin.math.sqrt(
            (result.x - initialPosition.x) * (result.x - initialPosition.x) +
            (result.y - initialPosition.y) * (result.y - initialPosition.y)
        )
        
        // With improved scaling, even small movements should be detectable
        assertTrue("Small movements should be responsive (distance: $distance)", 
                  distance > 0.1f || result.moved)
    }
    
    @Test
    fun testIncrementalMovement() {
        positionManager.centerCursor()
        val positions = mutableListOf<CursorOffset>()
        var timestamp = System.nanoTime()
        
        // Apply consistent small movements
        repeat(20) {
            val result = positionManager.calculatePosition(1f, 0f, 0f, timestamp, 8)
            positions.add(CursorOffset(result.x, result.y))
            timestamp += TEST_TIMESTAMP
        }
        
        // Verify cumulative movement
        val startPos = positions.first()
        val endPos = positions.last()
        val totalMovement = endPos.x - startPos.x
        
        assertTrue("Incremental movements should accumulate", totalMovement > 5f)
        
        // Verify smooth progression (no large jumps)
        for (i in 1 until positions.size) {
            val frameMovement = abs(positions[i].x - positions[i-1].x)
            assertTrue("Frame-to-frame movement should be smooth", frameMovement < 50f)
        }
    }
    
    @Test
    fun testMovementInAllDirections() {
        val directions = listOf(
            Pair(1f, 0f),   // Right
            Pair(-1f, 0f),  // Left
            Pair(0f, 1f),   // Down
            Pair(0f, -1f),  // Up
            Pair(1f, 1f),   // Diagonal down-right
            Pair(-1f, -1f), // Diagonal up-left
        )
        
        directions.forEach { (alpha, beta) ->
            positionManager.centerCursor()
            val startPos = positionManager.getCurrentPosition()
            var timestamp = System.nanoTime()
            
            // Apply movement in this direction
            repeat(30) {
                positionManager.calculatePosition(alpha, beta, 0f, timestamp, 10)
                timestamp += TEST_TIMESTAMP
            }
            
            val endPos = positionManager.getCurrentPosition()
            val deltaX = endPos.x - startPos.x
            val deltaY = endPos.y - startPos.y
            
            // Verify movement occurred in expected direction
            when {
                alpha > 0 -> assertTrue("Should move right", deltaX > 1f)
                alpha < 0 -> assertTrue("Should move left", deltaX < -1f)
            }
            
            when {
                beta > 0 -> assertTrue("Should move down", deltaY > 1f)
                beta < 0 -> assertTrue("Should move up", deltaY < -1f)
            }
            
            val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
            assertTrue("Should have significant movement in direction ($alpha, $beta)", distance > 2f)
        }
    }
}