package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler

/**
 * Action handler for opening Android Settings.
 *
 * Launches the system Settings app or specific settings screens
 * based on the user's utterance.
 *
 * Behavior:
 * - Opens main Settings by default
 * - Can open specific settings (WiFi, Bluetooth, etc.) based on utterance
 * - Uses FLAG_ACTIVITY_NEW_TASK for launching from non-activity context
 *
 * Intent classification examples:
 * - "Open settings"
 * - "Open security settings"
 * - "Go to settings"
 * - "Settings"
 *
 * @see IntentActionHandler
 */
class OpenSettingsActionHandler : IntentActionHandler {

    companion object {
        private const val TAG = "OpenSettingsHandler"
    }

    override val intent = "open_settings"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening settings for utterance: '$utterance'")

            val lowerUtterance = utterance.lowercase()

            // Determine which settings to open based on utterance
            val settingsAction = when {
                lowerUtterance.contains("wifi") || lowerUtterance.contains("wi-fi") ->
                    Settings.ACTION_WIFI_SETTINGS
                lowerUtterance.contains("bluetooth") ->
                    Settings.ACTION_BLUETOOTH_SETTINGS
                lowerUtterance.contains("display") || lowerUtterance.contains("brightness") ->
                    Settings.ACTION_DISPLAY_SETTINGS
                lowerUtterance.contains("sound") || lowerUtterance.contains("audio") || lowerUtterance.contains("volume") ->
                    Settings.ACTION_SOUND_SETTINGS
                lowerUtterance.contains("battery") || lowerUtterance.contains("power") ->
                    Settings.ACTION_BATTERY_SAVER_SETTINGS
                lowerUtterance.contains("storage") ->
                    Settings.ACTION_INTERNAL_STORAGE_SETTINGS
                lowerUtterance.contains("security") ->
                    Settings.ACTION_SECURITY_SETTINGS
                lowerUtterance.contains("location") || lowerUtterance.contains("gps") ->
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                lowerUtterance.contains("accessibility") ->
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
                lowerUtterance.contains("notification") ->
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                lowerUtterance.contains("app") || lowerUtterance.contains("application") ->
                    Settings.ACTION_APPLICATION_SETTINGS
                lowerUtterance.contains("date") || lowerUtterance.contains("time") ->
                    Settings.ACTION_DATE_SETTINGS
                lowerUtterance.contains("language") ->
                    Settings.ACTION_LOCALE_SETTINGS
                lowerUtterance.contains("network") || lowerUtterance.contains("data") ->
                    Settings.ACTION_DATA_ROAMING_SETTINGS
                else ->
                    Settings.ACTION_SETTINGS
            }

            val settingsIntent = Intent(settingsAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if the intent can be resolved
            if (settingsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(settingsIntent)

                val settingName = settingsAction.substringAfterLast(".")
                    .replace("_", " ")
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }

                Log.i(TAG, "Opened settings: $settingsAction")
                ActionResult.Success(
                    message = "Opening $settingName"
                )
            } else {
                // Fallback to main settings if specific setting not available
                val mainSettings = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(mainSettings)

                Log.w(TAG, "Specific settings not found, opening main settings")
                ActionResult.Success(
                    message = "Opening Settings"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings", e)
            ActionResult.Failure(
                message = "Failed to open settings: ${e.message}",
                exception = e
            )
        }
    }
}
