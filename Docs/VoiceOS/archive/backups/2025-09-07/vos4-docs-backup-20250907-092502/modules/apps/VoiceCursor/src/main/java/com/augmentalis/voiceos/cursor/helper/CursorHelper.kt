/**
 * Helper.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/helper/Helper.kt
 * 
 * Created: 2025-01-23 00:15 PST
 * Last Modified: 2025-01-26
 * Author: VOS4 Development Team
 * Version: 2.0.0
 * 
 * Purpose: Helper classes for cursor sensor integration and drag operations
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-23 00:15 PST): Initial port from VoiceOS
 * - v1.1.0 (2025-01-23 00:35 PDT): Updated package name to com.augmentalis.voiceos.cursor
 * - v1.2.0 (2025-01-23 01:30 PDT): Phase 2 - Updated to modern sensor API with Android 9-16 & XR compatibility
 * - v2.0.0 (2025-01-26): Integrated into VoiceCursor module
 */

package com.augmentalis.voiceos.cursor.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

/**
 * Utility methods for cursor operations
 */
object CursorHelper {
    
    /**
     * Convert vector drawable to bitmap for use in animations
     */
    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, drawableId)
            
            when (drawable) {
                is VectorDrawable -> {
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
                is VectorDrawableCompat -> {
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
                else -> {
                    // Handle other drawable types
                    drawable?.let {
                        val bitmap = Bitmap.createBitmap(
                            it.intrinsicWidth,
                            it.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        it.setBounds(0, 0, canvas.width, canvas.height)
                        it.draw(canvas)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Helper for drag operations
 */
class DragHelper {
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragEndX = 0f
    private var dragEndY = 0f
    
    fun startDrag(x: Float, y: Float) {
        isDragging = true
        dragStartX = x
        dragStartY = y
    }
    
    fun updateDrag(x: Float, y: Float) {
        if (isDragging) {
            dragEndX = x
            dragEndY = y
        }
    }
    
    fun endDrag(): DragResult? {
        return if (isDragging) {
            val result = DragResult(dragStartX, dragStartY, dragEndX, dragEndY)
            isDragging = false
            result
        } else {
            null
        }
    }
    
    fun cancelDrag() {
        isDragging = false
    }
    
    fun isDragging(): Boolean = isDragging
    
    data class DragResult(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float
    )
}