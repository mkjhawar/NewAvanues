/**
 * ISpeechContext.kt - Interface for speech operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Purpose: Segregated interface for speech context (Interface Segregation Principle)
 * Part of Phase 1: SOLID Refactoring - Interface Segregation
 */

package com.augmentalis.voiceoscore.accessibility

import com.augmentalis.voiceos.cursor.core.CursorOffset

/**
 * Interface for speech and cursor operations
 *
 * Provides access to speech-related operations and cursor management.
 * Focused interface following Interface Segregation Principle.
 *
 * @see IVoiceOSContext Aggregate interface combining all contexts
 */
interface ISpeechContext {

    /**
     * Get app launch commands map
     *
     * @return Map of command strings to package names
     */
    fun getAppCommands(): Map<String, String>

    /**
     * Start activity with given intent
     *
     * @param intent Intent to start activity
     */
    fun startActivity(intent: android.content.Intent)

    /**
     * Check if cursor is currently visible
     *
     * @return true if cursor is visible, false otherwise
     */
    fun isCursorVisible(): Boolean

    /**
     * Get current cursor position
     *
     * @return CursorOffset with current X,Y coordinates
     */
    fun getCursorPosition(): CursorOffset
}
