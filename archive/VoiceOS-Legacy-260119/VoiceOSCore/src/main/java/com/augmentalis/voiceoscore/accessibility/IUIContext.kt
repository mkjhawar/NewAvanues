/**
 * IUIContext.kt - Interface for UI operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Purpose: Segregated interface for UI context (Interface Segregation Principle)
 * Part of Phase 1: SOLID Refactoring - Interface Segregation
 */

package com.augmentalis.voiceoscore.accessibility

import android.view.WindowManager

/**
 * Interface for UI operations
 *
 * Provides access to UI-related operations including overlays, toasts, and haptic feedback.
 * Focused interface following Interface Segregation Principle.
 *
 * @see IVoiceOSContext Aggregate interface combining all contexts
 */
interface IUIContext {

    /**
     * Window manager for display operations
     *
     * Used for overlay and window management operations.
     */
    val windowManager: WindowManager

    /**
     * Show toast message to user
     *
     * @param message Message to display
     */
    fun showToast(message: String)

    /**
     * Vibrate device
     *
     * @param duration Duration in milliseconds
     */
    fun vibrate(duration: Long)
}
