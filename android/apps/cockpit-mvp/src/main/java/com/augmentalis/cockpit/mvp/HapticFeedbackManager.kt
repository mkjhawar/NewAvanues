package com.augmentalis.cockpit.mvp

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages haptic feedback for touch interactions in spatial mode
 *
 * Provides tactile confirmation for:
 * - Button taps (light, medium)
 * - Success actions (add window, reset)
 * - Error states (max windows reached)
 * - Gestures (swipe, pinch)
 *
 * Supports Android API 26+ (Oreo) with fallback for older devices
 */
class HapticFeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Light tap for close button
     * Duration: 10ms
     * Use: Window close, minor interactions
     */
    fun performLightTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(10)
        }
    }

    /**
     * Medium tap for mode toggle, layout cycle
     * Duration: 20ms
     * Use: Mode switches, layout changes, settings
     */
    fun performMediumTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(20)
        }
    }

    /**
     * Success feedback for add/reset
     * Pattern: 10ms-50ms-10ms
     * Use: Window added, workspace reset, successful actions
     */
    fun performSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 10, 50, 10), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 10, 50, 10), -1)
        }
    }

    /**
     * Error/limit feedback
     * Pattern: 50ms-100ms-50ms
     * Use: Max windows reached, invalid action, errors
     */
    fun performError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(200)
        }
    }

    /**
     * Gesture feedback for swipe/pinch
     * Duration: 5ms
     * Use: Layout swipe, pinch-to-scale
     */
    fun performGesture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(5, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(5)
        }
    }

    /**
     * Check if haptic feedback is available on this device
     */
    fun isAvailable(): Boolean {
        return vibrator?.hasVibrator() == true
    }
}
