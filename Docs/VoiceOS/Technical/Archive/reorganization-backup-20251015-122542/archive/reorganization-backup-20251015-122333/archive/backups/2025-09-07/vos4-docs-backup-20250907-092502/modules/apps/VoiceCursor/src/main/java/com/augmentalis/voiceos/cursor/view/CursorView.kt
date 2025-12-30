/**
 * View.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/view/View.kt
 * 
 * Created: 2025-01-23 00:15 PST
 * Last Modified: 2025-01-26 01:45 PST
 * Author: VOS4 Development Team
 * Version: 2.0.0
 * 
 * Purpose: Main cursor view with ARVision theming and DeviceManager IMU integration
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-23 00:15 PST): Initial port from VoiceOS
 * - v1.1.0 (2025-01-23 00:35 PDT): Updated package name to com.augmentalis.voiceos.cursor
 * - v2.0.0 (2025-01-26 01:45 PST): Migrated to VoiceCursor with ARVision theme and IMU integration
 */

package com.augmentalis.voiceos.cursor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.augmentalis.voiceos.cursor.R
import com.augmentalis.voiceos.cursor.core.*
import com.augmentalis.voiceos.cursor.filter.CursorFilter
import com.augmentalis.voiceos.cursor.helper.VoiceCursorIMUIntegration
import com.augmentalis.voiceos.cursor.service.VoiceCursorAccessibilityService
import kotlinx.coroutines.*

// Extension property for CursorConfig
val CursorConfig.showCrosshair: Boolean
    get() = type == CursorType.Normal

/**
 * Main cursor view with ARVision theming and enhanced IMU tracking
 * Coordinates cursor rendering, positioning, menu, and gesture dispatch
 */
class CursorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "CursorView"
        private const val CURSOR_DELAY_TIME = 0L
        private const val GAZE_CLICK_DELAY = 1500L
        private const val ANIMATION_DURATION = 200L
    }
    
    // Coroutine scope for animations and async operations
    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Core components
    private val positionManager = CursorPositionManager()
    private val renderer = CursorRenderer(context)
    private val gazeManager = GazeClickManager()
    private val cursorFilter = CursorFilter()
    private var imuIntegration: VoiceCursorIMUIntegration? = null
    private var accessibilityService: VoiceCursorAccessibilityService? = null
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    // Animation and gesture components
    private val cursorAnimator = CursorAnimator()
    private val gestureManager = GestureManager(context)
    
    // Current state (thread-safe)
    @Volatile private var cursorState = CursorState()
    @Volatile private var cursorConfig = CursorConfig()
    
    // ARVision-themed rendering
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.cursor_primary)
    }
    
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = ContextCompat.getColor(context, R.color.cursor_glass_border)
    }
    
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.cursor_glass_background)
    }
    
    private val coordinateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.cursor_primary)
        textSize = 28f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    
    private val coordinateBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x80000000.toInt() // Semi-transparent black
    }
    
    // Legacy animation support (kept for compatibility)
    private var pulseAnimator: ValueAnimator? = null
    private var currentScale = 1.0f
    
    // Animation and gesture state
    private var isHovering = false
    private var isDragging = false
    private var isGestureEnabled = true
    
    // Menu integration
    private var isMenuVisible = false
    private var menuShowTime = 0L
    
    // Gaze click support - now handled by GazeClickManager
    private var isOverlayShown = true
    private var gazeClickView: GazeClickView? = null
    
    // Lock state
    private var isCursorLocked = false
    private var lockedPosition = CursorOffset(0f, 0f)
    
    // Callbacks
    var onGazeAutoClick: ((CursorOffset) -> Unit)? = null
    var onMenuRequest: ((CursorOffset) -> Unit)? = null
    var onCursorMove: ((CursorOffset) -> Unit)? = null
    
    init {
        // Enable hardware acceleration for better performance
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // Initialize cursor configuration
        updateCursorStyle(cursorConfig)
        
        // Mark as not important for accessibility to avoid interference
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        
        // Initialize IMU integration
        initializeIMUIntegration()
        
        // Set up pulse animation
        setupPulseAnimation()
        
        // Initialize animation and gesture systems
        initializeAnimationSystem()
        initializeGestureSystem()
    }
    
    /**
     * Initialize IMU integration with DeviceManager
     */
    private fun initializeIMUIntegration() {
        imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
            setOnPositionUpdate { position ->
                updateCursorPosition(position)
            }
        }
    }
    
    /**
     * Set up pulse animation for cursor visibility
     */
    private fun setupPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.2f, 1.0f).apply {
            duration = ANIMATION_DURATION * 3
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                currentScale = animator.animatedValue as Float
                invalidate()
            }
        }
    }
    
    /**
     * Initialize animation system
     */
    private fun initializeAnimationSystem() {
        cursorAnimator.apply {
            onPositionUpdate = { position ->
                cursorState = cursorState.copy(position = position)
                invalidate()
            }
            
            onScaleUpdate = { scale ->
                renderer.setScale(scale)
                invalidate()
            }
            
            onAlphaUpdate = { alpha ->
                renderer.setAlpha(alpha)
                invalidate()
            }
            
            onAnimationComplete = { animationType ->
                when (animationType) {
                    AnimationType.CLICK_PULSE -> {
                        // Click pulse completed
                    }
                    AnimationType.VISIBILITY_FADE -> {
                        // Visibility animation completed
                    }
                    else -> { /* Handle other animation types */ }
                }
            }
        }
    }
    
    /**
     * Initialize gesture system
     */
    private fun initializeGestureSystem() {
        gestureManager.apply {
            onGestureEvent = { gestureEvent ->
                handleGestureEvent(gestureEvent)
            }
            
            onGestureStateChanged = { gestureState ->
                handleGestureStateChange(gestureState)
            }
        }
    }
    
    /**
     * Start cursor tracking
     */
    fun startTracking() {
        imuIntegration?.start()
        pulseAnimator?.start()
        
        // Get accessibility service reference
        accessibilityService = VoiceCursorAccessibilityService.getInstance()
    }
    
    /**
     * Stop cursor tracking with comprehensive cleanup
     */
    fun stopTracking() {
        imuIntegration?.stop()
        pulseAnimator?.cancel()
        cursorAnimator.cancelAllAnimations()
        
        // Cleanup gaze click animation
        cleanupGazeClickAnimation()
        
        viewScope.cancel()
    }
    
    /**
     * Update cursor position from IMU integration with smooth animation
     */
    private fun updateCursorPosition(position: CursorOffset) {
        if (!isCursorLocked) {
            // Apply jitter filtering if enabled
            val finalPosition = if (cursorConfig.jitterFilterEnabled) {
                val (filteredX, filteredY) = cursorFilter.filter(
                    position.x,
                    position.y,
                    System.nanoTime()
                )
                CursorOffset(filteredX, filteredY)
            } else {
                position
            }
            
            // Use smooth animation for position updates
            if (cursorAnimator.isEnabled && cursorAnimator.smoothMovementEnabled) {
                cursorAnimator.animateToPosition(finalPosition)
            } else {
                // Direct position update without animation
                cursorState = cursorState.copy(position = finalPosition)
                invalidate()
            }
            
            // Update accessibility service with filtered position
            accessibilityService?.updateCursorPosition(finalPosition)
            
            // Notify listeners
            onCursorMove?.invoke(finalPosition)
        }
        
        // Check for gaze click
        if (gazeManager.isGazeActive()) {
            checkGazeClick()
        }
    }
    
    /**
     * Legacy orientation API for backward compatibility
     */
    fun setOrientation(
        alpha: Float,
        beta: Float,
        gamma: Float,
        timestamp: Long,
        isLock: Boolean = false
    ): Int {
        // Use legacy position manager for compatibility
        val result = positionManager.calculatePosition(
            alpha, beta, gamma, timestamp, cursorConfig.speed
        )
        
        if (result.moved && !isCursorLocked) {
            val position = CursorOffset(result.x, result.y)
            updateCursorPosition(position)
        }
        
        // Handle lock state
        handleLockState(isLock, result.x, result.y)
        
        return result.distance.toInt()
    }
    
    /**
     * Handle cursor lock/unlock state
     */
    private fun handleLockState(isLock: Boolean, x: Float, y: Float) {
        when {
            !isCursorLocked && isLock -> {
                // Lock cursor at current position
                isCursorLocked = true
                lockedPosition = CursorOffset(x, y)
                cursorState = cursorState.copy(isLocked = true, lockedPosition = lockedPosition)
            }
            isCursorLocked && !isLock -> {
                // Unlock cursor
                isCursorLocked = false
                cursorState = cursorState.copy(isLocked = false)
            }
        }
    }
    
    /**
     * Check for gaze click activation using GazeClickManager
     */
    private fun checkGazeClick() {
        val result = gazeManager.checkGazeClick(
            cursorState.position.x,
            cursorState.position.y,
            System.currentTimeMillis(), // Fixed: Use millis instead of nanos to match GazeClickManager
            isOverlayShown
        )
        
        if (result.shouldClick) {
            performGazeClick()
        }
        
        cursorState = cursorState.copy(isGazeActive = result.isTracking)
    }
    
    /**
     * Perform gaze click action with animation
     */
    private fun performGazeClick() {
        val clickPosition = getClickPosition()
        
        // Show gaze click animation
        showGazeClickAnimation(clickPosition)
        
        // Animate click pulse effect on cursor
        cursorAnimator.animateClickPulse()
        
        // Provide haptic feedback if enabled
        provideHapticFeedback()
        
        onGazeAutoClick?.invoke(clickPosition)
        
        // Dispatch click through accessibility service with validation
        val service = accessibilityService
        if (service != null) {
            service.executeAction(CursorAction.SINGLE_CLICK, clickPosition)
        } else {
            Log.w(TAG, "Accessibility service not available for gaze click")
        }
    }
    
    /**
     * Show gaze click animation at specified position
     */
    private fun showGazeClickAnimation(position: CursorOffset) {
        try {
            // Clean up any existing gaze click view
            gazeClickView?.hide()
            
            // Create new gaze click animation view
            gazeClickView = GazeClickView(context, position).apply {
                show()
            }
        } catch (e: Exception) {
            // Handle animation creation errors gracefully
            Log.w(TAG, "Error showing gaze click animation", e)
        }
    }
    
    /**
     * Manually trigger gaze click animation (for external use)
     */
    fun triggerGazeClickAnimation(position: CursorOffset = cursorState.position) {
        showGazeClickAnimation(position)
    }
    
    /**
     * Cleanup gaze click animation
     */
    private fun cleanupGazeClickAnimation() {
        gazeClickView?.hide()
        gazeClickView = null
    }
    
    /**
     * Get current click position (considers lock state)
     */
    fun getClickPosition(): CursorOffset {
        return if (isCursorLocked) {
            lockedPosition
        } else {
            cursorState.position
        }
    }
    
    /**
     * Update cursor visual style
     */
    fun updateCursorStyle(config: CursorConfig) {
        cursorConfig = config
        
        // Update renderer with new configuration
        renderer.updateCursor(config, ResourceProvider(context))
        
        // Update paint colors
        cursorPaint.color = config.color
        borderPaint.strokeWidth = config.strokeWidth
        
        // Update IMU sensitivity
        imuIntegration?.setSensitivity(config.speed / 10.0f)
        
        invalidate()
    }
    
    /**
     * Center cursor on screen
     */
    fun centerCursor() {
        positionManager.centerCursor()
        imuIntegration?.centerCursor()
        
        val centerX = (width / 2).toFloat()
        val centerY = (height / 2).toFloat()
        val centerPosition = CursorOffset(centerX, centerY)
        
        cursorState = cursorState.copy(position = centerPosition)
        invalidate()
    }
    
    /**
     * Show/hide cursor menu
     */
    fun toggleMenu() {
        isMenuVisible = !isMenuVisible
        menuShowTime = System.currentTimeMillis()
        isOverlayShown = isMenuVisible
        
        if (isMenuVisible) {
            onMenuRequest?.invoke(cursorState.position)
        }
    }
    
    /**
     * Enable gaze tracking
     */
    fun enableGaze() {
        gazeManager.enableGaze()
    }
    
    /**
     * Disable gaze tracking
     */
    fun disableGaze() {
        gazeManager.disableGaze()
    }
    
    /**
     * Toggle coordinate display
     */
    fun toggleCoordinateDisplay() {
        cursorConfig = cursorConfig.copy(showCoordinates = !cursorConfig.showCoordinates)
        invalidate()
    }
    
    /**
     * Set coordinate display state
     */
    fun setCoordinateDisplay(show: Boolean) {
        cursorConfig = cursorConfig.copy(showCoordinates = show)
        invalidate()
    }
    
    /**
     * Set cursor visibility with smooth fade animation
     */
    fun setVisible(visible: Boolean, animate: Boolean = true) {
        cursorState = cursorState.copy(isVisible = visible)
        
        if (animate && cursorAnimator.isEnabled) {
            cursorAnimator.animateVisibility(visible)
        }
        
        visibility = if (visible) View.VISIBLE else View.INVISIBLE
        
        if (!animate) {
            invalidate()
        }
    }
    
    /**
     * Toggle between cursor types
     */
    fun toggleCursorType() {
        val newType = when (cursorConfig.type) {
            is CursorType.Normal -> CursorType.Hand
            is CursorType.Hand -> CursorType.Custom
            is CursorType.Custom -> CursorType.Normal
        }
        updateCursorType(newType)
    }
    
    /**
     * Update cursor type
     */
    fun updateCursorType(type: CursorType) {
        cursorConfig = cursorConfig.copy(type = type)
        renderer.updateCursor(cursorConfig, ResourceProvider(context))
        invalidate()
    }
    
    /**
     * Update cursor configuration including filter settings
     */
    fun updateCursorConfiguration(config: CursorConfig) {
        val oldConfig = cursorConfig
        cursorConfig = config
        
        // Update cursor filter configuration if filter settings changed
        if (oldConfig.jitterFilterEnabled != config.jitterFilterEnabled ||
            oldConfig.filterStrength != config.filterStrength ||
            oldConfig.motionSensitivity != config.motionSensitivity) {
            
            // Update filter configuration
            cursorFilter.setEnabled(config.jitterFilterEnabled)
            if (config.jitterFilterEnabled) {
                cursorFilter.updateConfig(
                    stationaryStrength = when (config.filterStrength) {
                        FilterStrength.Low -> 30
                        FilterStrength.Medium -> 60
                        FilterStrength.High -> 90
                    },
                    slowStrength = (config.filterStrength.numericValue * 0.7f).toInt(),
                    fastStrength = (config.filterStrength.numericValue * 0.3f).toInt(),
                    motionSensitivity = config.motionSensitivity
                )
            }
        }
        
        // Update renderer if visual properties changed
        if (oldConfig.type != config.type || 
            oldConfig.color != config.color || 
            oldConfig.size != config.size) {
            renderer.updateCursor(config, ResourceProvider(context))
        }
        
        // Update IMU sensitivity if speed changed
        if (oldConfig.speed != config.speed) {
            imuIntegration?.setSensitivity(config.speed / 10.0f)
        }
        
        // Update paint colors if color changed
        if (oldConfig.color != config.color) {
            updateCursorStyle(config)
        }
        
        invalidate()
    }
    
    
    /**
     * Adjust color alpha
     */
    private fun adjustColorAlpha(color: Int, alpha: Float): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb((255 * alpha).toInt(), red, green, blue)
    }
    
    /**
     * Update screen dimensions
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Update position manager and IMU integration
        positionManager.updateScreenDimensions(w, h)
        imuIntegration?.updateScreenDimensions(w, h)
        
        // Center cursor if first time sizing
        if (oldw == 0 && oldh == 0) {
            centerCursor()
        }
    }
    
    /**
     * Custom drawing with ARVision styling
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!cursorState.isVisible) return
        
        val position = cursorState.position
        val cursorRadius = (cursorConfig.size * currentScale) / 2f
        
        // Save canvas state
        canvas.save()
        
        try {
            // Apply overall alpha from animations
            val overallAlpha = renderer.getAlpha()
            
            // Draw glass morphism background with animation alpha
            glassPaint.alpha = ((cursorConfig.glassOpacity * overallAlpha) * 255).toInt()
            canvas.drawCircle(position.x, position.y, cursorRadius + 4f, glassPaint)
            
            // Use Renderer for main cursor drawing with effects
            renderer.drawCursorWithEffects(
                canvas, position.x, position.y, cursorConfig.type,
                showGlow = isHovering,
                showDragFeedback = isDragging
            )
            
            // Draw border overlay with animation alpha
            val originalBorderAlpha = borderPaint.alpha
            borderPaint.alpha = (255 * overallAlpha).toInt().coerceIn(0, 255)
            canvas.drawCircle(position.x, position.y, cursorRadius, borderPaint)
            borderPaint.alpha = originalBorderAlpha
            
            // Draw crosshair if needed
            if (cursorConfig.showCrosshair) {
                renderer.drawCrossHair(canvas, position.x, position.y, cursorRadius * 2)
            }
            
            // Draw gaze progress if active
            if (gazeManager.isGazeClickActive()) {
                drawGazeProgress(canvas, position, cursorRadius)
            }
            
            // Draw coordinate display if enabled
            if (cursorConfig.showCoordinates) {
                drawCoordinateDisplay(canvas, position, cursorRadius)
            }
            
        } finally {
            canvas.restore()
        }
    }
    
    // Legacy drawing methods - kept for compatibility but not used with Renderer
    @Suppress("UNUSED_PARAMETER")
    private fun drawRoundCursor(canvas: Canvas, position: CursorOffset, radius: Float) {
        // Now handled by Renderer
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun drawHandCursor(canvas: Canvas, position: CursorOffset, radius: Float) {
        // Now handled by Renderer
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun drawCustomCursor(canvas: Canvas, position: CursorOffset, radius: Float) {
        // Now handled by Renderer
    }
    
    /**
     * Draw gaze click progress indicator
     */
    private fun drawGazeProgress(canvas: Canvas, position: CursorOffset, radius: Float) {
        // Use animation frame from GazeClickManager for smooth progress
        val frameIndex = gazeManager.getAnimationFrame(60, 16)
        val progress = (frameIndex.toFloat() / 60).coerceIn(0f, 1f)
        
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = ContextCompat.getColor(context, R.color.system_green)
            pathEffect = null
        }
        
        val sweepAngle = 360f * progress
        val progressRadius = radius + 8f
        
        canvas.drawArc(
            position.x - progressRadius, position.y - progressRadius,
            position.x + progressRadius, position.y + progressRadius,
            -90f, sweepAngle, false, progressPaint
        )
    }
    
    /**
     * Draw coordinate display near cursor
     */
    private fun drawCoordinateDisplay(canvas: Canvas, position: CursorOffset, radius: Float) {
        // Format coordinates to whole numbers
        val coordinateText = "${position.x.toInt()}, ${position.y.toInt()}"
        
        // Calculate text bounds
        val textBounds = android.graphics.Rect()
        coordinateTextPaint.getTextBounds(coordinateText, 0, coordinateText.length, textBounds)
        
        // Position text below cursor with some padding
        val textX = position.x
        val textY = position.y + radius + textBounds.height() + 20f
        
        // Calculate background rectangle
        val padding = 12f
        val backgroundLeft = textX - textBounds.width() / 2f - padding
        val backgroundRight = textX + textBounds.width() / 2f + padding
        val backgroundTop = textY - textBounds.height() - padding
        val backgroundBottom = textY + padding
        
        // Draw semi-transparent background
        coordinateBackgroundPaint.alpha = (cursorConfig.glassOpacity * 180).toInt()
        canvas.drawRoundRect(
            backgroundLeft, backgroundTop, backgroundRight, backgroundBottom,
            8f, 8f, coordinateBackgroundPaint
        )
        
        // Draw coordinate text
        coordinateTextPaint.alpha = 255
        canvas.drawText(coordinateText, textX, textY, coordinateTextPaint)
    }
    
    /**
     * Handle touch events for gesture recognition
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false
        
        if (isGestureEnabled) {
            handled = gestureManager.onTouchEvent(event) || handled
        }
        
        return handled || super.onTouchEvent(event)
    }
    
    /**
     * Handle gesture events
     */
    private fun handleGestureEvent(gestureEvent: GestureEvent) {
        when (gestureEvent.type) {
            GestureType.TAP -> {
                // Animate click pulse on tap
                cursorAnimator.animateClickPulse()
                // Provide haptic feedback
                provideHapticFeedback()
                // Perform click action with validation
                val service = accessibilityService
                if (service != null) {
                    service.executeAction(CursorAction.SINGLE_CLICK, gestureEvent.currentPosition)
                } else {
                    Log.w(TAG, "Accessibility service not available for tap gesture")
                }
            }
            
            GestureType.DOUBLE_TAP -> {
                // Handle double tap - could trigger different action
                cursorAnimator.animateClickPulse()
                // Provide haptic feedback
                provideHapticFeedback()
                val service = accessibilityService
                if (service != null) {
                    service.executeAction(CursorAction.DOUBLE_CLICK, gestureEvent.currentPosition)
                } else {
                    Log.w(TAG, "Accessibility service not available for double tap gesture")
                }
            }
            
            GestureType.LONG_PRESS -> {
                // Handle long press - could show context menu
                toggleMenu()
            }
            
            GestureType.SWIPE_LEFT -> {
                // Handle swipe left - could move cursor or change mode
                val newX = (cursorState.position.x - 100f).coerceAtLeast(0f)
                val newPosition = CursorOffset(newX, cursorState.position.y)
                cursorAnimator.animateToPosition(newPosition, force = true)
            }
            
            GestureType.SWIPE_RIGHT -> {
                // Handle swipe right
                val newX = (cursorState.position.x + 100f).coerceAtMost(width.toFloat())
                val newPosition = CursorOffset(newX, cursorState.position.y)
                cursorAnimator.animateToPosition(newPosition, force = true)
            }
            
            GestureType.SWIPE_UP -> {
                // Handle swipe up
                val newY = (cursorState.position.y - 100f).coerceAtLeast(0f)
                val newPosition = CursorOffset(cursorState.position.x, newY)
                cursorAnimator.animateToPosition(newPosition, force = true)
            }
            
            GestureType.SWIPE_DOWN -> {
                // Handle swipe down
                val newY = (cursorState.position.y + 100f).coerceAtMost(height.toFloat())
                val newPosition = CursorOffset(cursorState.position.x, newY)
                cursorAnimator.animateToPosition(newPosition, force = true)
            }
            
            GestureType.PINCH_IN -> {
                // Handle pinch in - could decrease cursor size
                val newSize = (cursorConfig.size * 0.9f).toInt().coerceAtLeast(20)
                updateCursorConfiguration(cursorConfig.copy(size = newSize))
            }
            
            GestureType.PINCH_OUT -> {
                // Handle pinch out - could increase cursor size
                val newSize = (cursorConfig.size * 1.1f).toInt().coerceAtMost(100)
                updateCursorConfiguration(cursorConfig.copy(size = newSize))
            }
            
            else -> { /* Handle other gestures */ }
        }
    }
    
    /**
     * Handle gesture state changes
     */
    private fun handleGestureStateChange(gestureState: com.augmentalis.voiceos.cursor.core.GestureState) {
        // Update visual feedback based on gesture state
        isDragging = gestureState.isDragging
        
        if (gestureState.isDragging) {
            cursorAnimator.animateDragFeedback(true)
        } else if (!gestureState.isActive) {
            cursorAnimator.animateToNormalState()
        }
        
        invalidate()
    }
    
    /**
     * Enable or disable gesture recognition
     */
    fun setGestureEnabled(enabled: Boolean) {
        isGestureEnabled = enabled
        if (!enabled) {
            gestureManager.reset()
        }
    }
    
    /**
     * Enable or disable smooth animations
     */
    fun setSmoothAnimationsEnabled(enabled: Boolean) {
        cursorAnimator.smoothMovementEnabled = enabled
    }
    
    /**
     * Enable or disable cursor animations entirely
     */
    fun setAnimationsEnabled(enabled: Boolean) {
        cursorAnimator.setEnabled(enabled)
        if (!enabled) {
            cursorAnimator.cancelAllAnimations()
            renderer.resetAnimationValues()
        }
    }
    
    // Test and demonstration methods
    
    /**
     * Provide haptic feedback if enabled in settings
     */
    private fun provideHapticFeedback() {
        // Check if haptic feedback is enabled in settings
        val sharedPrefs = context.getSharedPreferences("voice_cursor_prefs", Context.MODE_PRIVATE)
        val hapticEnabled = sharedPrefs.getBoolean("haptic_feedback", true)
        
        if (hapticEnabled && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        }
    }
    
    /**
     * Test smooth cursor movement animation
     */
    fun testSmoothMovement(targetX: Float, targetY: Float) {
        val targetPosition = CursorOffset(targetX, targetY)
        cursorAnimator.animateToPosition(targetPosition, force = true)
    }
    
    /**
     * Test click pulse animation
     */
    fun testClickPulse() {
        cursorAnimator.animateClickPulse()
    }
    
    /**
     * Test hover glow effect
     */
    fun testHoverGlow(enable: Boolean) {
        isHovering = enable
        cursorAnimator.animateHoverGlow(enable)
    }
    
    /**
     * Test drag feedback animation
     */
    fun testDragFeedback(enable: Boolean) {
        cursorAnimator.animateDragFeedback(enable)
    }
    
    /**
     * Test fade in/out animation
     */
    fun testFadeAnimation(fadeIn: Boolean) {
        cursorAnimator.animateVisibility(fadeIn)
    }
    
    /**
     * Test gaze click animation
     */
    fun testGazeClickAnimation(position: CursorOffset = cursorState.position) {
        showGazeClickAnimation(position)
    }
    
    /**
     * Get current animation state for debugging
     */
    fun getAnimationState(): AnimationState {
        return cursorAnimator.getCurrentState()
    }
    
    /**
     * Get current gesture state for debugging
     */
    fun getGestureState(): com.augmentalis.voiceos.cursor.core.GestureState {
        return gestureManager.getCurrentState()
    }
    
    /**
     * Cleanup resources including gaze click animation
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        stopTracking()
        pulseAnimator?.cancel()
        
        // Cleanup gaze click animation
        cleanupGazeClickAnimation()
        
        // Dispose of all components
        imuIntegration?.dispose()
        positionManager.dispose()
        renderer.dispose()
        gazeManager.dispose()
        cursorAnimator.dispose()
        gestureManager.dispose()
    }
}

/**
 * Simple data class to track last point for delay handling
 */
private data class LastPoint(
    var x: Float = 0f,
    var y: Float = 0f,
    var ts: Long = 0L
) {
    fun reset(newX: Float, newY: Float, timestamp: Long) {
        x = newX
        y = newY
        ts = timestamp
    }
}