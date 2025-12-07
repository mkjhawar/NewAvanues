/**
 * CursorManager.kt - Manages cursor overlay for accessibility
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-27
 * 
 * Manages cursor overlay for precise UI element selection.
 * VOS4 Direct Implementation - No interfaces.
 */
package com.augmentalis.voiceos.accessibility.managers

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import com.augmentalis.voiceaccessibility.R
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import com.augmentalis.voiceos.accessibility.ui.utils.DisplayUtils
import kotlinx.coroutines.*

/**
 * Manages cursor overlay for accessibility navigation
 * Direct implementation following VOS4 patterns
 */
class CursorManager(private val service: VoiceAccessibilityService) {
    
    companion object {
        private const val TAG = "CursorManager"
        private const val DEFAULT_MOVE_STEP = 50
        private const val FAST_MOVE_STEP = 150
        private const val PRECISION_MOVE_STEP = 10
        private const val CURSOR_SIZE = 48 // dp
        private const val ANIMATION_DURATION_MS = 200L
    }
    
    private val windowManager: WindowManager = 
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    private var cursorView: View? = null
    private var isVisible = false
    private var currentX = 0
    private var currentY = 0
    
    private val cursorScope = CoroutineScope(
        Dispatchers.Main + SupervisorJob()
    )
    
    // Movement modes
    enum class MovementMode {
        NORMAL,      // Default movement
        FAST,        // Larger steps
        PRECISION    // Small precise steps
    }
    
    private var movementMode = MovementMode.NORMAL
    
    /**
     * Initialize cursor manager
     */
    fun initialize() {
        Log.d(TAG, "Initializing CursorManager")
        
        // Get display center using modern API
        val point = DisplayUtils.getRealScreenSize(windowManager)
        currentX = point.x / 2
        currentY = point.y / 2
    }
    
    /**
     * Show cursor overlay
     */
    fun showCursor(): Boolean {
        if (isVisible) return true
        
        return try {
            createView()
            isVisible = true
            Log.d(TAG, "Cursor shown at ($currentX, $currentY)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show cursor", e)
            false
        }
    }
    
    /**
     * Hide cursor overlay
     */
    fun hideCursor(): Boolean {
        if (!isVisible) return true
        
        return try {
            cursorView?.let { view ->
                windowManager.removeView(view)
                cursorView = null
            }
            isVisible = false
            Log.d(TAG, "Cursor hidden")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide cursor", e)
            false
        }
    }
    
    /**
     * Toggle cursor visibility
     */
    fun toggleCursor(): Boolean {
        return if (isVisible) hideCursor() else showCursor()
    }
    
    /**
     * Move cursor by relative amount
     */
    fun moveCursor(direction: Direction): Boolean {
        if (!isVisible) {
            showCursor()
        }
        
        val step = when (movementMode) {
            MovementMode.FAST -> FAST_MOVE_STEP
            MovementMode.PRECISION -> PRECISION_MOVE_STEP
            else -> DEFAULT_MOVE_STEP
        }
        
        val (dx, dy) = when (direction) {
            Direction.UP -> Pair(0, -step)
            Direction.DOWN -> Pair(0, step)
            Direction.LEFT -> Pair(-step, 0)
            Direction.RIGHT -> Pair(step, 0)
            Direction.UP_LEFT -> Pair(-step, -step)
            Direction.UP_RIGHT -> Pair(step, -step)
            Direction.DOWN_LEFT -> Pair(-step, step)
            Direction.DOWN_RIGHT -> Pair(step, step)
        }
        
        return moveToPosition(currentX + dx, currentY + dy)
    }
    
    /**
     * Move cursor to absolute position
     */
    fun moveToPosition(x: Int, y: Int): Boolean {
        if (!isVisible) return false
        
        // Get display bounds using modern API
        val point = DisplayUtils.getRealScreenSize(windowManager)
        
        // Clamp to screen bounds
        currentX = x.coerceIn(0, point.x)
        currentY = y.coerceIn(0, point.y)
        
        // Update cursor position
        updateCursorPosition()
        
        Log.d(TAG, "Cursor moved to ($currentX, $currentY)")
        return true
    }
    
    /**
     * Perform click at cursor position
     */
    fun clickAtCursor(): Boolean {
        if (!isVisible) return false
        
        // Use accessibility service to perform click at coordinates
        val rootNode = service.rootInActiveWindow ?: return false
        
        // Find node at cursor position and click it
        val clickableNode = service.findNodeAtCoordinates(rootNode, currentX, currentY)
        return clickableNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
    
    /**
     * Perform long click at cursor position
     */
    fun longClickAtCursor(): Boolean {
        if (!isVisible) return false
        
        val rootNode = service.rootInActiveWindow ?: return false
        val clickableNode = service.findNodeAtCoordinates(rootNode, currentX, currentY)
        return clickableNode?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) ?: false
    }
    
    /**
     * Set movement mode
     */
    fun setMovementMode(mode: MovementMode) {
        movementMode = mode
        Log.d(TAG, "Movement mode set to: $mode")
    }
    
    /**
     * Get current cursor position
     */
    fun getCursorPosition(): Pair<Int, Int> {
        return Pair(currentX, currentY)
    }
    
    /**
     * Check if cursor is visible
     */
    fun isCursorVisible(): Boolean {
        return isVisible
    }
    
    /**
     * Center cursor on screen
     */
    fun centerCursor(): Boolean {
        val point = DisplayUtils.getRealScreenSize(windowManager)
        return moveToPosition(point.x / 2, point.y / 2)
    }
    
    /**
     * Handle cursor command from voice input
     */
    fun handleCursorCommand(command: String): Boolean {
        val normalizedCommand = command.lowercase().trim().removePrefix("cursor ")
        
        Log.d(TAG, "Handling cursor command: $normalizedCommand")
        
        return when {
            // Show/Hide commands
            normalizedCommand in listOf("show", "enable", "on") -> showCursor()
            normalizedCommand in listOf("hide", "disable", "off") -> hideCursor()
            normalizedCommand == "toggle" -> toggleCursor()
            
            // Movement commands
            normalizedCommand == "up" -> moveCursor(Direction.UP)
            normalizedCommand == "down" -> moveCursor(Direction.DOWN)
            normalizedCommand == "left" -> moveCursor(Direction.LEFT)
            normalizedCommand == "right" -> moveCursor(Direction.RIGHT)
            normalizedCommand in listOf("up left", "upper left") -> moveCursor(Direction.UP_LEFT)
            normalizedCommand in listOf("up right", "upper right") -> moveCursor(Direction.UP_RIGHT)
            normalizedCommand in listOf("down left", "lower left") -> moveCursor(Direction.DOWN_LEFT)
            normalizedCommand in listOf("down right", "lower right") -> moveCursor(Direction.DOWN_RIGHT)
            
            // Action commands
            normalizedCommand in listOf("click", "tap", "select") -> clickAtCursor()
            normalizedCommand in listOf("long click", "long press", "hold") -> longClickAtCursor()
            
            // Position commands
            normalizedCommand == "center" -> centerCursor()
            
            // Movement mode commands
            normalizedCommand in listOf("fast", "fast mode") -> {
                setMovementMode(MovementMode.FAST)
                true
            }
            normalizedCommand in listOf("precision", "precise", "precision mode") -> {
                setMovementMode(MovementMode.PRECISION)
                true
            }
            normalizedCommand in listOf("normal", "normal mode", "default") -> {
                setMovementMode(MovementMode.NORMAL)
                true
            }
            
            // Move to specific positions (simple grid system)
            normalizedCommand.startsWith("move to ") -> {
                handleMoveToCommand(normalizedCommand.removePrefix("move to "))
            }
            
            else -> {
                Log.w(TAG, "Unknown cursor command: $normalizedCommand")
                false
            }
        }
    }
    
    /**
     * Handle move to position commands
     */
    private fun handleMoveToCommand(positionCommand: String): Boolean {
        val point = DisplayUtils.getRealScreenSize(windowManager)
        
        val (targetX, targetY) = when (positionCommand.trim()) {
            "top left", "upper left" -> Pair(point.x / 4, point.y / 4)
            "top center", "top", "upper center" -> Pair(point.x / 2, point.y / 4)
            "top right", "upper right" -> Pair(point.x * 3 / 4, point.y / 4)
            "left", "middle left" -> Pair(point.x / 4, point.y / 2)
            "center", "middle" -> Pair(point.x / 2, point.y / 2)
            "right", "middle right" -> Pair(point.x * 3 / 4, point.y / 2)
            "bottom left", "lower left" -> Pair(point.x / 4, point.y * 3 / 4)
            "bottom center", "bottom", "lower center" -> Pair(point.x / 2, point.y * 3 / 4)
            "bottom right", "lower right" -> Pair(point.x * 3 / 4, point.y * 3 / 4)
            else -> {
                Log.w(TAG, "Unknown position: $positionCommand")
                return false
            }
        }
        
        return moveToPosition(targetX, targetY)
    }
    
    /**
     * Create cursor overlay view
     */
    private fun createView() {
        if (cursorView != null) return
        
        // Create cursor view with custom drawable
        val imageView = ImageView(service).apply {
            setImageResource(R.drawable.ic_cursor)
            alpha = 0.8f
        }
        
        val params = WindowManager.LayoutParams().apply {
            width = CURSOR_SIZE
            height = CURSOR_SIZE
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = currentX - CURSOR_SIZE / 2
            y = currentY - CURSOR_SIZE / 2
        }
        
        cursorView = imageView
        windowManager.addView(imageView, params)
    }
    
    /**
     * Update cursor position on screen
     */
    private fun updateCursorPosition() {
        cursorView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            params.x = currentX - CURSOR_SIZE / 2
            params.y = currentY - CURSOR_SIZE / 2
            windowManager.updateViewLayout(view, params)
        }
    }
    
    /**
     * Animate cursor movement
     */
    private fun animateCursorTo(targetX: Int, targetY: Int) {
        cursorScope.launch {
            val startX = currentX
            val startY = currentY
            val steps = 10
            
            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                val x = (startX + (targetX - startX) * progress).toInt()
                val y = (startY + (targetY - startY) * progress).toInt()
                
                currentX = x
                currentY = y
                updateCursorPosition()
                
                delay(ANIMATION_DURATION_MS / steps)
            }
        }
    }
    
    /**
     * Dispose cursor manager
     */
    fun dispose() {
        Log.d(TAG, "Disposing CursorManager")
        hideCursor()
        cursorScope.cancel()
    }
    
    /**
     * Direction enum for cursor movement
     */
    enum class Direction {
        UP, DOWN, LEFT, RIGHT,
        UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
    }
}

/**
 * Extension function for VoiceAccessibilityService to find nodes at coordinates
 * Direct implementation - no abstraction
 */
fun VoiceAccessibilityService.findNodeAtCoordinates(
    node: AccessibilityNodeInfo,
    x: Int, 
    y: Int
): AccessibilityNodeInfo? {
    val bounds = android.graphics.Rect()
    node.getBoundsInScreen(bounds)
    
    if (bounds.contains(x, y)) {
        // Check if this node is clickable
        if (node.isClickable) {
            return node
        }
        
        // Check children for more specific match
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeAtCoordinates(child, x, y)
                if (result != null) return result
            }
        }
        
        // Return this node if no clickable child found
        return node
    }
    
    return null
}