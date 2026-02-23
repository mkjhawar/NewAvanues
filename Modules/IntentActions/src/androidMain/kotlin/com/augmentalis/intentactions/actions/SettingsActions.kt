package com.augmentalis.intentactions.actions

import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.augmentalis.intentactions.EntityType
import com.augmentalis.intentactions.ExtractedEntities
import com.augmentalis.intentactions.IIntentAction
import com.augmentalis.intentactions.IntentCategory
import com.augmentalis.intentactions.IntentResult
import com.augmentalis.intentactions.PlatformContext

/**
 * Opens the main Android Settings app.
 *
 * Also handles specific settings subsections based on the query entity,
 * routing to WiFi, Bluetooth, Display, Sound, etc.
 */
object OpenSettingsAction : IIntentAction {
    private const val TAG = "OpenSettingsAction"

    override val intentId = "open_settings"
    override val category = IntentCategory.SYSTEM_SETTINGS
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Opening settings with entities: $entities")

            val query = (entities.query ?: "").lowercase()

            val settingsAction = when {
                query.contains("wifi") || query.contains("wi-fi") -> Settings.ACTION_WIFI_SETTINGS
                query.contains("bluetooth") -> Settings.ACTION_BLUETOOTH_SETTINGS
                query.contains("display") || query.contains("brightness") -> Settings.ACTION_DISPLAY_SETTINGS
                query.contains("sound") || query.contains("audio") || query.contains("volume") -> Settings.ACTION_SOUND_SETTINGS
                query.contains("battery") || query.contains("power") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                query.contains("storage") -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
                query.contains("security") -> Settings.ACTION_SECURITY_SETTINGS
                query.contains("location") || query.contains("gps") -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                query.contains("accessibility") -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                query.contains("notification") -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                query.contains("app") || query.contains("application") -> Settings.ACTION_APPLICATION_SETTINGS
                query.contains("date") || query.contains("time") -> Settings.ACTION_DATE_SETTINGS
                query.contains("language") -> Settings.ACTION_LOCALE_SETTINGS
                query.contains("network") || query.contains("data") -> Settings.ACTION_DATA_ROAMING_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }

            val settingsIntent = Intent(settingsAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (settingsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(settingsIntent)

                val settingName = settingsAction.substringAfterLast(".")
                    .replace("_", " ")
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }

                Log.i(TAG, "Opened settings: $settingsAction")
                IntentResult.Success(message = "Opening $settingName")
            } else {
                val mainSettings = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(mainSettings)

                Log.w(TAG, "Specific settings not found, opening main settings")
                IntentResult.Success(message = "Opening Settings")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings", e)
            IntentResult.Failed(
                reason = "Failed to open settings: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Opens a specific settings subsection by name.
 *
 * Maps common subsection names to their Android Settings action constants.
 * Subsection names: security, connection, sound, display, about, quick_settings.
 */
object OpenSettingsSubsectionAction : IIntentAction {
    private const val TAG = "OpenSettingsSubsectionAction"

    override val intentId = "open_settings_subsection"
    override val category = IntentCategory.SYSTEM_SETTINGS
    override val requiredEntities = listOf(EntityType.QUERY)

    private val SUBSECTION_MAP = mapOf(
        "security" to Settings.ACTION_SECURITY_SETTINGS,
        "connection" to Settings.ACTION_WIRELESS_SETTINGS,
        "wireless" to Settings.ACTION_WIRELESS_SETTINGS,
        "network" to Settings.ACTION_WIRELESS_SETTINGS,
        "sound" to Settings.ACTION_SOUND_SETTINGS,
        "audio" to Settings.ACTION_SOUND_SETTINGS,
        "volume" to Settings.ACTION_SOUND_SETTINGS,
        "display" to Settings.ACTION_DISPLAY_SETTINGS,
        "screen" to Settings.ACTION_DISPLAY_SETTINGS,
        "brightness" to Settings.ACTION_DISPLAY_SETTINGS,
        "about" to Settings.ACTION_DEVICE_INFO_SETTINGS,
        "device info" to Settings.ACTION_DEVICE_INFO_SETTINGS,
        "wifi" to Settings.ACTION_WIFI_SETTINGS,
        "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
        "battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
        "storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        "location" to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
        "accessibility" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
        "notifications" to Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
        "apps" to Settings.ACTION_APPLICATION_SETTINGS,
        "date" to Settings.ACTION_DATE_SETTINGS,
        "time" to Settings.ACTION_DATE_SETTINGS,
        "language" to Settings.ACTION_LOCALE_SETTINGS
    )

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Opening settings subsection with entities: $entities")

            val subsection = entities.query?.lowercase()?.trim()
            if (subsection.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.QUERY,
                    prompt = "Which settings section? (WiFi, Bluetooth, Display, Sound, etc.)"
                )
            }

            // Try quick settings panel first
            if (subsection == "quick" || subsection == "quick settings" || subsection == "quick_settings") {
                return openQuickSettings(context)
            }

            // Find the matching settings action
            val settingsAction = SUBSECTION_MAP.entries
                .firstOrNull { (key, _) -> subsection.contains(key) }
                ?.value
                ?: Settings.ACTION_SETTINGS

            val settingsIntent = Intent(settingsAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(settingsIntent)

            val displayName = settingsAction.substringAfterLast(".")
                .replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() }

            Log.i(TAG, "Opened settings subsection: $settingsAction")
            IntentResult.Success(message = "Opening $displayName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings subsection", e)
            IntentResult.Failed(
                reason = "Failed to open settings: ${e.message}",
                exception = e
            )
        }
    }

    private fun openQuickSettings(context: PlatformContext): IntentResult {
        return try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManager.getMethod("expandSettingsPanel")
            expandMethod.invoke(statusBarService)

            Log.i(TAG, "Opened quick settings panel")
            IntentResult.Success(message = "Opening Quick Settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open quick settings, falling back to main settings", e)
            try {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                IntentResult.Success(message = "Opening Settings")
            } catch (e2: Exception) {
                IntentResult.Failed(
                    reason = "Failed to open settings: ${e.message}",
                    exception = e
                )
            }
        }
    }
}
