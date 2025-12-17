/**
 * GestureProcessor.kt - Interface for gesture processing
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.interfaces

/**
 * Interface for processing gesture input
 */
interface GestureProcessor {
    fun processGesture(points: List<Pair<Float, Float>>, callback: (String) -> Unit)
    fun release()
}