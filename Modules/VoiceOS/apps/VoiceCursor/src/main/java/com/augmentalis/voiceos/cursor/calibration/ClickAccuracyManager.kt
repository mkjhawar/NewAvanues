/**
 * ClickAccuracyManager.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/calibration/ClickAccuracyManager.kt
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Manage click accuracy calibration and target assistance for VoiceCursor
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-09-05): Initial creation with click calibration and target assistance
 */

package com.augmentalis.voiceos.cursor.calibration

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.geometry.Offset
import com.augmentalis.voiceos.cursor.view.ClickCalibrationData
import kotlin.math.*

/**
 * Manages click accuracy calibration and provides target assistance
 */
class ClickAccuracyManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "cursor_accuracy_prefs"
        private const val KEY_OFFSET_X = "click_offset_x"
        private const val KEY_OFFSET_Y = "click_offset_y"
        private const val KEY_ACCURACY = "click_accuracy"
        private const val KEY_SAMPLE_COUNT = "sample_count"
        private const val KEY_CALIBRATION_VERSION = "calibration_version"
        
        private const val SNAP_THRESHOLD = 80f // Pixels - distance to snap to targets
        private const val MIN_TARGET_SIZE = 48f // Minimum target size in pixels (Android guideline)
        private const val CALIBRATION_VERSION = 1
        
        // Target types with different snap priorities
        private val CLICKABLE_CLASSES = setOf(
            "android.widget.Button",
            "android.widget.ImageButton",
            "androidx.compose.ui.platform.AndroidComposeView",
            "android.widget.TextView",
            "android.widget.EditText",
            "android.widget.CheckBox",
            "android.widget.RadioButton",
            "android.widget.Switch",
            "android.widget.ToggleButton",
            "android.view.View"
        )
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var calibrationData: ClickCalibrationData
    private val targetCache = mutableMapOf<String, ClickTarget>()
    private var lastCacheUpdate = 0L
    
    init {
        calibrationData = loadCalibrationData()
        
        // Reset calibration if version changed
        if (prefs.getInt(KEY_CALIBRATION_VERSION, 0) != CALIBRATION_VERSION) {
            resetCalibration()
        }
    }
    
    /**
     * Load calibration data from preferences
     */
    private fun loadCalibrationData(): ClickCalibrationData {
        return ClickCalibrationData(
            offsetX = prefs.getFloat(KEY_OFFSET_X, 0f),
            offsetY = prefs.getFloat(KEY_OFFSET_Y, 0f),
            accuracy = prefs.getFloat(KEY_ACCURACY, 1.0f),
            sampleCount = prefs.getInt(KEY_SAMPLE_COUNT, 0)
        )
    }
    
    /**
     * Save calibration data to preferences
     */
    private fun saveCalibrationData() {
        prefs.edit().apply {
            putFloat(KEY_OFFSET_X, calibrationData.offsetX)
            putFloat(KEY_OFFSET_Y, calibrationData.offsetY)
            putFloat(KEY_ACCURACY, calibrationData.accuracy)
            putInt(KEY_SAMPLE_COUNT, calibrationData.sampleCount)
            putInt(KEY_CALIBRATION_VERSION, CALIBRATION_VERSION)
            apply()
        }
    }
    
    /**
     * Reset calibration to default values
     */
    fun resetCalibration() {
        calibrationData = ClickCalibrationData()
        saveCalibrationData()
        targetCache.clear()
    }
    
    /**
     * Add a calibration sample based on intended vs actual click position
     */
    fun addCalibrationSample(intendedPosition: Offset, actualClickPosition: Offset) {
        val deltaX = actualClickPosition.x - intendedPosition.x
        val deltaY = actualClickPosition.y - intendedPosition.y
        
        // Weighted average - newer samples have slightly more weight
        val weight = 1.0f / (calibrationData.sampleCount + 1).coerceAtMost(10)
        val inverseWeight = 1f - weight
        
        calibrationData = calibrationData.copy(
            offsetX = calibrationData.offsetX * inverseWeight + deltaX * weight,
            offsetY = calibrationData.offsetY * inverseWeight + deltaY * weight,
            sampleCount = calibrationData.sampleCount + 1,
            accuracy = calculateAccuracy(deltaX, deltaY)
        )
        
        saveCalibrationData()
    }
    
    /**
     * Calculate accuracy score based on click deviation
     */
    private fun calculateAccuracy(deltaX: Float, deltaY: Float): Float {
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        // Accuracy score: 1.0 = perfect, decreases with distance
        return (1.0f - (distance / 100f)).coerceIn(0.1f, 1.0f)
    }
    
    /**
     * Apply click calibration to cursor position
     */
    fun applyCalibratedPosition(rawPosition: Offset): Offset {
        return Offset(
            x = rawPosition.x - calibrationData.offsetX,
            y = rawPosition.y - calibrationData.offsetY
        )
    }
    
    /**
     * Find the best click target near the cursor position
     */
    fun findBestTarget(cursorPosition: Offset, rootView: View?): ClickTargetResult? {
        rootView ?: return null
        
        val currentTime = System.currentTimeMillis()
        // Update cache every 500ms to balance performance and accuracy
        if (currentTime - lastCacheUpdate > 500) {
            updateTargetCache(rootView)
            lastCacheUpdate = currentTime
        }
        
        var bestTarget: ClickTarget? = null
        var bestScore = Float.MAX_VALUE
        
        targetCache.values.forEach { target ->
            val distance = calculateDistance(cursorPosition, target.center)
            if (distance <= SNAP_THRESHOLD) {
                val score = calculateTargetScore(cursorPosition, target, distance)
                if (score < bestScore) {
                    bestScore = score
                    bestTarget = target
                }
            }
        }
        
        return bestTarget?.let { target ->
            val snapPosition = calculateSnapPosition(cursorPosition, target)
            ClickTargetResult(
                target = target,
                snapPosition = snapPosition,
                confidence = 1f - (bestScore / SNAP_THRESHOLD)
            )
        }
    }
    
    /**
     * Update the cache of clickable targets
     */
    private fun updateTargetCache(rootView: View) {
        targetCache.clear()
        findClickableTargets(rootView, targetCache)
    }
    
    /**
     * Recursively find clickable targets in the view hierarchy
     */
    private fun findClickableTargets(view: View, cache: MutableMap<String, ClickTarget>) {
        if (!view.isShown) return
        
        // Check if view is clickable
        if (view.isClickable || view.isFocusable) {
            val rect = Rect()
            if (view.getGlobalVisibleRect(rect) && rect.width() > 0 && rect.height() > 0) {
                val priority = getTargetPriority(view)
                val target = ClickTarget(
                    view = view,
                    bounds = rect,
                    center = Offset(rect.exactCenterX(), rect.exactCenterY()),
                    priority = priority,
                    type = getTargetType(view)
                )
                
                val key = "${view.javaClass.simpleName}_${rect.left}_${rect.top}"
                cache[key] = target
            }
        }
        
        // Recursively check children
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findClickableTargets(view.getChildAt(i), cache)
            }
        }
    }
    
    /**
     * Get priority for target type (lower = higher priority)
     */
    private fun getTargetPriority(view: View): Int {
        return when {
            view.javaClass.simpleName.contains("Button") -> 1
            view.isClickable && view.isFocusable -> 2
            view.isClickable -> 3
            view.isFocusable -> 4
            else -> 5
        }
    }
    
    /**
     * Get target type for accessibility
     */
    private fun getTargetType(view: View): TargetType {
        return when {
            view.javaClass.simpleName.contains("Button") -> TargetType.BUTTON
            view.javaClass.simpleName.contains("Text") -> TargetType.TEXT
            view.javaClass.simpleName.contains("Edit") -> TargetType.INPUT
            view.javaClass.simpleName.contains("Image") -> TargetType.IMAGE
            else -> TargetType.GENERIC
        }
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(point1: Offset, point2: Offset): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Calculate target score (lower is better)
     */
    private fun calculateTargetScore(cursorPos: Offset, target: ClickTarget, distance: Float): Float {
        var score = distance
        
        // Prioritize by target type
        score += target.priority * 10f
        
        // Prefer larger targets
        val targetSize = min(target.bounds.width(), target.bounds.height())
        if (targetSize < MIN_TARGET_SIZE) {
            score += (MIN_TARGET_SIZE - targetSize) * 0.5f
        }
        
        // Prefer targets that are more centered relative to cursor
        val targetRect = target.bounds
        val isWithinBounds = cursorPos.x >= targetRect.left && 
                           cursorPos.x <= targetRect.right && 
                           cursorPos.y >= targetRect.top && 
                           cursorPos.y <= targetRect.bottom
        
        if (isWithinBounds) {
            score *= 0.5f // Strong preference for targets cursor is already over
        }
        
        return score
    }
    
    /**
     * Calculate the best snap position for a target
     */
    private fun calculateSnapPosition(cursorPos: Offset, target: ClickTarget): Offset {
        val targetRect = target.bounds
        
        // If cursor is already within target bounds, prefer current position
        if (cursorPos.x >= targetRect.left && cursorPos.x <= targetRect.right &&
            cursorPos.y >= targetRect.top && cursorPos.y <= targetRect.bottom) {
            return cursorPos
        }
        
        // Otherwise, snap to center but prefer positions closer to cursor
        val snapX = when {
            cursorPos.x < targetRect.left -> targetRect.left + 20f
            cursorPos.x > targetRect.right -> targetRect.right - 20f
            else -> cursorPos.x
        }
        
        val snapY = when {
            cursorPos.y < targetRect.top -> targetRect.top + 20f
            cursorPos.y > targetRect.bottom -> targetRect.bottom - 20f
            else -> cursorPos.y
        }
        
        return Offset(snapX, snapY)
    }
    
    /**
     * Get current calibration data
     */
    fun getCalibrationData(): ClickCalibrationData = calibrationData
    
    /**
     * Check if calibration has enough samples to be considered reliable
     */
    fun isCalibrationReliable(): Boolean = calibrationData.sampleCount >= 5
    
    /**
     * Get calibration status as human-readable string
     */
    fun getCalibrationStatus(): String {
        return when {
            calibrationData.sampleCount == 0 -> "Not calibrated"
            calibrationData.sampleCount < 5 -> "Calibrating (${calibrationData.sampleCount}/5 samples)"
            calibrationData.accuracy > 0.8f -> "Excellent (${(calibrationData.accuracy * 100).toInt()}%)"
            calibrationData.accuracy > 0.6f -> "Good (${(calibrationData.accuracy * 100).toInt()}%)"
            calibrationData.accuracy > 0.4f -> "Fair (${(calibrationData.accuracy * 100).toInt()}%)"
            else -> "Poor (${(calibrationData.accuracy * 100).toInt()}%) - Consider recalibrating"
        }
    }
}

/**
 * Represents a clickable target on screen
 */
data class ClickTarget(
    val view: View,
    val bounds: Rect,
    val center: Offset,
    val priority: Int,
    val type: TargetType
)

/**
 * Result of target finding operation
 */
data class ClickTargetResult(
    val target: ClickTarget,
    val snapPosition: Offset,
    val confidence: Float
)

/**
 * Types of clickable targets
 */
enum class TargetType {
    BUTTON,
    TEXT,
    INPUT,
    IMAGE,
    GENERIC
}