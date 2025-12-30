/**
 * PositionManager.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/core/PositionManager.kt
 * 
 * Created: 2025-01-23 00:15 PST
 * Last Modified: 2025-01-26 00:00 PST
 * Author: VOS4 Development Team
 * Version: 2.2.0
 * 
 * Purpose: Thread-safe cursor position calculations and movement management
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-23 00:15 PST): Initial port from VoiceOS
 * - v2.0.0 (2025-01-23 00:25 PDT): Fixed thread safety, resource management, logic errors
 * - v2.1.0 (2025-01-23 00:33 PDT): Updated package name to com.augmentalis.voiceos.cursor
 * - v2.2.0 (2025-01-26 00:00 PST): Migrated to VoiceCursor module with namespace com.augmentalis.voiceos.voicecursor
 */

package com.augmentalis.voiceos.cursor.core

import com.augmentalis.voiceos.cursor.filter.CursorFilter
import com.augmentalis.voiceos.cursor.view.EdgeDetectionResult
import com.augmentalis.voiceos.cursor.view.EdgeType
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Manages cursor position calculations and movement
 * Single Responsibility: Handle all position-related logic
 * Thread-safe implementation with proper resource management
 */
open class CursorPositionManager(
    private var screenWidth: Int = 0,
    private var screenHeight: Int = 0
) {
    companion object {
        private const val RADIAN_TOLERANCE = 0.002f
        private const val DEFAULT_DISPLACEMENT_FACTOR = 8
        private const val DISTANCE_TOLERANCE = 1.0f
        private const val MIN_INTERVAL_NS = 8_000_000L // 8ms (~120Hz)
        
        // Edge detection constants
        private const val EDGE_BUFFER = 20f // Pixels from edge to start detection
        private const val BOUNCE_FACTOR = 0.3f // How much to bounce back
        private const val EDGE_RESISTANCE = 0.5f // Movement resistance at edges
        
        // Cursor center offsets
        private const val HAND_CURSOR_CENTER_X = 0.413f
        private const val HAND_CURSOR_CENTER_Y = 0.072f
        private const val ROUND_CURSOR_CENTER_X = 0.5f
        private const val ROUND_CURSOR_CENTER_Y = 0.5f
    }
    
    // Movement parameters
    private val cursorScaleX = 2.0f
    private val cursorScaleY = 3.0f
    private val cursorScaleZ = 2.0f
    
    // FIX: Thread-safe with @Volatile
    @Volatile private var previousAlpha = 0f
    @Volatile private var previousBeta = 0f
    @Volatile private var previousGamma = 0f
    
    // FIX: Thread-safe position tracking
    @Volatile private var currentX = 0f
    @Volatile private var currentY = 0f
    @Volatile private var startX = 0f
    @Volatile private var startY = 0f
    
    // Edge detection state
    @Volatile private var isAtEdge = false
    @Volatile private var lastEdgeTime = 0L
    @Volatile private var bounceVelocityX = 0f
    @Volatile private var bounceVelocityY = 0f
    
    // Performance optimization
    @Volatile private var lastProcessTime = 0L
    
    // FIX: Synchronization lock for compound operations
    private val positionLock = Any()
    
    // Moving averages for smoothing
    private val alphaAverage = MovingAverage(4, 300_000_000L)
    private val betaAverage = MovingAverage(4, 300_000_000L)
    private val gammaAverage = MovingAverage(4, 300_000_000L)
    
    // CursorFilter for jitter elimination
    private val cursorFilter = CursorFilter()
    
    fun updateScreenDimensions(width: Int, height: Int) {
        synchronized(positionLock) {
            screenWidth = width
            screenHeight = height
            centerCursor()
        }
    }
    
    fun centerCursor() {
        synchronized(positionLock) {
            startX = (screenWidth / 2).toFloat()
            startY = (screenHeight / 2).toFloat()
            currentX = startX
            currentY = startY
            // Reset filter when cursor is recentered
            cursorFilter.reset()
        }
    }
    
    /**
     * Calculate new cursor position based on orientation
     * FIX: Thread-safe with synchronization
     */
    open fun calculatePosition(
        alpha: Float,
        beta: Float, 
        gamma: Float,
        timestamp: Long,
        speedFactor: Int = DEFAULT_DISPLACEMENT_FACTOR
    ): PositionResult {
        // FIX: Synchronized block for thread safety
        synchronized(positionLock) {
            // Skip if too frequent
            val currentTime = System.nanoTime()
            if (currentTime - lastProcessTime < MIN_INTERVAL_NS) {
                return PositionResult(currentX, currentY, 0f, false)
            }
            lastProcessTime = currentTime
            
            // Apply smoothing
            val smoothAlpha = alphaAverage.getAvg(alpha, timestamp)
            val smoothBeta = betaAverage.getAvg(beta, timestamp)
            val smoothGamma = if (gamma < 0) {
                -gammaAverage.getAvg(-gamma, timestamp)
            } else {
                gammaAverage.getAvg(gamma, timestamp)
            }
            
            // Calculate deltas
            val dx = smoothAlpha - previousAlpha
            val dy = smoothBeta - previousBeta
            val dz = smoothGamma - previousGamma
            
            // Check for tiny movements
            if (abs(dx) < RADIAN_TOLERANCE && 
                abs(dy) < RADIAN_TOLERANCE && 
                abs(dz) < RADIAN_TOLERANCE) {
                return PositionResult(currentX, currentY, 0f, false, false, EdgeType.NONE)
            }
            
            // FIX: First-time initialization now allows movement
            if (previousAlpha == 0f && previousBeta == 0f && previousGamma == 0f) {
                previousAlpha = smoothAlpha
                previousBeta = smoothBeta
                previousGamma = smoothGamma
                // FIX: Return with moved=true to allow immediate response
                return PositionResult(currentX, currentY, 0.1f, true, false, EdgeType.NONE)
            }
            
            // Update previous values
            previousAlpha = smoothAlpha
            previousBeta = smoothBeta
            previousGamma = smoothGamma
            
            // Calculate displacement based on rotation changes with improved scaling
            val speedMultiplier = speedFactor * 0.8  // Increased from 0.5 to 0.8
            val disX = dx * screenWidth * cursorScaleX * speedMultiplier
            val disY = dy * screenHeight * cursorScaleY * speedMultiplier
            
            // Apply fine tuning for small movements (less restrictive than before)
            val finalDisX = if (abs(disX) < (screenWidth / 80)) disX * 0.6 else disX
            val finalDisY = if (abs(disY) < (screenHeight / 80)) disY * 0.6 else disY
            
            // Calculate intended position
            val intendedX = currentX + finalDisX.toFloat()
            val intendedY = currentY + finalDisY.toFloat()
            
            // Apply edge detection and bounce-back
            val edgeResult = detectEdges(intendedX, intendedY, currentTime)
            val (constrainedX, constrainedY) = applyEdgeConstraints(intendedX, intendedY, edgeResult)
            
            // Apply CursorFilter for jitter elimination
            val (filteredX, filteredY) = cursorFilter.filter(constrainedX, constrainedY, System.nanoTime())
            
            // Calculate movement distance
            val deltaX = filteredX - currentX
            val deltaY = filteredY - currentY
            val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
            
            // Update current position only (don't reset start position)
            currentX = filteredX
            currentY = filteredY
            
            return PositionResult(
                x = filteredX, 
                y = filteredY, 
                distance = distance, 
                moved = distance > DISTANCE_TOLERANCE,
                edgeDetected = edgeResult.isAtEdge,
                edgeType = edgeResult.edgeType
            )
        }
    }
    
    fun getCurrentPosition(): CursorOffset = CursorOffset(currentX, currentY)
    
    /**
     * Detect if cursor is near or at screen edges
     */
    private fun detectEdges(x: Float, y: Float, timestamp: Long): EdgeDetectionResult {
        val edgeType = when {
            x <= EDGE_BUFFER && y <= EDGE_BUFFER -> EdgeType.TOP_LEFT
            x >= screenWidth - EDGE_BUFFER && y <= EDGE_BUFFER -> EdgeType.TOP_RIGHT
            x <= EDGE_BUFFER && y >= screenHeight - EDGE_BUFFER -> EdgeType.BOTTOM_LEFT
            x >= screenWidth - EDGE_BUFFER && y >= screenHeight - EDGE_BUFFER -> EdgeType.BOTTOM_RIGHT
            x <= EDGE_BUFFER -> EdgeType.LEFT
            x >= screenWidth - EDGE_BUFFER -> EdgeType.RIGHT
            y <= EDGE_BUFFER -> EdgeType.TOP
            y >= screenHeight - EDGE_BUFFER -> EdgeType.BOTTOM
            else -> EdgeType.NONE
        }
        
        val isAtEdge = edgeType != EdgeType.NONE
        
        // Calculate bounce vector
        val bounceVector = when (edgeType) {
            EdgeType.LEFT -> Offset(BOUNCE_FACTOR * 10, 0f)
            EdgeType.RIGHT -> Offset(-BOUNCE_FACTOR * 10, 0f)
            EdgeType.TOP -> Offset(0f, BOUNCE_FACTOR * 10)
            EdgeType.BOTTOM -> Offset(0f, -BOUNCE_FACTOR * 10)
            EdgeType.TOP_LEFT -> Offset(BOUNCE_FACTOR * 7, BOUNCE_FACTOR * 7)
            EdgeType.TOP_RIGHT -> Offset(-BOUNCE_FACTOR * 7, BOUNCE_FACTOR * 7)
            EdgeType.BOTTOM_LEFT -> Offset(BOUNCE_FACTOR * 7, -BOUNCE_FACTOR * 7)
            EdgeType.BOTTOM_RIGHT -> Offset(-BOUNCE_FACTOR * 7, -BOUNCE_FACTOR * 7)
            else -> Offset.Zero
        }
        
        // Update edge state
        if (isAtEdge && !this.isAtEdge) {
            lastEdgeTime = timestamp
            bounceVelocityX = bounceVector.x
            bounceVelocityY = bounceVector.y
        }
        this.isAtEdge = isAtEdge
        
        return EdgeDetectionResult(isAtEdge, edgeType, bounceVector)
    }
    
    /**
     * Apply edge constraints with bounce-back effect
     */
    private fun applyEdgeConstraints(
        intendedX: Float, 
        intendedY: Float, 
        edgeResult: EdgeDetectionResult
    ): Pair<Float, Float> {
        var finalX = intendedX
        var finalY = intendedY
        
        // Apply bounce-back velocity decay
        val currentTime = System.nanoTime()
        val timeSinceEdge = (currentTime - lastEdgeTime) / 1_000_000f // Convert to milliseconds
        val velocityDecay = kotlin.math.exp(-timeSinceEdge * 0.005f) // Exponential decay
        
        // Apply bounce velocity if recently hit edge
        if (timeSinceEdge < 500f && (abs(bounceVelocityX) > 0.1f || abs(bounceVelocityY) > 0.1f)) {
            finalX += bounceVelocityX * velocityDecay
            finalY += bounceVelocityY * velocityDecay
        }
        
        // Hard constraints - ensure cursor stays within screen bounds
        finalX = finalX.coerceIn(0f, screenWidth.toFloat())
        finalY = finalY.coerceIn(0f, screenHeight.toFloat())
        
        // Apply edge resistance for smoother feel
        if (edgeResult.isAtEdge) {
            val resistanceFactor = when (edgeResult.edgeType) {
                EdgeType.LEFT, EdgeType.RIGHT -> if (abs(intendedX - currentX) > abs(intendedY - currentY)) EDGE_RESISTANCE else 1f
                EdgeType.TOP, EdgeType.BOTTOM -> if (abs(intendedY - currentY) > abs(intendedX - currentX)) EDGE_RESISTANCE else 1f
                else -> EDGE_RESISTANCE
            }
            
            finalX = currentX + (finalX - currentX) * resistanceFactor
            finalY = currentY + (finalY - currentY) * resistanceFactor
        }
        
        return Pair(finalX, finalY)
    }
    
    fun getCursorCenterOffset(type: CursorType, cursorWidth: Int, cursorHeight: Int): Pair<Float, Float> {
        return when (type) {
            is CursorType.Hand -> {
                Pair(cursorWidth * HAND_CURSOR_CENTER_X, cursorHeight * HAND_CURSOR_CENTER_Y)
            }
            else -> {
                Pair(cursorWidth * ROUND_CURSOR_CENTER_X, cursorHeight * ROUND_CURSOR_CENTER_Y)
            }
        }
    }
    
    /**
     * FIX: Added cleanup method for resource management
     */
    fun dispose() {
        synchronized(positionLock) {
            alphaAverage.cleanup()
            betaAverage.cleanup()
            gammaAverage.cleanup()
            
            // Reset filter
            cursorFilter.reset()
            
            // Reset all values
            previousAlpha = 0f
            previousBeta = 0f
            previousGamma = 0f
            currentX = 0f
            currentY = 0f
            startX = 0f
            startY = 0f
            lastProcessTime = 0L
        }
    }
    
    data class PositionResult(
        val x: Float,
        val y: Float,
        val distance: Float,
        val moved: Boolean,
        val edgeDetected: Boolean = false,
        val edgeType: EdgeType = EdgeType.NONE
    )
}

/**
 * Moving average calculator for smoothing sensor input
 * FIX: Added cleanup method to prevent memory leaks
 */
class MovingAverage(
    private val windowSize: Int,
    private val timeWindowNs: Long
) {
    private val values = FloatArray(windowSize)
    private val timestamps = LongArray(windowSize)
    private var index = 0
    private var count = 0
    
    // FIX: Thread-safe access
    private val lock = Any()
    
    fun getAvg(value: Float, timestamp: Long): Float {
        synchronized(lock) {
            values[index] = value
            timestamps[index] = timestamp
            index = (index + 1) % windowSize
            if (count < windowSize) count++
            
            var sum = 0f
            var validCount = 0
            val cutoffTime = timestamp - timeWindowNs
            
            for (i in 0 until count) {
                if (timestamps[i] >= cutoffTime) {
                    sum += values[i]
                    validCount++
                }
            }
            
            return if (validCount > 0) sum / validCount else value
        }
    }
    
    fun reset() {
        synchronized(lock) {
            index = 0
            count = 0
            values.fill(0f)
            timestamps.fill(0L)
        }
    }
    
    /**
     * FIX: Added cleanup method to free resources
     */
    fun cleanup() {
        synchronized(lock) {
            values.fill(0f)
            timestamps.fill(0L)
            index = 0
            count = 0
        }
    }
}