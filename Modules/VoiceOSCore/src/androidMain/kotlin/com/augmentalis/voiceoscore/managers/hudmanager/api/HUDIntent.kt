/**
 * HUDIntent.kt
 * Path: CodeImport/HUDManager/src/main/java/com/augmentalis/hudmanager/api/HUDIntent.kt
 * 
 * Created: 2025-01-23
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Public intent definitions for HUD system access
 * Allows external apps to interact with HUDManager functionality
 */

package com.augmentalis.voiceoscore.managers.hudmanager.api

import android.content.Intent
import android.os.Bundle

/**
 * Public Intent API for HUD system
 * Enables third-party apps to interact with VoiceUI HUD
 */
object HUDIntent {
    
    // Intent actions
    const val ACTION_SHOW_HUD = "com.augmentalis.hudmanager.ACTION_SHOW_HUD"
    const val ACTION_HIDE_HUD = "com.augmentalis.hudmanager.ACTION_HIDE_HUD"
    const val ACTION_TOGGLE_HUD = "com.augmentalis.hudmanager.ACTION_TOGGLE_HUD"
    const val ACTION_SHOW_NOTIFICATION = "com.augmentalis.hudmanager.ACTION_SHOW_NOTIFICATION"
    const val ACTION_UPDATE_OVERLAY = "com.augmentalis.hudmanager.ACTION_UPDATE_OVERLAY"
    const val ACTION_SHOW_VOICE_COMMAND = "com.augmentalis.hudmanager.ACTION_SHOW_VOICE_COMMAND"
    const val ACTION_SHOW_DATA_VISUALIZATION = "com.augmentalis.hudmanager.ACTION_SHOW_DATA_VISUALIZATION"
    const val ACTION_SET_HUD_MODE = "com.augmentalis.hudmanager.ACTION_SET_HUD_MODE"
    const val ACTION_ENABLE_GAZE_TRACKING = "com.augmentalis.hudmanager.ACTION_ENABLE_GAZE_TRACKING"
    const val ACTION_DISABLE_GAZE_TRACKING = "com.augmentalis.hudmanager.ACTION_DISABLE_GAZE_TRACKING"
    const val ACTION_SET_ACCESSIBILITY_MODE = "com.augmentalis.hudmanager.ACTION_SET_ACCESSIBILITY_MODE"
    
    // Intent extras keys
    const val EXTRA_HUD_ELEMENT_ID = "hud_element_id"
    const val EXTRA_NOTIFICATION_MESSAGE = "notification_message"
    const val EXTRA_NOTIFICATION_DURATION = "notification_duration"
    const val EXTRA_NOTIFICATION_POSITION = "notification_position"
    const val EXTRA_NOTIFICATION_PRIORITY = "notification_priority"
    const val EXTRA_OVERLAY_ID = "overlay_id"
    const val EXTRA_OVERLAY_CONTENT = "overlay_content"
    const val EXTRA_VOICE_COMMAND = "voice_command"
    const val EXTRA_VOICE_CONFIDENCE = "voice_confidence"
    const val EXTRA_VOICE_CATEGORY = "voice_category"
    const val EXTRA_DATA_TYPE = "data_type"
    const val EXTRA_DATA_VALUES = "data_values"
    const val EXTRA_DATA_TITLE = "data_title"
    const val EXTRA_HUD_MODE = "hud_mode"
    const val EXTRA_FADE_DURATION = "fade_duration"
    const val EXTRA_POSITION_X = "position_x"
    const val EXTRA_POSITION_Y = "position_y"
    const val EXTRA_POSITION_Z = "position_z"
    const val EXTRA_ACCESSIBILITY_MODE = "accessibility_mode"
    const val EXTRA_HIGH_CONTRAST = "high_contrast"
    const val EXTRA_TEXT_SCALE = "text_scale"
    const val EXTRA_VOICE_SPEED = "voice_speed"
    
    // Result codes
    const val RESULT_SUCCESS = 0
    const val RESULT_ERROR_NOT_INITIALIZED = -1
    const val RESULT_ERROR_PERMISSION_DENIED = -2
    const val RESULT_ERROR_INVALID_PARAMS = -3
    const val RESULT_ERROR_SERVICE_UNAVAILABLE = -4
    
    // Permission for HUD access
    const val PERMISSION_USE_HUD = "com.augmentalis.hudmanager.permission.USE_HUD"
    const val PERMISSION_MANAGE_HUD = "com.augmentalis.hudmanager.permission.MANAGE_HUD"
    
    /**
     * Create intent to show HUD notification
     */
    fun createShowNotificationIntent(
        message: String,
        duration: Int = 3000,
        position: String = "CENTER",
        priority: String = "NORMAL"
    ): Intent {
        return Intent(ACTION_SHOW_NOTIFICATION).apply {
            putExtra(EXTRA_NOTIFICATION_MESSAGE, message)
            putExtra(EXTRA_NOTIFICATION_DURATION, duration)
            putExtra(EXTRA_NOTIFICATION_POSITION, position)
            putExtra(EXTRA_NOTIFICATION_PRIORITY, priority)
        }
    }
    
    /**
     * Create intent to show voice command
     */
    fun createShowVoiceCommandIntent(
        command: String,
        confidence: Float = 0.9f,
        category: String = "SYSTEM"
    ): Intent {
        return Intent(ACTION_SHOW_VOICE_COMMAND).apply {
            putExtra(EXTRA_VOICE_COMMAND, command)
            putExtra(EXTRA_VOICE_CONFIDENCE, confidence)
            putExtra(EXTRA_VOICE_CATEGORY, category)
        }
    }
    
    /**
     * Create intent to show data visualization
     */
    fun createShowDataVisualizationIntent(
        type: String,
        values: FloatArray,
        title: String = ""
    ): Intent {
        return Intent(ACTION_SHOW_DATA_VISUALIZATION).apply {
            putExtra(EXTRA_DATA_TYPE, type)
            putExtra(EXTRA_DATA_VALUES, values)
            putExtra(EXTRA_DATA_TITLE, title)
        }
    }
    
    /**
     * Create intent to update overlay
     */
    fun createUpdateOverlayIntent(
        overlayId: String,
        content: String
    ): Intent {
        return Intent(ACTION_UPDATE_OVERLAY).apply {
            putExtra(EXTRA_OVERLAY_ID, overlayId)
            putExtra(EXTRA_OVERLAY_CONTENT, content)
        }
    }
    
    /**
     * Create intent to set HUD mode
     */
    fun createSetHUDModeIntent(mode: String): Intent {
        return Intent(ACTION_SET_HUD_MODE).apply {
            putExtra(EXTRA_HUD_MODE, mode)
        }
    }
    
    /**
     * Create intent to toggle HUD visibility
     */
    fun createToggleHUDIntent(fadeDuration: Int = 300): Intent {
        return Intent(ACTION_TOGGLE_HUD).apply {
            putExtra(EXTRA_FADE_DURATION, fadeDuration)
        }
    }
    
    /**
     * Create intent to enable gaze tracking
     */
    fun createEnableGazeTrackingIntent(): Intent {
        return Intent(ACTION_ENABLE_GAZE_TRACKING)
    }
    
    /**
     * Create intent to set accessibility mode
     */
    fun createSetAccessibilityModeIntent(
        mode: String,
        highContrast: Boolean = false,
        textScale: Float = 1.0f,
        voiceSpeed: Float = 1.0f
    ): Intent {
        return Intent(ACTION_SET_ACCESSIBILITY_MODE).apply {
            putExtra(EXTRA_ACCESSIBILITY_MODE, mode)
            putExtra(EXTRA_HIGH_CONTRAST, highContrast)
            putExtra(EXTRA_TEXT_SCALE, textScale)
            putExtra(EXTRA_VOICE_SPEED, voiceSpeed)
        }
    }
    
    /**
     * Parse HUD intent result
     */
    fun parseResult(resultCode: Int): String {
        return when (resultCode) {
            RESULT_SUCCESS -> "Success"
            RESULT_ERROR_NOT_INITIALIZED -> "HUD not initialized"
            RESULT_ERROR_PERMISSION_DENIED -> "Permission denied"
            RESULT_ERROR_INVALID_PARAMS -> "Invalid parameters"
            RESULT_ERROR_SERVICE_UNAVAILABLE -> "Service unavailable"
            else -> "Unknown error"
        }
    }
}

/**
 * HUD position constants
 */
object HUDPosition {
    const val TOP_LEFT = "TOP_LEFT"
    const val TOP_CENTER = "TOP_CENTER"
    const val TOP_RIGHT = "TOP_RIGHT"
    const val CENTER_LEFT = "CENTER_LEFT"
    const val CENTER = "CENTER"
    const val CENTER_RIGHT = "CENTER_RIGHT"
    const val BOTTOM_LEFT = "BOTTOM_LEFT"
    const val BOTTOM_CENTER = "BOTTOM_CENTER"
    const val BOTTOM_RIGHT = "BOTTOM_RIGHT"
}

/**
 * HUD priority constants
 */
object HUDPriority {
    const val LOW = "LOW"
    const val NORMAL = "NORMAL"
    const val HIGH = "HIGH"
    const val CRITICAL = "CRITICAL"
}

/**
 * HUD mode constants
 */
object HUDModes {
    const val STANDARD = "STANDARD"
    const val MEETING = "MEETING"
    const val DRIVING = "DRIVING"
    const val WORKSHOP = "WORKSHOP"
    const val ACCESSIBILITY = "ACCESSIBILITY"
    const val GAMING = "GAMING"
    const val ENTERTAINMENT = "ENTERTAINMENT"
}

/**
 * Accessibility mode constants
 */
object AccessibilityModes {
    const val STANDARD = "STANDARD"
    const val VISION_IMPAIRED = "VISION_IMPAIRED"
    const val HEARING_IMPAIRED = "HEARING_IMPAIRED"
    const val MOTOR_IMPAIRED = "MOTOR_IMPAIRED"
    const val COGNITIVE_SUPPORT = "COGNITIVE_SUPPORT"
    const val FULL_ACCESSIBILITY = "FULL_ACCESSIBILITY"
}

/**
 * Data visualization types
 */
object DataVisualizationTypes {
    const val LINE_CHART = "LINE_CHART"
    const val BAR_CHART = "BAR_CHART"
    const val PIE_CHART = "PIE_CHART"
    const val SCATTER_PLOT = "SCATTER_PLOT"
    const val GAUGE = "GAUGE"
    const val PROGRESS_BAR = "PROGRESS_BAR"
}