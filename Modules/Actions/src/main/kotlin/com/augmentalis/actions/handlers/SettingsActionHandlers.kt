package com.augmentalis.actions.handlers

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.IntentActionHandler

/**
 * Action handler for opening security settings.
 */
class OpenSecurityActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "OpenSecurityHandler"
    }

    override val intent = "open_security"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening security settings for utterance: '$utterance'")

            val settingsIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)

            Log.i(TAG, "Opened security settings")
            ActionResult.Success(message = "Opening Security settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open security settings", e)
            ActionResult.Failure(
                message = "Failed to open security settings: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for opening connection/network settings.
 */
class OpenConnectionActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "OpenConnectionHandler"
    }

    override val intent = "open_connection"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening connection settings for utterance: '$utterance'")

            val settingsIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)

            Log.i(TAG, "Opened connection settings")
            ActionResult.Success(message = "Opening Connection settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open connection settings", e)
            ActionResult.Failure(
                message = "Failed to open connection settings: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for opening sound settings.
 */
class OpenSoundActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "OpenSoundHandler"
    }

    override val intent = "open_sound"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening sound settings for utterance: '$utterance'")

            val settingsIntent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)

            Log.i(TAG, "Opened sound settings")
            ActionResult.Success(message = "Opening Sound settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open sound settings", e)
            ActionResult.Failure(
                message = "Failed to open sound settings: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for opening display settings.
 */
class OpenDisplayActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "OpenDisplayHandler"
    }

    override val intent = "open_display"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening display settings for utterance: '$utterance'")

            val settingsIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)

            Log.i(TAG, "Opened display settings")
            ActionResult.Success(message = "Opening Display settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open display settings", e)
            ActionResult.Failure(
                message = "Failed to open display settings: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for opening about/device info settings.
 */
class OpenAboutActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "OpenAboutHandler"
    }

    override val intent = "open_about"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening about settings for utterance: '$utterance'")

            val settingsIntent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)

            Log.i(TAG, "Opened about settings")
            ActionResult.Success(message = "Opening About device")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open about settings", e)
            ActionResult.Failure(
                message = "Failed to open about settings: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for opening quick settings panel.
 */
class QuickSettingsActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "QuickSettingsHandler"
    }

    override val intent = "quick_settings"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening quick settings for utterance: '$utterance'")

            // Use statusbar service to expand quick settings
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManager.getMethod("expandSettingsPanel")
            expandMethod.invoke(statusBarService)

            Log.i(TAG, "Opened quick settings")
            ActionResult.Success(message = "Opening Quick Settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open quick settings, trying fallback", e)
            // Fallback to main settings
            try {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                ActionResult.Success(message = "Opening Settings")
            } catch (e2: Exception) {
                ActionResult.Failure(
                    message = "Failed to open quick settings: ${e.message}",
                    exception = e
                )
            }
        }
    }
}
