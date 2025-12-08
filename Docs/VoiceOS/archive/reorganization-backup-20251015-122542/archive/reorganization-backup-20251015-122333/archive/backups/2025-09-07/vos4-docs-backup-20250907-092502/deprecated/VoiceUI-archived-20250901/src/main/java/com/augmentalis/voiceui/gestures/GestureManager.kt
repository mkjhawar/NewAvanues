/**
 * GestureManager.kt - Advanced gesture recognition and management system
 * Adapted from VOS2 VoiceToTouchBridge with enhancements for VOS4
 */

package com.augmentalis.voiceui.gestures

import android.content.Context
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.math.*

/**
 * Advanced gesture management system supporting:
 * - Multi-touch gestures
 * - Air tap (for AR glasses)
 * - Force touch
 * - Custom gesture patterns
 * - Voice-to-gesture mapping
 */
class GestureManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "GestureManager"
        private const val GESTURE_THRESHOLD = 100f
        private const val LONG_PRESS_DURATION = 500L
        private const val DOUBLE_TAP_TIMEOUT = 300L
        private const val FORCE_TOUCH_THRESHOLD = 0.7f
        private const val AIR_TAP_Z_THRESHOLD = 50f
    }
    
    // Gesture types
    enum class GestureType {
        TAP,
        DOUBLE_TAP,
        LONG_PRESS,
        SWIPE_LEFT,
        SWIPE_RIGHT,
        SWIPE_UP,
        SWIPE_DOWN,
        PINCH_IN,
        PINCH_OUT,
        ROTATE_CW,
        ROTATE_CCW,
        TWO_FINGER_TAP,
        THREE_FINGER_TAP,
        FORCE_TOUCH,
        AIR_TAP,
        CUSTOM_PATTERN,
        DRAG,
        FLING
    }
    
    // Gesture event data
    data class GestureEvent(
        val id: String = UUID.randomUUID().toString(),
        val type: GestureType,
        val startPoint: PointF,
        val endPoint: PointF? = null,
        val velocity: Float = 0f,
        val pressure: Float = 1f,
        val fingerCount: Int = 1,
        val duration: Long = 0,
        val customData: Map<String, Any> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // Gesture recognizer configuration
    data class GestureConfig(
        val type: String = "all",
        val enabled: Boolean = true,
        val sensitivity: Float = 1.0f,
        val enableMultiTouch: Boolean = true,
        val enableForceTouch: Boolean = true,
        val enableAirTap: Boolean = false,
        val enableCustomPatterns: Boolean = true,
        val sensitivityLevel: Float = 1.0f,
        val hapticFeedback: Boolean = true
    )
    
    // State management
    private var config = GestureConfig()
    private val configs = mutableMapOf<String, GestureConfig>()
    private var isEnabled = true
    private val _gestureEvents = MutableSharedFlow<GestureEvent>()
    val gestureEvents: SharedFlow<GestureEvent> = _gestureEvents.asSharedFlow()
    val gestureFlow: SharedFlow<GestureEvent> get() = gestureEvents
    
    // Custom gesture library
    private var customGestureLibrary: CustomGestureLibrary? = null
    private val registeredPatterns = mutableMapOf<String, (GestureEvent) -> Unit>()
    
    // Touch tracking
    private val activeTouches = mutableMapOf<Int, PointF>()
    private var lastTapTime = 0L
    private var lastTapLocation: PointF? = null
    
    init {
        loadCustomGestures()
    }
    
    /**
     * Configure gesture recognition settings
     */
    fun configure(config: GestureConfig) {
        this.config = config
        Log.d(TAG, "Gesture configuration updated: $config")
    }
    
    /**
     * Register a custom gesture pattern
     */
    fun registerCustomPattern(
        patternName: String,
        onRecognized: (GestureEvent) -> Unit
    ) {
        registeredPatterns[patternName] = onRecognized
        Log.d(TAG, "Registered custom pattern: $patternName")
    }
    
    /**
     * Process touch event and recognize gestures
     */
    fun processTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleTouchDown(event)
            MotionEvent.ACTION_MOVE -> handleTouchMove(event)
            MotionEvent.ACTION_UP -> handleTouchUp(event)
            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(event)
            MotionEvent.ACTION_POINTER_UP -> handlePointerUp(event)
            MotionEvent.ACTION_CANCEL -> handleTouchCancel(event)
        }
        return true
    }
    
    /**
     * Process air tap gesture (for AR glasses)
     */
    fun processAirTap(position: Offset, depth: Float) {
        if (!config.enableAirTap) return
        
        if (depth < AIR_TAP_Z_THRESHOLD) {
            scope.launch {
                _gestureEvents.emit(
                    GestureEvent(
                        type = GestureType.AIR_TAP,
                        startPoint = PointF(position.x, position.y),
                        customData = mapOf("depth" to depth)
                    )
                )
            }
        }
    }
    
    /**
     * Process force touch gesture
     */
    fun processForceTouch(position: PointF, pressure: Float) {
        if (!config.enableForceTouch) return
        
        if (pressure > FORCE_TOUCH_THRESHOLD) {
            scope.launch {
                _gestureEvents.emit(
                    GestureEvent(
                        type = GestureType.FORCE_TOUCH,
                        startPoint = position,
                        pressure = pressure
                    )
                )
            }
        }
    }
    
    /**
     * Convert voice command to gesture
     */
    suspend fun voiceToGesture(command: String, targetView: View? = null): GestureEvent? {
        val normalizedCommand = command.lowercase().trim()
        
        val gestureType = when {
            normalizedCommand.contains("tap") || normalizedCommand.contains("click") -> GestureType.TAP
            normalizedCommand.contains("double tap") -> GestureType.DOUBLE_TAP
            normalizedCommand.contains("long press") || normalizedCommand.contains("hold") -> GestureType.LONG_PRESS
            normalizedCommand.contains("swipe left") -> GestureType.SWIPE_LEFT
            normalizedCommand.contains("swipe right") -> GestureType.SWIPE_RIGHT
            normalizedCommand.contains("swipe up") -> GestureType.SWIPE_UP
            normalizedCommand.contains("swipe down") -> GestureType.SWIPE_DOWN
            normalizedCommand.contains("pinch in") || normalizedCommand.contains("zoom out") -> GestureType.PINCH_IN
            normalizedCommand.contains("pinch out") || normalizedCommand.contains("zoom in") -> GestureType.PINCH_OUT
            normalizedCommand.contains("rotate") && normalizedCommand.contains("clockwise") -> GestureType.ROTATE_CW
            normalizedCommand.contains("rotate") && normalizedCommand.contains("counter") -> GestureType.ROTATE_CCW
            normalizedCommand.contains("drag") -> GestureType.DRAG
            else -> null
        } ?: return null
        
        val centerPoint = targetView?.let {
            val location = IntArray(2)
            it.getLocationOnScreen(location)
            PointF(
                location[0] + it.width / 2f,
                location[1] + it.height / 2f
            )
        } ?: PointF(540f, 960f) // Default center
        
        val gestureEvent = GestureEvent(
            type = gestureType,
            startPoint = centerPoint,
            endPoint = when (gestureType) {
                GestureType.SWIPE_LEFT -> PointF(centerPoint.x - 200, centerPoint.y)
                GestureType.SWIPE_RIGHT -> PointF(centerPoint.x + 200, centerPoint.y)
                GestureType.SWIPE_UP -> PointF(centerPoint.x, centerPoint.y - 200)
                GestureType.SWIPE_DOWN -> PointF(centerPoint.x, centerPoint.y + 200)
                else -> null
            }
        )
        
        _gestureEvents.emit(gestureEvent)
        return gestureEvent
    }
    
    // Private helper methods
    
    private fun handleTouchDown(event: MotionEvent) {
        val pointerId = event.getPointerId(0)
        val point = PointF(event.x, event.y)
        activeTouches[pointerId] = point
        
        // Check for double tap
        val currentTime = System.currentTimeMillis()
        if (lastTapLocation != null && 
            currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT &&
            distance(point, lastTapLocation!!) < GESTURE_THRESHOLD) {
            
            emitGesture(GestureEvent(
                type = GestureType.DOUBLE_TAP,
                startPoint = point
            ))
            lastTapTime = 0
            lastTapLocation = null
        } else {
            lastTapTime = currentTime
            lastTapLocation = point
            
            // Start long press detection
            scope.launch {
                delay(LONG_PRESS_DURATION)
                if (activeTouches.containsKey(pointerId)) {
                    emitGesture(GestureEvent(
                        type = GestureType.LONG_PRESS,
                        startPoint = point,
                        duration = LONG_PRESS_DURATION
                    ))
                }
            }
        }
        
        // Check for force touch
        if (event.pressure > FORCE_TOUCH_THRESHOLD) {
            processForceTouch(point, event.pressure)
        }
    }
    
    private fun handleTouchMove(event: MotionEvent) {
        // Update touch positions
        for (i in 0 until event.pointerCount) {
            val pointerId = event.getPointerId(i)
            activeTouches[pointerId] = PointF(event.getX(i), event.getY(i))
        }
        
        // Detect pinch/zoom
        if (activeTouches.size == 2) {
            detectPinchGesture()
        }
    }
    
    private fun handleTouchUp(event: MotionEvent) {
        val pointerId = event.getPointerId(0)
        val startPoint = activeTouches[pointerId] ?: return
        val endPoint = PointF(event.x, event.y)
        
        val distance = distance(startPoint, endPoint)
        val duration = event.eventTime - event.downTime
        
        when {
            distance < GESTURE_THRESHOLD && duration < LONG_PRESS_DURATION -> {
                // Simple tap
                emitGesture(GestureEvent(
                    type = GestureType.TAP,
                    startPoint = startPoint,
                    fingerCount = activeTouches.size
                ))
            }
            distance >= GESTURE_THRESHOLD -> {
                // Swipe gesture
                val angle = atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x)
                val gestureType = when {
                    abs(angle) < PI / 4 -> GestureType.SWIPE_RIGHT
                    abs(angle) > 3 * PI / 4 -> GestureType.SWIPE_LEFT
                    angle > 0 -> GestureType.SWIPE_DOWN
                    else -> GestureType.SWIPE_UP
                }
                
                emitGesture(GestureEvent(
                    type = gestureType,
                    startPoint = startPoint,
                    endPoint = endPoint,
                    velocity = distance / duration,
                    duration = duration
                ))
            }
        }
        
        activeTouches.remove(pointerId)
    }
    
    private fun handlePointerDown(event: MotionEvent) {
        val index = event.actionIndex
        val pointerId = event.getPointerId(index)
        activeTouches[pointerId] = PointF(event.getX(index), event.getY(index))
        
        // Detect multi-finger taps
        when (activeTouches.size) {
            2 -> emitGesture(GestureEvent(
                type = GestureType.TWO_FINGER_TAP,
                startPoint = PointF(event.getX(index), event.getY(index)),
                fingerCount = 2
            ))
            3 -> emitGesture(GestureEvent(
                type = GestureType.THREE_FINGER_TAP,
                startPoint = PointF(event.getX(index), event.getY(index)),
                fingerCount = 3
            ))
        }
    }
    
    private fun handlePointerUp(event: MotionEvent) {
        val index = event.actionIndex
        val pointerId = event.getPointerId(index)
        activeTouches.remove(pointerId)
    }
    
    private fun handleTouchCancel(event: MotionEvent) {
        activeTouches.clear()
    }
    
    private var previousPinchDistance: Float? = null
    
    private fun detectPinchGesture() {
        if (activeTouches.size != 2) return
        
        val points = activeTouches.values.toList()
        val currentDistance = distance(points[0], points[1])
        
        previousPinchDistance?.let { prevDistance ->
            val scaleFactor = currentDistance / prevDistance
            when {
                scaleFactor > 1.1f -> emitGesture(GestureEvent(
                    type = GestureType.PINCH_OUT,
                    startPoint = points[0],
                    endPoint = points[1],
                    customData = mapOf("scale" to scaleFactor)
                ))
                scaleFactor < 0.9f -> emitGesture(GestureEvent(
                    type = GestureType.PINCH_IN,
                    startPoint = points[0],
                    endPoint = points[1],
                    customData = mapOf("scale" to scaleFactor)
                ))
            }
        }
        
        previousPinchDistance = currentDistance
    }
    
    private fun distance(p1: PointF, p2: PointF): Float {
        return sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
    }
    
    private fun emitGesture(event: GestureEvent) {
        scope.launch {
            _gestureEvents.emit(event)
            
            // Check for registered custom patterns
            registeredPatterns.values.forEach { handler ->
                handler(event)
            }
        }
    }
    
    private fun loadCustomGestures() {
        // Load custom gesture patterns from storage
        try {
            val gestureFile = context.getFileStreamPath("custom_gestures")
            if (gestureFile.exists()) {
                customGestureLibrary = CustomGestureLibrary.fromFile(gestureFile)
                customGestureLibrary?.load()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom gestures", e)
        }
    }
    
    fun shutdown() {
        activeTouches.clear()
        registeredPatterns.clear()
        customGestureLibrary = null
    }
    
    // Additional methods for intent/provider support
    fun enableGesture(gestureType: String, enabled: Boolean) {
        configs[gestureType] = GestureConfig(type = gestureType, enabled = enabled)
    }
    
    fun triggerGesture(gestureType: String, x: Float, y: Float) {
        val type = when(gestureType.uppercase()) {
            "TAP" -> GestureType.TAP
            "SWIPE" -> GestureType.SWIPE_RIGHT
            "PINCH" -> GestureType.PINCH_IN
            "ROTATE" -> GestureType.ROTATE_CW
            else -> GestureType.TAP
        }
        emitGesture(GestureEvent(
            type = type,
            startPoint = PointF(x, y)
        ))
    }
    
    fun processGesture(type: String, x: Float, y: Float) {
        triggerGesture(type, x, y)
    }
    
    fun configure(sensitivity: Float, multiTouch: Boolean) {
        config = config.copy(
            sensitivityLevel = sensitivity,
            enableMultiTouch = multiTouch
        )
    }
    
    fun enableMultiTouch(enabled: Boolean) {
        config = config.copy(enableMultiTouch = enabled)
    }
    
    fun isEnabled(): Boolean = isEnabled
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun getGestureConfigs(): List<GestureConfig> {
        return configs.values.toList().ifEmpty {
            listOf(
                GestureConfig(type = "tap", enabled = true),
                GestureConfig(type = "swipe", enabled = true),
                GestureConfig(type = "pinch", enabled = true)
            )
        }
    }
}

/**
 * Custom gesture library placeholder
 */
internal class CustomGestureLibrary {
    companion object {
        fun fromFile(file: java.io.File): CustomGestureLibrary? {
            return try {
                CustomGestureLibrary()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun load(): Boolean = true
}