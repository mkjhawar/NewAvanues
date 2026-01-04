/**
 * BlockedState.kt - Screen blocked state enumeration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-03
 *
 * Represents states where screen exploration should be paused
 * due to user authentication or permission requirements.
 */
package com.augmentalis.voiceoscore.learnapp.integration

/**
 * Screen Blocked State
 *
 * Enumeration of states that should pause automatic exploration:
 * - LOGIN_REQUIRED: Screen requires user authentication
 * - PERMISSION_REQUIRED: Screen requires permission grant
 */
enum class BlockedState {
    /**
     * Login screen detected - user authentication required
     * Examples: Login forms, sign-in dialogs, password prompts
     */
    LOGIN_REQUIRED,

    /**
     * Permission dialog detected - user action required
     * Examples: Location permission, camera access, storage permission
     */
    PERMISSION_REQUIRED
}
