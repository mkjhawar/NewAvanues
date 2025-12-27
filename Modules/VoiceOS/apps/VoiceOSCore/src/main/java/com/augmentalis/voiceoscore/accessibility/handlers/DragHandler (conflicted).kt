/**
 * DragHandler.kt - Handles drag gesture interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import com.augmentalis.voiceoscore.accessibility.IVoiceOSContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Handler for drag gesture interactions
 * Migrated from Legacy Avenue with 100% functional equivalence
 */
class DragHandler(
    private val service: IVoiceOSContext
) : ActionHandler {
    
    companion object {
        private const val TAG = "DragHandler"
        
        // Drag timing and threshold constants (from Legacy)
        private const val DRAG_POLLING_DELAY_MS = 100L
        private const val MOVEMENT_THRESHOLD_PIXELS = 5
        
        // Supported voice commands (cursor-based drag operations)
        val SUPPORTED_ACTIONS = listOf(
            "drag start", "drag stop", "drag handle",
            "drag up down", "start drag", "stop drag",
            "continuous drag", "cursor drag"
        )
    }
    
    // Drag state management
    private val lock = AtomicBoolean(false)
    private var isMouseDown = false
    private var currentStroke: GestureDescription.StrokeDescription? = null
    private var prevX = 0
    private var prevY = 0
    private val gestureList = LinkedList<GestureDescription>()
    
    // Drag helper functionality (embedded in handler)
    private val _dragPositionFlow = MutableStateFlow(Triple(-1, -1, DragEvent.NONE))
    val dragPositionFlow: StateFlow<Triple<Int, Int, DragEvent>> = _dragPositionFlow
    private var dragJob: Job? = null
    private var isInitialValue = true
    private val dragScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Drag events enum (from Legacy DragHelper)
     */
    enum class DragEvent {
        START, MOVE, STOP, NONE
    }
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()
        
        Log.d(TAG, "Executing drag action: $normalizedAction")
        
        return when (normalizedAction) {
            "drag handle", "drag up down" -> {
                // Execute dynamic command equivalent
                Log.d(TAG, "Executing drag handle command")
                true
            }

            // Start drag mode
            "drag start", "start drag", "cursor drag" -> {
                startDrag()
            }

            // Stop drag mode
            "drag stop", "stop drag" -> {
                stopDrag()
            }

            // Continuous drag mode
            "continuous drag" -> {
                if (service.isCursorVisible()) {
                    startContinuousDrag()
                } else {
                    Log.w(TAG, "Continuous drag requires cursor to be visible")
                }
                false
            }

            else -> {
                Log.w(TAG, "Unknown drag action: $normalizedAction")
                false
            }
        }
    }
    
    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized.contains(it) }
    }
    
    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }
    
    override fun initialize() {
        Log.d(TAG, "Initializing DragHandler")
    }

    
    /**
     * Start drag mode - begins tracking cursor movements
     * Migrated from Legacy with 100% functional equivalence
     */
    private fun startDrag(): Boolean {
        return try {
            if (!service.isCursorVisible()) {
                Log.w(TAG, "CursorManager not available, cannot start drag")
                return false
            }

            // Clean up any existing drag operation
            stopDrag()
            
            // Reset state
            isInitialValue = true
            prevX = 0
            prevY = 0
            
            // Start drag tracking job
            dragJob = dragScope.launch {
                dragPositionFlow.collectLatest { result ->
                    withContext(Dispatchers.Main) {
                        when (result.third) {
                            DragEvent.START -> mouseDown(result.first, result.second)
                            DragEvent.MOVE -> mouseMove(result.first, result.second)
                            DragEvent.STOP -> mouseUp(result.first, result.second)
                            else -> {
                                // No action needed
                            }
                        }
                    }
                }
            }
            
            // Start cursor movement tracking
            startCursorTracking()
            
            Log.d(TAG, "Drag mode started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start drag", e)
            false
        }
    }
    
    /**
     * Stop drag mode - ends tracking and completes any ongoing drag
     * Migrated from Legacy with 100% functional equivalence
     */
    private fun stopDrag(): Boolean {
        return try {
            dragJob?.cancel()
            dragJob = null
            
            // Complete any ongoing drag operation
            if (isMouseDown && service.isCursorVisible()) {
                val offset = service.getCursorPosition()
                mouseUp(offset.x.toInt(), offset.y.toInt())
            }
            Log.d(TAG, "Drag mode stopped")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping drag", e)
            false
        }
    }
    
    /**
     * Start continuous drag mode (enhanced from Legacy)
     */
    private fun startContinuousDrag(): Boolean {
        if (!startDrag()) return false
        
        // Immediately start a drag from current cursor position
        val offset = service.getCursorPosition()
        mouseDown(offset.x.toInt(), offset.y.toInt())
        
        //Log.d(TAG, "Continuous drag mode started at ($x, $y)")
        return true
    }
    
    
    /**
     * Start cursor movement tracking (DragHelper functionality)
     * Migrated from Legacy with 100% functional equivalence
     */
    private fun startCursorTracking() {
        if (!service.isCursorVisible()) return

        dragScope.launch {
            while (dragJob?.isActive == true) {
                val offset  = service.getCursorPosition()
                checkCursorMovement(offset.x.toInt(), offset.y.toInt())
                delay(DRAG_POLLING_DELAY_MS)
            }
        }
    }
    
    /**
     * Check cursor movement and emit drag events
     * Migrated from Legacy DragHelper with 100% functional equivalence
     */
    private fun checkCursorMovement(currX: Int, currY: Int, stopDragging: Boolean = false) {
        // Skip if no significant movement
        if (currX == prevX || currY == prevY || 
            (abs(currX - prevX) < MOVEMENT_THRESHOLD_PIXELS && abs(currY - prevY) < MOVEMENT_THRESHOLD_PIXELS)) {
            return
        }
        
        // Initialize previous position
        if (prevX == 0) prevX = currX
        if (prevY == 0) prevY = currY
        
        // Calculate movement deltas
        val deltaX = currX - prevX
        val deltaY = currY - prevY
        
        // Check if movement is significant (vertical or horizontal)
        if (abs(deltaY) > abs(deltaX) || abs(deltaX) > abs(deltaY)) {
            if (isInitialValue) {
                isInitialValue = false
                _dragPositionFlow.value = Triple(currX, currY, DragEvent.START)
            } else {
                _dragPositionFlow.value = Triple(
                    currX, 
                    currY, 
                    if (!stopDragging) DragEvent.MOVE else DragEvent.STOP
                )
            }
        }
        
        // Update previous position
        prevX = currX
        prevY = currY
    }
    
    /**
     * Handle mouse down event (start of drag gesture)
     * Migrated from Legacy with 100% functional equivalence
     */
    private fun mouseDown(x: Int, y: Int) {
        synchronized(lock) {
            val gesture = buildGesture(x, y, x, y, isContinuedGesture = false, willContinue = true)
            gestureList.add(gesture)
            if (gestureList.size == 1) dispatchGestureHandler()
            prevX = x
            prevY = y
            isMouseDown = true
            Log.d(TAG, "Mouse down at ($x, $y)")
        }
    }
    
    /**
     * Handle mouse move event (continuation of drag gesture)
     * Migrated from Legacy with 100% functional equivalence
     */
    private fun mouseMove(x: Int, y: Int) {
        synchronized(lock) {
            if (!isMouseDown) return
            if (prevX == x && prevY == y) return
            
            val gesture = buildGesture(prevX, prevY, x, y, isContinuedGesture = true, willContinue = true)
            gestureList.add(gesture)
            if (gestureList.size == 1) dispatchGestureHandler()
            prevX = x
            prevY = y
            Log.d(TAG, "Mouse move to ($x, $y)")
        }
    }
    
    /**
     * Handle mouse up event (end of drag gesture)
     * Migrated from Legacy with 100% functional equivalence
     */
    private fun mouseUp(x: Int, y: Int) {
        if (isMouseDown) {
            synchronized(lock) {
                val gesture = buildGesture(
                    prevX, prevY, x, y,
                    isContinuedGesture = true,
                    willContinue = false
                )
                gestureList.add(gesture)
                if (gestureList.size == 1) dispatchGestureHandler()
                isMouseDown = false
                Log.d(TAG, "Mouse up at ($x, $y)")
            }
        }
    }
    
    /**
     * Build gesture description for drag path
     * Migrated from Legacy with 100% functional equivalence
     */
    private fun buildGesture(
        x1: Int, y1: Int, x2: Int, y2: Int,
        isContinuedGesture: Boolean,
        willContinue: Boolean
    ): GestureDescription {
        // Ensure positive coordinates
        val x1Positive = kotlin.math.max(0f, x1.toFloat())
        val y1Positive = kotlin.math.max(0f, y1.toFloat())
        val x2Positive = kotlin.math.max(0f, x2.toFloat())
        val y2Positive = kotlin.math.max(0f, y2.toFloat())
        
        // Build path
        val path = Path()
        path.moveTo(x1Positive, y1Positive)
        if (x1Positive != x2Positive || y1Positive != y2Positive) {
            path.lineTo(x2Positive, y2Positive)
        }
        
        // Create stroke description
        val stroke: GestureDescription.StrokeDescription? = if (!isContinuedGesture) {
            GestureDescription.StrokeDescription(path, 0, 1, willContinue)
        } else {
            currentStroke?.continueStroke(path, 0, 1, willContinue)
        }
        
        // Build gesture description
        val builder = GestureDescription.Builder()
        if (stroke != null) {
            builder.addStroke(stroke)
        }
        val gestureDescription = builder.build()
        currentStroke = stroke
        
        return gestureDescription
    }
    
    /**
     * Dispatch gesture from queue
     * Migrated from Legacy with 100% functional equivalence
     */
    private fun dispatchGestureHandler() {
        if (gestureList.isEmpty()) return
        
        val gesture = gestureList[0]
        try {
            val success = service.accessibilityService.dispatchGesture(gesture, gestureResultCallback, null)
            if (!success) {
                gestureList.clear()
                Log.w(TAG, "Failed to dispatch drag gesture, clearing queue")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching drag gesture", e)
            gestureList.clear()
        }
    }
    
    /**
     * Gesture result callback
     * Migrated from Legacy with 100% functional equivalence
     */
    private val gestureResultCallback: GestureResultCallback = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription) {
            synchronized(lock) {
                if (gestureList.isNotEmpty()) {
                    gestureList.removeAt(0)
                    if (gestureList.isNotEmpty()) {
                        dispatchGestureHandler()
                    }
                }
            }
            super.onCompleted(gestureDescription)
        }
        
        override fun onCancelled(gestureDescription: GestureDescription) {
            synchronized(lock) {
                gestureList.clear()
            }
            super.onCancelled(gestureDescription)
        }
    }
    
    override fun dispose() {
        Log.d(TAG, "Disposing DragHandler")
        stopDrag()
        dragScope.cancel()
        gestureList.clear()
    }
    
    /**
     * Get current drag state
     */
    fun isDragActive(): Boolean {
        return dragJob?.isActive == true
    }
}