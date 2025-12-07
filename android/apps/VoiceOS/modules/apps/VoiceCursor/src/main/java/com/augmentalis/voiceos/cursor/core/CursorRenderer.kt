/**
 * Renderer.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/core/Renderer.kt
 * 
 * Created: 2025-01-23 00:15 PST
 * Last Modified: 2025-01-23 00:33 PDT
 * Author: VOS4 Development Team
 * Version: 2.1.0
 * 
 * Purpose: Thread-safe cursor rendering and bitmap management
 * Module: Cursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-23 00:15 PST): Initial port from VoiceOS
 * - v2.0.0 (2025-01-23 00:27 PDT): Fixed thread safety, bitmap management, error handling
 * - v2.1.0 (2025-01-23 00:33 PDT): Updated package name to com.augmentalis.voiceos.cursor
 * - v3.0.0 (2025-01-26): Integrated into VoiceCursor module
 */

package com.augmentalis.voiceos.cursor.core

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.scale

/**
 * Handles all cursor rendering operations
 * Single Responsibility: Drawing cursors on canvas
 * Thread-safe implementation with comprehensive error handling
 */
class CursorRenderer(private val context: Context) {
    
    companion object {
        private const val TAG = "CursorRenderer"
        private const val CURSOR_STROKE_WIDTH = 4.0f
        private const val FRAME_RATE_LIMIT_MS = 16 // ~60fps max
        private const val DEFAULT_CURSOR_SIZE = 40
    }
    
    // FIX: Thread-safe bitmap management
    @Volatile private var currentCursor: Bitmap? = null
    @Volatile private var cursorType: CursorType = CursorType.Normal
    
    // FIX: Synchronization lock for rendering operations
    private val renderLock = Any()
    
    // FIX: Track all created bitmaps for proper disposal
    private val bitmapCache = mutableListOf<Bitmap>()
    
    // Animation support
    @Volatile private var currentScale = 1.0f
    @Volatile private var currentAlpha = 1.0f
    
    // Paint objects (reused for performance)
    private val cursorPaint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 4f
    }
    
    private val crossHairPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(5f, 10f), 1f)
        strokeWidth = CURSOR_STROKE_WIDTH
    }
    
    // Additional paints for visual effects
    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    
    private val dragPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.CYAN
        alpha = 128
    }
    
    // Cached drawing coordinates
    @Volatile private var cursorDrawX = 0f
    @Volatile private var cursorDrawY = 0f
    @Volatile private var lastInvalidateTime = 0L
    
    /**
     * Update cursor bitmap based on configuration
     * FIX: Thread-safe with synchronization and error handling
     */
    fun updateCursor(config: CursorConfig, resourceProvider: ResourceProvider) {
        synchronized(renderLock) {
            cursorType = config.type
            
            // FIX: Dispose old cursor before creating new one
            val oldCursor = currentCursor
            
            currentCursor = try {
                when (config.type) {
                    is CursorType.Hand -> {
                        createHandCursor(config, resourceProvider)
                    }
                    is CursorType.Normal -> {
//                        createRoundCursor(
//                            config.size,
//                            config.color,
//                            resourceProvider.getRoundCursorResource()
//                        )
                        resourceProvider.getCursorBitmap(config.size)
                    }
                    is CursorType.Custom -> {
                        createCustomCursor(config, resourceProvider)
                    }
                }
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OOM creating cursor, using fallback", e)
                createFallbackCursor(config.size)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create cursor, using fallback", e)
                createFallbackCursor(config.size)
            }
            
            // FIX: Properly dispose old cursor after new one is ready
            oldCursor?.let {
                if (it != currentCursor && !it.isRecycled) {
                    it.recycle()
                    bitmapCache.remove(it)
                }
            }
            
            // Add new cursor to cache
            currentCursor?.let {
                if (!bitmapCache.contains(it)) {
                    bitmapCache.add(it)
                }
            }
            
            // Update cached offsets
            updateCursorOffsets()
        }
    }
    
    /**
     * Draw cursor on canvas with animation support
     * FIX: Thread-safe drawing with scale and alpha animations
     */
    fun drawCursor(
        canvas: Canvas,
        x: Float,
        y: Float,
        type: CursorType = cursorType
    ) {
        synchronized(renderLock) {
            currentCursor?.let { cursor ->
                if (!cursor.isRecycled) {
                    val centerOffset = getCenterOffset(type, cursor.width, cursor.height)
                    
                    try {
                        // Apply transformations for animations
                        canvas.save()
                        
                        // Apply scale transformation
                        if (currentScale != 1.0f) {
                            canvas.scale(currentScale, currentScale, x, y)
                        }
                        
                        // Apply alpha to paint
                        //val originalAlpha = cursorPaint.alpha
                        //cursorPaint.alpha = (255 * currentAlpha).toInt().coerceIn(0, 255)
                        
                        canvas.drawBitmap(
                            cursor,
                            x - centerOffset.first,
                            y - centerOffset.second, cursorPaint)
                        
                        // Restore original alpha
                        //cursorPaint.alpha = originalAlpha
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to draw cursor", e)
                    } finally {
                        canvas.restore()
                    }
                }
            }
        }
    }
    
    /**
     * Draw cursor with additional visual effects
     */
    fun drawCursorWithEffects(
        canvas: Canvas,
        x: Float,
        y: Float,
        type: CursorType = cursorType,
        showGlow: Boolean = false,
        showDragFeedback: Boolean = false
    ) {
        synchronized(renderLock) {
            canvas.save()
            
            try {
                // Draw glow effect if enabled
                if (showGlow) {
                    drawGlowEffect(canvas, x, y)
                }
                
                // Draw drag feedback if enabled
                if (showDragFeedback) {
                    drawDragEffect(canvas, x, y)
                }
                
                // Draw main cursor
                drawCursor(canvas, x, y, type)
                
            } finally {
                canvas.restore()
            }
        }
    }
    
    /**
     * Draw crosshair overlay with animation support
     */
    fun drawCrossHair(canvas: Canvas, x: Float, y: Float, size: Float) {
        val halfSize = size / 2
        
        // Apply alpha to crosshair paint
        val originalAlpha = crossHairPaint.alpha
        crossHairPaint.alpha = (255 * currentAlpha).toInt().coerceIn(0, 255)
        
        try {
            // Horizontal line
            canvas.drawLine(x - halfSize, y, x + halfSize, y, crossHairPaint)
            
            // Vertical line
            canvas.drawLine(x, y - halfSize, x, y + halfSize, crossHairPaint)
        } finally {
            // Restore original alpha
            crossHairPaint.alpha = originalAlpha
        }
    }
    
    /**
     * Draw glow effect around cursor
     */
    private fun drawGlowEffect(canvas: Canvas, x: Float, y: Float) {
        val glowRadius = 30f * currentScale
        
        // Create radial gradient for glow
        val shader = RadialGradient(
            x, y, glowRadius,
            intArrayOf(
                Color.argb((80 * currentAlpha).toInt(), 255, 255, 255),
                Color.argb((40 * currentAlpha).toInt(), 255, 255, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        
        glowPaint.shader = shader
        canvas.drawCircle(x, y, glowRadius, glowPaint)
        glowPaint.shader = null
    }
    
    /**
     * Draw drag effect feedback
     */
    private fun drawDragEffect(canvas: Canvas, x: Float, y: Float) {
        val dragRadius = 25f * currentScale
        
        // Apply alpha to drag paint
        val originalAlpha = dragPaint.alpha
        dragPaint.alpha = (128 * currentAlpha).toInt().coerceIn(0, 255)
        
        try {
            // Draw drag indicator circle
            canvas.drawCircle(x, y, dragRadius, dragPaint)
            
            // Draw crosshair in drag circle
            val halfSize = dragRadius * 0.6f
            canvas.drawLine(x - halfSize, y, x + halfSize, y, dragPaint)
            canvas.drawLine(x, y - halfSize, x, y + halfSize, dragPaint)
        } finally {
            dragPaint.alpha = originalAlpha
        }
    }
    
    /**
     * Check if redraw is needed based on frame rate limit
     */
    fun shouldRedraw(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInvalidateTime >= FRAME_RATE_LIMIT_MS) {
            lastInvalidateTime = currentTime
            return true
        }
        return false
    }
    
    /**
     * FIX: Added error handling and null checks
     */
    private fun createHandCursor(config: CursorConfig, resourceProvider: ResourceProvider): Bitmap? {
        return try {
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                resourceProvider.getHandCursorResource()
            )
            
            if (bitmap == null) {
                Log.w(TAG, "Hand cursor resource not found, using fallback")
                return createFallbackCursor(config.handCursorSize)
            }
            
            bitmap.scale(config.handCursorSize, config.handCursorSize)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM creating hand cursor", e)
            createFallbackCursor(config.handCursorSize)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create hand cursor", e)
            createFallbackCursor(config.handCursorSize)
        }
    }
    
    private fun createRoundCursor(size: Int, color: Int, resourceId: Int): Bitmap? {
        return try {
            val drawable = safeGetDrawable(resourceId)
            if (drawable == null) {
                Log.w(TAG, "Round cursor resource not found, using fallback")
                return createFallbackCursor(size)
            }
            
            val wrappedDrawable = DrawableCompat.wrap(drawable.mutate())
            
            // Ensure color is properly converted from Long to Int if needed
            val tintColor = when {
                color == 0 -> 0 // No tinting
                color > Int.MAX_VALUE -> color // Convert Long to Int
                else -> color
            }
            
            if (tintColor != 0) {
                DrawableCompat.setTint(wrappedDrawable, tintColor)
            }
            
            val adjustedSize = (size * 0.6).toInt()
            drawableToBitmap(wrappedDrawable)?.scale(adjustedSize, adjustedSize)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create round cursor", e)
            createFallbackCursor(size)
        }
    }
    
    private fun createCustomCursor(config: CursorConfig, resourceProvider: ResourceProvider): Bitmap? {
        return try {
            val resourceId = resourceProvider.getCustomCursorResource(config.type)
            val drawable = safeGetDrawable(resourceId)
            if (drawable == null) {
                Log.w(TAG, "Custom cursor resource not found, using fallback")
                return createFallbackCursor(config.size)
            }
            
            val wrappedDrawable = DrawableCompat.wrap(drawable.mutate())
            
            // Ensure color is properly converted from Long to Int if needed
            val tintColor = when {
                config.color == 0 -> 0 // No tinting
                config.color > Int.MAX_VALUE -> config.color.toInt() // Convert Long to Int
                else -> config.color
            }
            
            if (tintColor != 0) {
                DrawableCompat.setTint(wrappedDrawable, tintColor)
            }
            
            val adjustedSize = (config.size * 0.6).toInt()
            drawableToBitmap(wrappedDrawable)?.scale(adjustedSize, adjustedSize)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create custom cursor", e)
            createFallbackCursor(config.size)
        }
    }
    
    /**
     * FIX: Added fallback cursor creation
     */
    private fun createFallbackCursor(size: Int): Bitmap {
        return try {
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                val paint = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.FILL_AND_STROKE
                    strokeWidth = 2f
                    isAntiAlias = true
                }
                
                // Draw a simple circle as fallback
                val center = size / 2f
                canvas.drawCircle(center, center, center - 4, paint)
                
                // Add center dot
                paint.color = Color.WHITE
                canvas.drawCircle(center, center, 4f, paint)
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM creating fallback cursor, using minimal", e)
            // Create absolute minimal 1x1 bitmap as last resort
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }
    
    /**
     * FIX: Added error handling
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap
            }
            
            val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                createBitmap(1, 1)
            } else {
                createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            }
            
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM converting drawable to bitmap", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert drawable to bitmap", e)
            null
        }
    }
    
    private fun updateCursorOffsets() {
        currentCursor?.let { cursor ->
            if (!cursor.isRecycled) {
                val centerOffset = getCenterOffset(cursorType, cursor.width, cursor.height)
                cursorDrawX = centerOffset.first
                cursorDrawY = centerOffset.second
            }
        }
    }
    
    private fun getCenterOffset(type: CursorType, width: Int, height: Int): Pair<Float, Float> {
        return when (type) {
            is CursorType.Hand -> Pair(width * 0.413f, height * 0.072f)
            else -> Pair(width * 0.5f, height * 0.5f)
        }
    }
    
    fun getCursorSize(): Int = currentCursor?.width ?: DEFAULT_CURSOR_SIZE
    
    /**
     * Set animation scale factor
     */
    fun setScale(scale: Float) {
        currentScale = scale.coerceIn(0.1f, 3.0f)
    }
    
    /**
     * Set animation alpha factor
     */
    fun setAlpha(alpha: Float) {
        currentAlpha = alpha.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Get current scale
     */
    fun getScale(): Float = currentScale
    
    /**
     * Get current alpha
     */
    fun getAlpha(): Float = currentAlpha
    
    /**
     * Reset animation values to defaults
     */
    fun resetAnimationValues() {
        synchronized(renderLock) {
            currentScale = 1.0f
            currentAlpha = 1.0f
        }
    }
    
    /**
     * Apply animation state from CursorAnimator
     */
    fun applyAnimationState(scale: Float, alpha: Float) {
        synchronized(renderLock) {
            currentScale = scale.coerceIn(0.1f, 3.0f)
            currentAlpha = alpha.coerceIn(0.0f, 1.0f)
        }
    }
    
    /**
     * FIX: Comprehensive resource cleanup
     */
    fun dispose() {
        synchronized(renderLock) {
            // Dispose all cached bitmaps
            bitmapCache.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            bitmapCache.clear()
            
            // Clear current cursor reference
            currentCursor = null
            
            // Reset cached values
            cursorDrawX = 0f
            cursorDrawY = 0f
            lastInvalidateTime = 0L
            
            // Reset animation values
            currentScale = 1.0f
            currentAlpha = 1.0f
        }
    }
    
    /**
     * Validate that a resource exists before attempting to load it
     */
    private fun validateResource(resourceId: Int): Boolean {
        return try {
            context.resources.getResourceName(resourceId)
            true
        } catch (e: Resources.NotFoundException) {
            Log.w(TAG, "Resource not found: $resourceId", e)
            false
        }
    }
    
    /**
     * Safely load a drawable resource with validation
     */
    private fun safeGetDrawable(resourceId: Int): Drawable? {
        return try {
            if (validateResource(resourceId)) {
                ContextCompat.getDrawable(context, resourceId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading drawable resource: $resourceId", e)
            null
        }
    }
}

/**
 * Resource provider for cursor drawables
 * This allows for easy testing and resource switching
 *
 * Enhanced with resource validation to prevent crashes from missing resources
 */
class ResourceProvider(private val context: Context) {
    companion object {
        private const val TAG = "ResourceProvider"
    }

    // Fallback resource - cannot be const because R.drawable values are not compile-time constants
    private val FALLBACK_RESOURCE = com.augmentalis.voiceos.cursor.R.drawable.cursor_round

    /**
     * Validate that a drawable resource exists and is accessible
     * @param resId Resource ID to validate
     * @return True if resource is valid and accessible, false otherwise
     */
    private fun isResourceValid(resId: Int): Boolean {
        return try {
            context.resources.getDrawable(resId, null)
            true
        } catch (e: android.content.res.Resources.NotFoundException) {
            android.util.Log.e(TAG, "Resource not found: $resId", e)
            false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading resource: $resId", e)
            false
        }
    }

    /**
     * Get a validated drawable resource, with fallback to default cursor
     * @param resId Resource ID to load
     * @return Valid resource ID, or fallback resource if invalid
     */
    private fun getValidatedResource(resId: Int): Int {
        return if (isResourceValid(resId)) {
            resId
        } else {
            android.util.Log.w(TAG, "Using fallback resource for invalid resource: $resId")
            FALLBACK_RESOURCE
        }
    }

    // Map cursor types to actual VoiceCursor drawable resources with validation
    fun getHandCursorResource(): Int = getValidatedResource(com.augmentalis.voiceos.cursor.R.drawable.cursor_hand)

    fun getRoundCursorResource(): Int = getValidatedResource(com.augmentalis.voiceos.cursor.R.drawable.cursor_round)

    fun getCursorBitmap(size: Int): Bitmap{
        val cursorId = getRoundCursorResource()
        val drawable = ContextCompat.getDrawable(context, cursorId)
            ?: throw IllegalStateException("Cursor drawable not found: $cursorId")
        val wrappedDrawable = DrawableCompat.wrap(drawable)
        val width = (size * 0.6).toInt()
        val cursor = drawableToBitmap(wrappedDrawable)
       return cursor.scale(width, width)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            createBitmap(1, 1)
        } else {
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun getCustomCursorResource(type: CursorType): Int {
        val resId = when(type) {
            CursorType.Custom -> com.augmentalis.voiceos.cursor.R.drawable.cursor_round_transparent
            CursorType.Hand -> com.augmentalis.voiceos.cursor.R.drawable.cursor_hand
            CursorType.Normal -> com.augmentalis.voiceos.cursor.R.drawable.cursor_round
        }
        return getValidatedResource(resId)
    }

    // Support all cursor shape options from settings with validation
    fun getCursorResourceByName(resourceName: String): Int {
        val resId = when(resourceName) {
            "cursor_round" -> com.augmentalis.voiceos.cursor.R.drawable.cursor_round
            "cursor_round_transparent" -> com.augmentalis.voiceos.cursor.R.drawable.cursor_round_transparent
            "cursor_crosshair" -> com.augmentalis.voiceos.cursor.R.drawable.cursor_crosshair
            "cursor_hand" -> com.augmentalis.voiceos.cursor.R.drawable.cursor_hand
            "ic_cursor_circular_red" -> com.augmentalis.voiceos.cursor.R.drawable.ic_cursor_circular_red
            "ic_cursor_circular_blue" -> com.augmentalis.voiceos.cursor.R.drawable.ic_cursor_circular_blue
            else -> FALLBACK_RESOURCE // fallback to round cursor for unknown names
        }
        return getValidatedResource(resId)
    }
}