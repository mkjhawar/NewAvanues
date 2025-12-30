/**
 * VivokaMocks.kt - Mock implementations for Vivoka VSDK
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Provides mock implementations of Vivoka VSDK classes for testing
 * when the actual Vivoka SDK is not available.
 */
package com.vivoka.vsdk

import android.content.Context

/**
 * Mock Vsdk class
 */
object Vsdk {
    fun initialize(_context: Context): Boolean = false
    
    fun setApiKey(_apiKey: String) {}
    
    fun isAvailable(): Boolean = false
}

/**
 * Mock AsrEngine class
 */
class AsrEngine {
    fun start() {}
    fun stop() {}
    fun isListening(): Boolean = false
}