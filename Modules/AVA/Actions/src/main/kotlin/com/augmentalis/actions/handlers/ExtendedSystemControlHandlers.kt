package com.augmentalis.actions.handlers

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.IntentActionHandler
import com.augmentalis.actions.VoiceOSConnection

/**
 * Action handler for enabling airplane mode.
 */
class AirplaneModeOnActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "AirplaneModeOnHandler"
    }

    override val intent = "airplane_mode_on"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Enabling airplane mode for utterance: '$utterance'")

            // Open airplane mode settings (direct toggle requires system permissions)
            val settingsIntent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)

            Log.i(TAG, "Opened airplane mode settings")
            ActionResult.Success(message = "Opening Airplane mode settings - please enable it")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open airplane mode settings", e)
            ActionResult.Failure(
                message = "Failed to open airplane mode settings: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for disabling airplane mode.
 */
class AirplaneModeOffActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "AirplaneModeOffHandler"
    }

    override val intent = "airplane_mode_off"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Disabling airplane mode for utterance: '$utterance'")

            val settingsIntent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)

            Log.i(TAG, "Opened airplane mode settings")
            ActionResult.Success(message = "Opening Airplane mode settings - please disable it")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open airplane mode settings", e)
            ActionResult.Failure(
                message = "Failed to open airplane mode settings: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for increasing brightness.
 */
class BrightnessUpActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "BrightnessUpHandler"
    }

    override val intent = "brightness_up"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Increasing brightness for utterance: '$utterance'")

            // Get current brightness and increase
            val currentBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )

            val newBrightness = (currentBrightness + 25).coerceAtMost(255)

            // Try to set brightness (requires WRITE_SETTINGS permission)
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    newBrightness
                )

                val percentage = (newBrightness * 100 / 255)
                Log.i(TAG, "Brightness increased to $percentage%")
                ActionResult.Success(message = "Brightness increased to $percentage%")
            } else {
                // Open display settings if no permission
                val settingsIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                ActionResult.Success(message = "Opening Display settings to adjust brightness")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increase brightness", e)
            ActionResult.Failure(
                message = "Failed to increase brightness: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for decreasing brightness.
 */
class BrightnessDownActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "BrightnessDownHandler"
    }

    override val intent = "brightness_down"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Decreasing brightness for utterance: '$utterance'")

            val currentBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )

            val newBrightness = (currentBrightness - 25).coerceAtLeast(10)

            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    newBrightness
                )

                val percentage = (newBrightness * 100 / 255)
                Log.i(TAG, "Brightness decreased to $percentage%")
                ActionResult.Success(message = "Brightness decreased to $percentage%")
            } else {
                val settingsIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                ActionResult.Success(message = "Opening Display settings to adjust brightness")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrease brightness", e)
            ActionResult.Failure(
                message = "Failed to decrease brightness: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for locking the screen.
 *
 * Tries device admin first, falls back to VoiceOS accessibility service.
 */
class LockScreenActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "LockScreenHandler"
        private const val CATEGORY = "system"
    }

    override val intent = "lock_screen"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Locking screen for utterance: '$utterance'")

            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

            // Try device admin first
            devicePolicyManager.lockNow()

            Log.i(TAG, "Screen locked via device admin")
            ActionResult.Success(message = "Screen locked")
        } catch (e: SecurityException) {
            Log.w(TAG, "No device admin permission, trying VoiceOS", e)

            // Fallback to VoiceOS
            val voiceOS = VoiceOSConnection.getInstance(context)
            val result = voiceOS.executeCommand(intent, CATEGORY)

            when (result) {
                is VoiceOSConnection.CommandResult.Success -> {
                    ActionResult.Success(message = result.message)
                }
                is VoiceOSConnection.CommandResult.Failure -> {
                    ActionResult.Failure(message = result.error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock screen", e)
            ActionResult.Failure(
                message = "Failed to lock screen: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for taking a screenshot.
 *
 * Routes to VoiceOS accessibility service for execution.
 */
class ScreenshotActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "ScreenshotHandler"
        private const val CATEGORY = "system"
    }

    override val intent = "screenshot"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Taking screenshot for utterance: '$utterance'")

            val voiceOS = VoiceOSConnection.getInstance(context)
            val result = voiceOS.executeCommand(intent, CATEGORY)

            when (result) {
                is VoiceOSConnection.CommandResult.Success -> {
                    ActionResult.Success(message = result.message)
                }
                is VoiceOSConnection.CommandResult.Failure -> {
                    ActionResult.Failure(message = result.error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot", e)
            ActionResult.Failure(
                message = "Failed to take screenshot: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for unmuting volume.
 */
class VolumeUnmuteActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "VolumeUnmuteHandler"
    }

    override val intent = "unmute"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Unmuting volume for utterance: '$utterance'")

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)

            Log.i(TAG, "Volume unmuted")
            ActionResult.Success(message = "Volume unmuted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unmute volume", e)
            ActionResult.Failure(
                message = "Failed to unmute volume: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for mute intent (alias for volume_mute).
 */
class MuteActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "MuteHandler"
    }

    override val intent = "mute"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Muting volume for utterance: '$utterance'")

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)

            Log.i(TAG, "Volume muted")
            ActionResult.Success(message = "Volume muted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mute volume", e)
            ActionResult.Failure(
                message = "Failed to mute volume: ${e.message}",
                exception = e
            )
        }
    }
}
