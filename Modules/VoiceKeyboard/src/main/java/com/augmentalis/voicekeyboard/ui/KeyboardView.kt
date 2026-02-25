/**
 * KeyboardView.kt - Custom keyboard view implementation
 * Replaces deprecated Android KeyboardView with modern custom implementation
 * 
 * Author: Manoj Jhawar
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.augmentalis.voicekeyboard.R
import com.augmentalis.voicekeyboard.service.KeyboardActionListener
import com.augmentalis.voicekeyboard.service.KeyboardLayout
import com.augmentalis.voicekeyboard.utils.KeyboardConstants

/**
 * Modern custom keyboard view implementation
 * Replaces deprecated Android KeyboardView
 * 
 * SOLID Principles:
 * - Single Responsibility: Only handles keyboard rendering and touch events
 * - Open/Closed: Extensible through listeners and layouts
 * - Dependency Inversion: Depends on interfaces (KeyboardActionListener)
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "KeyboardView"
        private const val LONG_PRESS_TIMEOUT = 400L
        private const val KEY_REPEAT_DELAY = 50L
        private const val GESTURE_TRAIL_FADE_DURATION = 200L
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
        
        // Key dimensions (dp)
        private const val KEY_HEIGHT_DP = 48f
        private const val KEY_MARGIN_DP = 2f
        private const val KEY_TEXT_SIZE_SP = 16f
    }
    
    // Listeners
    private var actionListener: KeyboardActionListener? = null
    
    // State
    private var currentLayout = KeyboardLayout.QWERTY
    private var isShifted = false
    private var isAlphabetMode = true
    private var voiceInputEnabled = false
    private var gestureTypingEnabled = false
    
    // Gesture detection
    private val gestureDetector: GestureDetector
    private val gesturePoints = mutableListOf<Pair<Float, Float>>()
    private var isGestureTyping = false
    
    // Visual properties
    private var keyboardTheme: KeyboardTheme = KeyboardTheme.LIGHT
    private val keyPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val keyTextPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
@Suppress("DEPRECATION")
        textSize = KEY_TEXT_SIZE_SP * resources.displayMetrics.scaledDensity
    }
    private val keyStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    
    // Layout properties
    private val keyHeight = KEY_HEIGHT_DP * resources.displayMetrics.density
    private val keyMargin = KEY_MARGIN_DP * resources.displayMetrics.density
    private var keys: List<KeyInfo> = emptyList()
    
    // Touch handling
    private var longPressHandler: Handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var repeatKeyRunnable: Runnable? = null
    private var currentPressedKey: KeyInfo? = null
    
    init {
        // Initialize gesture detector
        gestureDetector = GestureDetector(context, GestureListener())
        
        // Load default keyboard layout
        loadKeyboardLayout(currentLayout)
        
        // Setup visual properties
        setupVisualProperties()
        
        // Set click feedback
        isClickable = true
        isFocusable = true
    }
    
    /**
     * Set the action listener for keyboard events
     */
    fun setActionListener(listener: KeyboardActionListener) {
        actionListener = listener
    }
    
    /**
     * Update keyboard layout
     */
    fun updateLayout(layout: KeyboardLayout) {
        if (currentLayout != layout) {
            currentLayout = layout
            loadKeyboardLayout(layout)
            invalidate()
        }
    }
    
    /**
     * Set shifted state
     */
    fun setShifted(shifted: Boolean) {
        if (isShifted != shifted) {
            isShifted = shifted
            invalidate()
        }
    }
    
    /**
     * Set alphabet mode
     */
    fun setAlphabetMode(alphabetMode: Boolean) {
        isAlphabetMode = alphabetMode
        updateLayout(if (alphabetMode) KeyboardLayout.QWERTY else KeyboardLayout.SYMBOLS)
    }
    
    /**
     * Enable/disable voice input
     */
    fun setVoiceInputEnabled(enabled: Boolean) {
        voiceInputEnabled = enabled
        invalidate()
    }
    
    /**
     * Enable/disable gesture typing
     */
    fun setGestureTypingEnabled(enabled: Boolean) {
        gestureTypingEnabled = enabled
    }
    
    /**
     * Set keyboard theme
     */
    fun setTheme(theme: KeyboardTheme) {
        keyboardTheme = theme
        setupVisualProperties()
        invalidate()
    }
    
    /**
     * Show specific keyboard layouts
     */
    fun showQwertyKeyboard() = updateLayout(KeyboardLayout.QWERTY)
    fun showNumberKeyboard() = updateLayout(KeyboardLayout.NUMERIC)
    fun showPhoneKeyboard() = updateLayout(KeyboardLayout.PHONE)
    fun showSymbolsKeyboard() = updateLayout(KeyboardLayout.SYMBOLS)
    fun showEmojiKeyboard() = updateLayout(KeyboardLayout.EMOJI)
    fun showEmailKeyboard() = updateLayout(KeyboardLayout.QWERTY) // TODO: Special email layout
    fun showUrlKeyboard() = updateLayout(KeyboardLayout.QWERTY) // TODO: Special URL layout
    fun showPasswordKeyboard() = updateLayout(KeyboardLayout.QWERTY) // TODO: Special password layout
    fun showDateTimeKeyboard() = updateLayout(KeyboardLayout.NUMERIC)
    
    /**
     * Update word suggestions (placeholder)
     */
    fun updateSuggestions(@Suppress("UNUSED_PARAMETER") suggestions: List<String>) {
        // TODO: Display suggestions in candidate view (${suggestions.size} available)
    }
    
    /**
     * Show alternative characters for long press (placeholder)
     */
    fun showAlternativeCharacters(@Suppress("UNUSED_PARAMETER") keyCode: Int) {
        // TODO: Show popup with alternative characters for key $keyCode
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = calculateKeyboardHeight()
        setMeasuredDimension(width, height.toInt())
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutKeys()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw keys
        for (key in keys) {
            drawKey(canvas, key)
        }
        
        // Draw gesture trail if active
        if (isGestureTyping && gesturePoints.size > 1) {
            drawGestureTrail(canvas)
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle gesture typing if enabled
        if (gestureTypingEnabled && handleGestureTyping(event)) {
            return true
        }
        
        // Handle normal touch events
        when (event.action) {
            MotionEvent.ACTION_DOWN -> handleTouchDown(event)
            MotionEvent.ACTION_MOVE -> handleTouchMove(event)
            MotionEvent.ACTION_UP -> handleTouchUp(event)
        }
        
        // Pass to gesture detector for swipe detection
        gestureDetector.onTouchEvent(event)
        
        return true
    }
    
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
    
    /**
     * Load keyboard layout
     */
    private fun loadKeyboardLayout(layout: KeyboardLayout) {
        keys = when (layout) {
            KeyboardLayout.QWERTY -> createQwertyKeys()
            KeyboardLayout.NUMERIC -> createNumericKeys()
            KeyboardLayout.PHONE -> createPhoneKeys()
            KeyboardLayout.SYMBOLS -> createSymbolKeys()
            KeyboardLayout.EMOJI -> createEmojiKeys()
        }
        layoutKeys()
    }
    
    /**
     * Create QWERTY keyboard layout
     */
    private fun createQwertyKeys(): List<KeyInfo> {
        val keys = mutableListOf<KeyInfo>()
        
        // Row 1: QWERTYUIOP
        val row1 = "qwertyuiop".toCharArray()
        row1.forEachIndexed { index, char ->
            keys.add(KeyInfo(char.code, char.toString().uppercase(), index, 0, KeyType.LETTER))
        }
        
        // Row 2: ASDFGHJKL
        val row2 = "asdfghjkl".toCharArray()
        row2.forEachIndexed { index, char ->
            keys.add(KeyInfo(char.code, char.toString().uppercase(), index, 1, KeyType.LETTER))
        }
        
        // Row 3: ZXCVBNM with special keys
        keys.add(KeyInfo(KeyboardConstants.KEYCODE_SHIFT, "⇧", 0, 2, KeyType.MODIFIER))
        val row3 = "zxcvbnm".toCharArray()
        row3.forEachIndexed { index, char ->
            keys.add(KeyInfo(char.code, char.toString().uppercase(), index + 1, 2, KeyType.LETTER))
        }
        keys.add(KeyInfo(android.view.KeyEvent.KEYCODE_DEL, "⌫", 8, 2, KeyType.ACTION))
        
        // Row 4: Space bar and special keys
        keys.add(KeyInfo(KeyboardConstants.KEYCODE_MODE_CHANGE, "123", 0, 3, KeyType.MODIFIER))
        keys.add(KeyInfo(KeyboardConstants.KEYCODE_VOICE, "🎤", 1, 3, KeyType.ACTION))
        keys.add(KeyInfo(android.view.KeyEvent.KEYCODE_SPACE, "space", 2, 3, KeyType.SPACE))
        keys.add(KeyInfo(android.view.KeyEvent.KEYCODE_ENTER, "↵", 8, 3, KeyType.ACTION))
        keys.add(KeyInfo(KeyboardConstants.KEYCODE_SETTINGS, "⚙", 9, 3, KeyType.ACTION))
        
        return keys
    }
    
    /**
     * Create numeric keyboard layout
     */
    private fun createNumericKeys(): List<KeyInfo> {
        val keys = mutableListOf<KeyInfo>()
        
        // Numbers 1-9, 0
        for (i in 1..9) {
            val row = (i - 1) / 3
            val col = (i - 1) % 3
            keys.add(KeyInfo(i + '0'.code, i.toString(), col, row, KeyType.NUMBER))
        }
        
        keys.add(KeyInfo('0'.code, "0", 1, 3, KeyType.NUMBER))
        keys.add(KeyInfo(android.view.KeyEvent.KEYCODE_DEL, "⌫", 2, 3, KeyType.ACTION))
        
        return keys
    }
    
    /**
     * Create phone keyboard layout
     */
    private fun createPhoneKeys(): List<KeyInfo> {
        // Similar to numeric but with phone-specific layout
        return createNumericKeys()
    }
    
    /**
     * Create symbol keyboard layout
     */
    private fun createSymbolKeys(): List<KeyInfo> {
        val keys = mutableListOf<KeyInfo>()
        
        val symbols = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`"
        symbols.forEachIndexed { index, char ->
            val row = index / 10
            val col = index % 10
            keys.add(KeyInfo(char.code, char.toString(), col, row, KeyType.SYMBOL))
        }
        
        return keys
    }
    
    /**
     * Create emoji keyboard layout (placeholder)
     */
    private fun createEmojiKeys(): List<KeyInfo> {
        // TODO: Implement emoji layout
        return createSymbolKeys()
    }
    
    /**
     * Layout keys based on current view dimensions
     */
    private fun layoutKeys() {
        if (width <= 0) return
        
        val availableWidth = width - paddingLeft - paddingRight
        val keysPerRow = getMaxKeysPerRow()
        val keyWidth = (availableWidth - (keysPerRow + 1) * keyMargin) / keysPerRow
        
        keys.forEach { key ->
            val x = paddingLeft + key.col * (keyWidth + keyMargin) + keyMargin
            val y = paddingTop + key.row * (keyHeight + keyMargin) + keyMargin
            
            key.bounds = RectF(x, y, x + keyWidth, y + keyHeight)
        }
    }
    
    /**
     * Draw individual key
     */
    private fun drawKey(canvas: Canvas, key: KeyInfo) {
        val bounds = key.bounds
        
        // Draw key background
        keyPaint.color = getKeyBackgroundColor(key)
        canvas.drawRoundRect(bounds, 8f, 8f, keyPaint)
        
        // Draw key border
        keyStrokePaint.color = getKeyBorderColor()
        canvas.drawRoundRect(bounds, 8f, 8f, keyStrokePaint)
        
        // Draw key text
        keyTextPaint.color = getKeyTextColor(key)
        val centerX = bounds.centerX()
        val centerY = bounds.centerY() - (keyTextPaint.descent() + keyTextPaint.ascent()) / 2
        canvas.drawText(key.label, centerX, centerY, keyTextPaint)
    }
    
    /**
     * Handle gesture typing events
     */
    private fun handleGestureTyping(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val key = getKeyAtPosition(event.x, event.y)
                if (key != null && key.type == KeyType.LETTER) {
                    isGestureTyping = true
                    gesturePoints.clear()
                    gesturePoints.add(Pair(event.x, event.y))
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isGestureTyping) {
                    gesturePoints.add(Pair(event.x, event.y))
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isGestureTyping) {
                    processGesturePath()
                    isGestureTyping = false
                    gesturePoints.clear()
                    invalidate()
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Process completed gesture path
     */
    private fun processGesturePath() {
        if (gesturePoints.size < 3) return
        // Send gesture points to service for processing
        (context as? com.augmentalis.voicekeyboard.service.VoiceKeyboardService)?.onGestureTypingPath(gesturePoints.toList())
    }
    
    /**
     * Draw gesture typing trail
     */
    private fun drawGestureTrail(canvas: Canvas) {
        val paint = Paint().apply {
            color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
            alpha = 200
        }
        
        for (i in 1 until gesturePoints.size) {
            val start = gesturePoints[i - 1]
            val end = gesturePoints[i]
            canvas.drawLine(start.first, start.second, end.first, end.second, paint)
        }
    }
    
    /**
     * Handle touch events
     */
    private fun handleTouchDown(event: MotionEvent) {
        val key = getKeyAtPosition(event.x, event.y)
        if (key != null) {
            currentPressedKey = key
            performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            
            // Handle immediate key press
            actionListener?.onKey(key.code, null)
            performClick() // For accessibility support
            
            // Setup long press for repeatable keys
            if (key.code == android.view.KeyEvent.KEYCODE_DEL) {
                setupKeyRepeat(key)
            }
        }
    }
    
    private fun handleTouchMove(@Suppress("UNUSED_PARAMETER") event: MotionEvent) {
        val key = getKeyAtPosition(event.x, event.y)
        if (key != currentPressedKey) {
            cancelKeyRepeat()
        }
    }
    
    private fun handleTouchUp(@Suppress("UNUSED_PARAMETER") event: MotionEvent) {
        // Call performClick for accessibility support
        if (currentPressedKey != null) {
            performClick()
        }
        cancelKeyRepeat()
        currentPressedKey = null
    }
    
    /**
     * Setup key repeat for delete key
     */
    private fun setupKeyRepeat(key: KeyInfo) {
        val runnable = object : Runnable {
            override fun run() {
                actionListener?.onKey(key.code, null)
                longPressHandler.postDelayed(this, KEY_REPEAT_DELAY)
            }
        }
        repeatKeyRunnable = runnable
        longPressHandler.postDelayed(runnable, LONG_PRESS_TIMEOUT)
    }
    
    /**
     * Cancel key repeat
     */
    private fun cancelKeyRepeat() {
        repeatKeyRunnable?.let {
            longPressHandler.removeCallbacks(it)
            repeatKeyRunnable = null
        }
    }
    
    /**
     * Get key at position
     */
    private fun getKeyAtPosition(x: Float, y: Float): KeyInfo? {
        return keys.firstOrNull { it.bounds.contains(x, y) }
    }
    
    /**
     * Helper methods for colors and layout
     */
    private fun getKeyBackgroundColor(key: KeyInfo): Int {
        return when (keyboardTheme) {
            KeyboardTheme.LIGHT -> {
                if (key == currentPressedKey) ContextCompat.getColor(context, android.R.color.darker_gray)
                else ContextCompat.getColor(context, android.R.color.white)
            }
            KeyboardTheme.DARK -> {
                if (key == currentPressedKey) ContextCompat.getColor(context, android.R.color.darker_gray)
                else ContextCompat.getColor(context, android.R.color.black)
            }
            KeyboardTheme.SYSTEM -> ContextCompat.getColor(context, android.R.color.white) // Fallback to light theme
        }
    }
    
    private fun getKeyBorderColor(): Int {
        return ContextCompat.getColor(context, android.R.color.darker_gray)
    }
    
    private fun getKeyTextColor(@Suppress("UNUSED_PARAMETER") key: KeyInfo): Int {
        return when (keyboardTheme) {
            KeyboardTheme.LIGHT -> ContextCompat.getColor(context, android.R.color.black)
            KeyboardTheme.DARK -> ContextCompat.getColor(context, android.R.color.white)
            KeyboardTheme.SYSTEM -> ContextCompat.getColor(context, android.R.color.black) // Fallback to light theme
        }
    }
    
    private fun calculateKeyboardHeight(): Float {
        val rowCount = getMaxRows()
        return paddingTop + paddingBottom + rowCount * keyHeight + (rowCount + 1) * keyMargin
    }
    
    private fun getMaxKeysPerRow(): Int = 10 // Standard QWERTY row width
    
    private fun getMaxRows(): Int = 4 // Standard keyboard rows
    
    private fun setupVisualProperties() {
        when (keyboardTheme) {
            KeyboardTheme.LIGHT -> {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.background_light))
            }
            KeyboardTheme.DARK -> {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.background_dark))
            }
            KeyboardTheme.SYSTEM -> {
                // TODO: Detect system theme
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.background_light))
            }
        }
    }
    
    /**
     * Gesture listener for swipe detection
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null || isGestureTyping) return false
            
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        actionListener?.swipeRight()
                    } else {
                        actionListener?.swipeLeft()
                    }
                    return true
                }
            } else {
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        actionListener?.swipeDown()
                    } else {
                        actionListener?.swipeUp()
                    }
                    return true
                }
            }
            
            return false
        }
    }
}

/**
 * Key information data class
 */
data class KeyInfo(
    val code: Int,
    val label: String,
    val col: Int,
    val row: Int,
    val type: KeyType,
    var bounds: RectF = RectF()
)

/**
 * Key types for different styling
 */
enum class KeyType {
    LETTER,
    NUMBER,
    SYMBOL,
    SPACE,
    ACTION,
    MODIFIER
}

/**
 * Keyboard theme options
 */
enum class KeyboardTheme {
    LIGHT,
    DARK,
    SYSTEM
}