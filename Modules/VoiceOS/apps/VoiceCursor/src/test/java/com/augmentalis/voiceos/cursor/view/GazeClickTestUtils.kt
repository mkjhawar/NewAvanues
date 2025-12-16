/**
 * GazeClickTestUtils.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/view/GazeClickTestUtils.kt
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Test utilities for gaze click animation and cursor visibility
 * Module: VoiceCursor System
 */

package com.augmentalis.voiceos.cursor.view

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.cursor.core.CursorOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Utility class for testing gaze click functionality and cursor visibility
 */
object GazeClickTestUtils {
    
    private const val TAG = "GazeClickTestUtils"
    
    /**
     * Test gaze click animation at center screen
     */
    fun testGazeClickAtCenter(cursorView: CursorView, screenWidth: Int, screenHeight: Int) {
        val centerPosition = CursorOffset(screenWidth / 2f, screenHeight / 2f)
        cursorView.testGazeClickAnimation(centerPosition)
        Log.d(TAG, "Testing gaze click animation at center: $centerPosition")
    }
    
    /**
     * Test gaze click animation at specific position
     */
    fun testGazeClickAtPosition(cursorView: CursorView, x: Float, y: Float) {
        val position = CursorOffset(x, y)
        cursorView.testGazeClickAnimation(position)
        Log.d(TAG, "Testing gaze click animation at position: $position")
    }
    
    /**
     * Test cursor visibility by toggling on/off
     */
    fun testCursorVisibility(cursorView: CursorView) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG, "Testing cursor visibility - hiding cursor")
            cursorView.setVisible(false, animate = true)
            
            delay(2000)
            
            Log.d(TAG, "Testing cursor visibility - showing cursor")
            cursorView.setVisible(true, animate = true)
        }
    }
    
    /**
     * Test multiple gaze click animations in sequence
     */
    fun testMultipleGazeClicks(cursorView: CursorView, positions: List<CursorOffset>) {
        CoroutineScope(Dispatchers.Main).launch {
            for ((index, position) in positions.withIndex()) {
                Log.d(TAG, "Testing gaze click ${index + 1}/${positions.size} at: $position")
                cursorView.testGazeClickAnimation(position)
                
                // Wait for animation to complete before next one
                delay(1000)
            }
            Log.d(TAG, "Multiple gaze click test completed")
        }
    }
    
    /**
     * Test cursor tracking and gaze click sequence
     */
    fun testFullGazeSequence(cursorView: CursorView) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG, "Starting full gaze sequence test")
            
            // Start tracking
            cursorView.startTracking()
            delay(500)
            
            // Enable gaze
            cursorView.enableGaze()
            delay(500)
            
            // Test gaze click at current position
            cursorView.testGazeClickAnimation()
            delay(1000)
            
            // Center cursor
            cursorView.centerCursor()
            delay(500)
            
            // Test another gaze click at center
            cursorView.testGazeClickAnimation()
            delay(1000)
            
            Log.d(TAG, "Full gaze sequence test completed")
        }
    }
    
    /**
     * Test cursor z-order by creating standalone GazeClickView
     */
    fun testCursorZOrder(context: Context, position: CursorOffset) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG, "Testing cursor z-order with standalone GazeClickView")
            
            val gazeView = GazeClickView(context, position)
            val showResult = gazeView.show()
            
            Log.d(TAG, "GazeClickView show result: $showResult")
            
            // Let animation run
            delay(3000)
            
            gazeView.hide()
            Log.d(TAG, "GazeClickView z-order test completed")
        }
    }
    
    /**
     * Validate gaze click animation resources
     */
    fun validateGazeClickResources(context: Context): Boolean {
        return try {
            // Try to load all gaze circle drawables
            val smallCircle = context.getDrawable(com.augmentalis.voiceos.cursor.R.drawable.ic_gaze_circle_small)
            val mediumCircle = context.getDrawable(com.augmentalis.voiceos.cursor.R.drawable.ic_gaze_circle_medium)
            val bigCircle = context.getDrawable(com.augmentalis.voiceos.cursor.R.drawable.ic_gaze_circle_big)
            
            val allResourcesLoaded = smallCircle != null && mediumCircle != null && bigCircle != null
            Log.d(TAG, "Gaze click resources validation: $allResourcesLoaded")
            
            allResourcesLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Error validating gaze click resources", e)
            false
        }
    }
}