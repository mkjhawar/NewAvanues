/**
 * KeyboardConstants.kt - Keyboard constants and action definitions
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.avanues.utils

object KeyboardConstants {
    // Broadcast Actions
    const val ACTION_VOICE_KEY_CODE = "com.augmentalis.action.voice_key_code"
    const val ACTION_VOICE_KEY_COMMAND = "com.augmentalis.action.voice_key_command"
    const val ACTION_CLOSE_COMMAND = "com.augmentalis.action.close"
    const val ACTION_VOICE_SWITCH_KEYBOARD = "com.augmentalis.action.switch_keyboard"
    const val ACTION_VOICE_COMMAND_SHOW_INPUT = "com.augmentalis.action.open_keyboard"
    const val ACTION_FREE_SPEECH_COMMAND = "com.augmentalis.action.free_speech"
    const val ACTION_DICTATION_STATUS = "com.augmentalis.action.dictation_status"
    const val ACTION_LAUNCH_DICTATION = "com.augmentalis.action.launch_dictation"
    const val ACTION_KEYBOARD_OPEN_STATUS = "com.augmentalis.action.keyboard_open_status"
    const val ACTION_KEYBOARD_HEIGHT = "com.augmentalis.action.height"
    const val ACTION_KEYBOARD_COMMAND_BAR = "com.augmentalis.action.keyboard_command_bar"
    
    // Broadcast Keys
    const val KEY_COMMAND = "command"
    const val KEY_DICTATION_ACTIVE = "is_dictation_active"
    const val KEY_OPENED = "is_keyboard_opened"
    const val KEY_KEYBOARD_HEIGHT = "is_keyboard_height"
    const val KEY_OVERLAP_KEYBOARD = "is_overlap_keyboard"
    
    // Key Codes
    const val KEYCODE_SHIFT = -1
    const val KEYCODE_MODE_CHANGE = -2
    const val KEYCODE_CANCEL = -3
    const val KEYCODE_DONE = -4
    const val KEYCODE_DELETE = -5
    const val KEYCODE_ALT = -6
    const val KEYCODE_VOICE = -7
    const val KEYCODE_SETTINGS = -8
    const val KEYCODE_EMOJI = -9
    const val KEYCODE_LANGUAGE_SWITCH = -10
    const val KEYCODE_DICTATION = -11
    const val KEYCODE_SYMBOLS = -12
    
    // Swipe Constants
    const val MIN_SWIPE_DISTANCE = 100
    const val MAX_SWIPE_VELOCITY = 100
    
    // Gesture Typing
    const val GESTURE_TRAIL_FADE_START = 100
    const val GESTURE_TRAIL_WIDTH = 3f
    
    // Dictation
    const val DEFAULT_DICTATION_TIMEOUT = 5 // seconds
    const val DEFAULT_DICTATION_START_COMMAND = "dictation"
    const val DEFAULT_DICTATION_STOP_COMMAND = "end dictation"
}

object KeyboardActions {
    const val KEYCODE_SHIFT = KeyboardConstants.KEYCODE_SHIFT
    const val KEYCODE_MODE_CHANGE = KeyboardConstants.KEYCODE_MODE_CHANGE
    const val KEYCODE_VOICE = KeyboardConstants.KEYCODE_VOICE
    const val KEYCODE_SETTINGS = KeyboardConstants.KEYCODE_SETTINGS
    const val KEYCODE_EMOJI = KeyboardConstants.KEYCODE_EMOJI
    const val KEYCODE_DICTATION = KeyboardConstants.KEYCODE_DICTATION
}