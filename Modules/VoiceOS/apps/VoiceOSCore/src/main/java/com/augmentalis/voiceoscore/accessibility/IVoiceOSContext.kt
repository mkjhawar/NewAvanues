/**
 * IVoiceOSContext.kt - Interface providing context for action handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-17
 *
 * Purpose: Abstracts VoiceOSService dependencies for testability and SOLID compliance (DIP)
 * Allows handlers to depend on interface rather than concrete service implementation
 */
package com.augmentalis.voiceoscore.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Interface providing context for action handlers
 * Abstracts VoiceOSService dependencies for testability and SOLID compliance
 *
 * VOS4 Exception: Interface justified for Dependency Inversion Principle
 * - Allows handlers to depend on abstraction instead of concrete VoiceOSService
 * - Enables testing with mock implementations
 * - Follows SOLID principle: Depend on abstractions, not concretions
 */
interface IVoiceOSContext {

    /**
     * Application context
     */
    val context: Context

    /**
     * Accessibility service instance
     */
    val accessibilityService: AccessibilityService

    /**
     * Window manager for display operations
     */
    val windowManager: WindowManager

    /**
     * Package manager for app operations
     */
    val packageManager: PackageManager

    /**
     * Get root accessibility node of active window
     * @return Root node or null if no active window
     */
    val rootInActiveWindow: AccessibilityNodeInfo?

    /**
     * Perform global accessibility action
     * @param action Global action constant from AccessibilityService
     * @return true if action was performed
     */
    fun performGlobalAction(action: Int): Boolean

    /**
     * Get app launch commands map
     * @return Map of command strings to package names
     */
    fun getAppCommands(): Map<String, String>

    /**
     * Get system service by name
     * @param name Service name constant from Context
     * @return Service instance or null
     */
    fun getSystemService(name: String): Any?

    /**
     * Start activity with given intent
     * @param intent Intent to start activity
     */
    fun startActivity(intent: android.content.Intent)

    /**
     * Show toast message to user
     * @param message Message to display
     */
    fun showToast(message: String)

    /**
     * Vibrate device
     * @param duration Duration in milliseconds
     */
    fun vibrate(duration: Long)
}
