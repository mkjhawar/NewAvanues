/**
 * GestureService.kt - Gesture processing service implementation
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.services

import android.content.Context
import com.augmentalis.voicekeyboard.interfaces.GestureProcessor
import com.augmentalis.voicekeyboard.gestures.GestureTypingHandler

/**
 * Service implementation for gesture processing
 * Wraps GestureTypingHandler to implement the interface
 */
class GestureService(context: Context) : GestureProcessor {
    
    private val gestureHandler = GestureTypingHandler(context)
    
    override fun processGesture(points: List<Pair<Float, Float>>, callback: (String) -> Unit) {
        gestureHandler.processGesture(points, callback)
    }
    
    override fun release() {
        gestureHandler.release()
    }
}