/**
 * GestureManager.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/core/GestureManager.kt
 * 
 * Created: 2025-01-26 02:15 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Handles gesture recognition and processing for VoiceCursor
 * Module: VoiceCursor System
 * 
 * Features:
 * - Swipe gesture detection (4 directions)
 * - Pinch to zoom gesture support
 * - Drag gesture recognition with visual feedback
 * - Touch event processing and filtering
 * - Gesture state management
 */

package com.augmentalis.voiceos.cursor.core

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import kotlin.math.*

/**
 * Gesture types supported by the cursor system
 */
enum class GestureType {
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    PINCH_IN,
    PINCH_OUT,
    DRAG_START,
    DRAG_MOVE,
    DRAG_END,
    TAP,
    LONG_PRESS,
    DOUBLE_TAP,
    UNKNOWN
}

/**
 * Gesture event data
 */
data class GestureEvent(
    val type: GestureType,
    val startPosition: CursorOffset,
    val currentPosition: CursorOffset,
    val endPosition: CursorOffset = currentPosition,
    val velocity: Float = 0f,
    val scaleFactor: Float = 1f,
    val distance: Float = 0f,
    val duration: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Gesture configuration settings
 */
data class GestureConfig(
    val swipeMinDistance: Float = 100f,
    val swipeMinVelocity: Float = 300f,
    val pinchMinSpan: Float = 50f,
    val dragMinDistance: Float = 10f,
    val longPressTimeout: Long = 500L,
    val doubleTapTimeout: Long = 300L,
    val enableSwipeGestures: Boolean = true,
    val enablePinchGestures: Boolean = true,
    val enableDragGestures: Boolean = true,
    val enableTapGestures: Boolean = true
)

/**
 * Current gesture state
 */
data class GestureState(
    val isActive: Boolean = false,
    val currentGesture: GestureType = GestureType.UNKNOWN,
    val startTime: Long = 0L,
    val startPosition: CursorOffset = CursorOffset(0f, 0f),
    val currentPosition: CursorOffset = CursorOffset(0f, 0f),
    val isDragging: Boolean = false,
    val isScaling: Boolean = false,
    val currentScale: Float = 1f,
    val accumulatedDistance: Float = 0f
)

/**
 * Main gesture manager class
 */
class GestureManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GestureManager"
        private const val MIN_FLING_VELOCITY = 150f
        private const val MIN_SWIPE_DISTANCE = 50f
        private const val DRAG_THRESHOLD = 20f
    }
    
    // Configuration
    var config = GestureConfig()
    
    // Current state
    @Volatile
    private var gestureState = GestureState()
    
    // System configurations
    private val viewConfiguration = ViewConfiguration.get(context)
    private val touchSlop = viewConfiguration.scaledTouchSlop.toFloat()
    private val minimumFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity.toFloat()
    private val maximumFlingVelocity = viewConfiguration.scaledMaximumFlingVelocity.toFloat()
    
    // Gesture detectors
    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector
    
    // Event callbacks
    var onGestureEvent: ((GestureEvent) -> Unit)? = null
    var onGestureStateChanged: ((GestureState) -> Unit)? = null
    
    // Touch tracking
    private var lastDownTime = 0L
    private var lastTapTime = 0L
    private var tapCount = 0
    
    init {
        // Initialize gesture detector
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            
            override fun onDown(e: MotionEvent): Boolean {
                lastDownTime = e.eventTime
                updateGestureState(
                    gestureState.copy(
                        isActive = true,
                        startTime = e.eventTime,
                        startPosition = CursorOffset(e.x, e.y),
                        currentPosition = CursorOffset(e.x, e.y)
                    )
                )
                return true
            }
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!config.enableTapGestures) return false
                
                val currentTime = e.eventTime
                val timeDiff = currentTime - lastTapTime
                
                if (timeDiff < config.doubleTapTimeout && tapCount == 1) {
                    // Double tap detected
                    tapCount = 0
                    handleDoubleTap(e)
                } else {
                    // Single tap
                    tapCount = 1
                    lastTapTime = currentTime
                    handleSingleTap(e)
                }
                return true
            }
            
            override fun onLongPress(e: MotionEvent) {
                if (!config.enableTapGestures) return
                
                val event = GestureEvent(
                    type = GestureType.LONG_PRESS,
                    startPosition = CursorOffset(e.x, e.y),
                    currentPosition = CursorOffset(e.x, e.y),
                    duration = e.eventTime - gestureState.startTime
                )
                
                onGestureEvent?.invoke(event)
            }
            
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!config.enableDragGestures || e1 == null) return false
                
                val currentPos = CursorOffset(e2.x, e2.y)
                val distance = calculateDistance(gestureState.startPosition, currentPos)
                
                if (!gestureState.isDragging && distance > config.dragMinDistance) {
                    // Start dragging
                    updateGestureState(
                        gestureState.copy(
                            isDragging = true,
                            currentGesture = GestureType.DRAG_START,
                            currentPosition = currentPos
                        )
                    )
                    
                    val event = GestureEvent(
                        type = GestureType.DRAG_START,
                        startPosition = gestureState.startPosition,
                        currentPosition = currentPos,
                        distance = distance,
                        duration = e2.eventTime - gestureState.startTime
                    )
                    
                    onGestureEvent?.invoke(event)
                } else if (gestureState.isDragging) {
                    // Continue dragging
                    updateGestureState(
                        gestureState.copy(
                            currentGesture = GestureType.DRAG_MOVE,
                            currentPosition = currentPos,
                            accumulatedDistance = gestureState.accumulatedDistance + sqrt(distanceX * distanceX + distanceY * distanceY)
                        )
                    )
                    
                    val event = GestureEvent(
                        type = GestureType.DRAG_MOVE,
                        startPosition = gestureState.startPosition,
                        currentPosition = currentPos,
                        distance = distance,
                        duration = e2.eventTime - gestureState.startTime
                    )
                    
                    onGestureEvent?.invoke(event)
                }
                
                return true
            }
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (!config.enableSwipeGestures || e1 == null) return false
                
                val startX = e1.x
                val startY = e1.y
                val endX = e2.x
                val endY = e2.y
                
                val deltaX = endX - startX
                val deltaY = endY - startY
                val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
                val velocity = sqrt(velocityX * velocityX + velocityY * velocityY)
                
                // Check if this qualifies as a swipe
                if (distance < config.swipeMinDistance || velocity < config.swipeMinVelocity) {
                    return false
                }
                
                // Determine swipe direction
                val gestureType = determineSwipeDirection(deltaX, deltaY, abs(velocityX), abs(velocityY))
                
                if (gestureType != GestureType.UNKNOWN) {
                    val event = GestureEvent(
                        type = gestureType,
                        startPosition = CursorOffset(startX, startY),
                        currentPosition = CursorOffset(endX, endY),
                        endPosition = CursorOffset(endX, endY),
                        velocity = velocity,
                        distance = distance,
                        duration = e2.eventTime - e1.eventTime
                    )
                    
                    onGestureEvent?.invoke(event)
                    return true
                }
                
                return false
            }
        })
        
        // Initialize scale gesture detector
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                if (!config.enablePinchGestures) return false
                
                updateGestureState(
                    gestureState.copy(
                        isScaling = true,
                        currentScale = detector.scaleFactor
                    )
                )
                
                return true
            }
            
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!config.enablePinchGestures) return false
                
                val scaleFactor = detector.scaleFactor
                val gestureType = if (scaleFactor > 1f) GestureType.PINCH_OUT else GestureType.PINCH_IN
                
                updateGestureState(
                    gestureState.copy(
                        currentScale = scaleFactor,
                        currentGesture = gestureType
                    )
                )
                
                val event = GestureEvent(
                    type = gestureType,
                    startPosition = CursorOffset(detector.focusX, detector.focusY),
                    currentPosition = CursorOffset(detector.focusX, detector.focusY),
                    scaleFactor = scaleFactor,
                    duration = System.currentTimeMillis() - gestureState.startTime
                )
                
                onGestureEvent?.invoke(event)
                return true
            }
            
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                updateGestureState(
                    gestureState.copy(
                        isScaling = false,
                        currentScale = 1f
                    )
                )
            }
        })
    }
    
    /**
     * Process touch event through gesture detectors
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false
        
        try {
            // Update current position for all events
            if (gestureState.isActive) {
                updateGestureState(
                    gestureState.copy(
                        currentPosition = CursorOffset(event.x, event.y)
                    )
                )
            }
            
            // Process through scale detector first
            handled = scaleGestureDetector.onTouchEvent(event) || handled
            
            // Process through regular gesture detector if not scaling
            if (!gestureState.isScaling) {
                handled = gestureDetector.onTouchEvent(event) || handled
            }
            
            // Handle touch up events
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                handleTouchUp(event)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing touch event", e)
        }
        
        return handled
    }
    
    /**
     * Handle touch up events
     */
    private fun handleTouchUp(event: MotionEvent) {
        if (gestureState.isDragging) {
            // End drag gesture
            val gestureEvent = GestureEvent(
                type = GestureType.DRAG_END,
                startPosition = gestureState.startPosition,
                currentPosition = CursorOffset(event.x, event.y),
                endPosition = CursorOffset(event.x, event.y),
                distance = gestureState.accumulatedDistance,
                duration = event.eventTime - gestureState.startTime
            )
            
            onGestureEvent?.invoke(gestureEvent)
        }
        
        // Reset gesture state
        updateGestureState(
            GestureState(
                isActive = false,
                currentGesture = GestureType.UNKNOWN,
                isDragging = false,
                isScaling = false,
                currentScale = 1f,
                accumulatedDistance = 0f
            )
        )
    }
    
    /**
     * Handle single tap
     */
    private fun handleSingleTap(event: MotionEvent) {
        val gestureEvent = GestureEvent(
            type = GestureType.TAP,
            startPosition = CursorOffset(event.x, event.y),
            currentPosition = CursorOffset(event.x, event.y),
            duration = event.eventTime - lastDownTime
        )
        
        onGestureEvent?.invoke(gestureEvent)
    }
    
    /**
     * Handle double tap
     */
    private fun handleDoubleTap(event: MotionEvent) {
        val gestureEvent = GestureEvent(
            type = GestureType.DOUBLE_TAP,
            startPosition = CursorOffset(event.x, event.y),
            currentPosition = CursorOffset(event.x, event.y),
            duration = event.eventTime - lastDownTime
        )
        
        onGestureEvent?.invoke(gestureEvent)
    }
    
    /**
     * Determine swipe direction based on deltas and velocities
     */
    private fun determineSwipeDirection(
        deltaX: Float,
        deltaY: Float,
        velocityX: Float,
        velocityY: Float
    ): GestureType {
        val absDeltaX = abs(deltaX)
        val absDeltaY = abs(deltaY)
        
        // Check if horizontal or vertical swipe
        return if (absDeltaX > absDeltaY) {
            // Horizontal swipe
            if (deltaX > 0 && velocityX > minimumFlingVelocity) {
                GestureType.SWIPE_RIGHT
            } else if (deltaX < 0 && velocityX > minimumFlingVelocity) {
                GestureType.SWIPE_LEFT
            } else {
                GestureType.UNKNOWN
            }
        } else {
            // Vertical swipe
            if (deltaY > 0 && velocityY > minimumFlingVelocity) {
                GestureType.SWIPE_DOWN
            } else if (deltaY < 0 && velocityY > minimumFlingVelocity) {
                GestureType.SWIPE_UP
            } else {
                GestureType.UNKNOWN
            }
        }
    }
    
    /**
     * Update gesture state and notify listeners
     */
    private fun updateGestureState(newState: GestureState) {
        gestureState = newState
        onGestureStateChanged?.invoke(newState)
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(start: CursorOffset, end: CursorOffset): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Get current gesture state
     */
    fun getCurrentState(): GestureState = gestureState
    
    /**
     * Check if gesture is currently active
     */
    fun isGestureActive(): Boolean = gestureState.isActive
    
    /**
     * Check if currently dragging
     */
    fun isDragging(): Boolean = gestureState.isDragging
    
    /**
     * Check if currently scaling
     */
    fun isScaling(): Boolean = gestureState.isScaling
    
    /**
     * Update gesture configuration
     */
    fun updateConfig(newConfig: GestureConfig) {
        config = newConfig
    }
    
    /**
     * Enable/disable specific gesture types
     */
    fun setSwipeGesturesEnabled(enabled: Boolean) {
        config = config.copy(enableSwipeGestures = enabled)
    }
    
    fun setPinchGesturesEnabled(enabled: Boolean) {
        config = config.copy(enablePinchGestures = enabled)
    }
    
    fun setDragGesturesEnabled(enabled: Boolean) {
        config = config.copy(enableDragGestures = enabled)
    }
    
    fun setTapGesturesEnabled(enabled: Boolean) {
        config = config.copy(enableTapGestures = enabled)
    }
    
    /**
     * Reset gesture state
     */
    fun reset() {
        updateGestureState(GestureState())
        tapCount = 0
        lastTapTime = 0L
        lastDownTime = 0L
    }
    
    /**
     * Dispose of resources
     */
    fun dispose() {
        reset()
        onGestureEvent = null
        onGestureStateChanged = null
    }
}