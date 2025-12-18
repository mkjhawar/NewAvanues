// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/context/ContextEngine.kt
// created: 2025-11-02 00:00:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 4 - Context Engine
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.context

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Context engine for detecting active app and generating smart suggestions.
 *
 * Monitors system state to provide context-aware AI assistance:
 * - Active foreground app detection
 * - App category classification (browser, messenger, email, etc.)
 * - Screen text extraction (accessibility service integration)
 * - Smart suggestion generation based on context
 *
 * Requires:
 * - PACKAGE_USAGE_STATS permission for app detection
 * - Accessibility service for screen text (optional)
 *
 * @param context Android context
 * @author Manoj Jhawar
 */
class ContextEngine(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
        as? UsageStatsManager

    // Current context state
    private val _activeApp = MutableStateFlow<AppContext?>(null)
    val activeApp: StateFlow<AppContext?> = _activeApp.asStateFlow()

    private val _screenText = MutableStateFlow<String?>(null)
    val screenText: StateFlow<String?> = _screenText.asStateFlow()

    /**
     * Detect currently active foreground app
     *
     * @return AppContext with package name and category
     */
    suspend fun detectActiveApp(): AppContext? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val endTime = System.currentTimeMillis()
                val beginTime = endTime - 1000 // Last 1 second

                val usageEvents = usageStatsManager?.queryEvents(beginTime, endTime)
                var lastEvent: UsageEvents.Event? = null

                while (usageEvents?.hasNextEvent() == true) {
                    val event = UsageEvents.Event()
                    usageEvents.getNextEvent(event)

                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        lastEvent = event
                    }
                }

                val packageName = lastEvent?.packageName
                if (packageName != null && packageName != context.packageName) {
                    val appContext = AppContext(
                        packageName = packageName,
                        appName = getAppName(packageName),
                        category = classifyApp(packageName)
                    )
                    _activeApp.value = appContext
                    return@withContext appContext
                }
            } catch (e: Exception) {
                // Permission not granted or error accessing usage stats
            }
        }

        return@withContext null
    }

    /**
     * Get human-readable app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * Classify app into category for context-aware suggestions
     */
    private fun classifyApp(packageName: String): AppCategory {
        return when {
            // Browsers
            packageName.contains("chrome") ||
            packageName.contains("firefox") ||
            packageName.contains("browser") ||
            packageName.contains("opera") ||
            packageName.contains("edge") -> AppCategory.BROWSER

            // Messaging
            packageName.contains("whatsapp") ||
            packageName.contains("telegram") ||
            packageName.contains("signal") ||
            packageName.contains("messenger") ||
            packageName.contains("slack") ||
            packageName.contains("discord") -> AppCategory.MESSAGING

            // Email
            packageName.contains("gmail") ||
            packageName.contains("outlook") ||
            packageName.contains("mail") ||
            packageName.contains("email") -> AppCategory.EMAIL

            // Social Media
            packageName.contains("facebook") ||
            packageName.contains("twitter") ||
            packageName.contains("instagram") ||
            packageName.contains("tiktok") ||
            packageName.contains("linkedin") ||
            packageName.contains("reddit") -> AppCategory.SOCIAL

            // Notes/Documents
            packageName.contains("notes") ||
            packageName.contains("keep") ||
            packageName.contains("notion") ||
            packageName.contains("evernote") ||
            packageName.contains("docs") ||
            packageName.contains("sheets") -> AppCategory.PRODUCTIVITY

            // Maps/Navigation
            packageName.contains("maps") ||
            packageName.contains("waze") ||
            packageName.contains("navigation") -> AppCategory.MAPS

            // Shopping
            packageName.contains("amazon") ||
            packageName.contains("ebay") ||
            packageName.contains("shop") ||
            packageName.contains("store") -> AppCategory.SHOPPING

            // Media
            packageName.contains("youtube") ||
            packageName.contains("spotify") ||
            packageName.contains("netflix") ||
            packageName.contains("music") ||
            packageName.contains("video") -> AppCategory.MEDIA

            else -> AppCategory.OTHER
        }
    }

    /**
     * Generate smart suggestions based on current context
     *
     * @param appContext Current app context
     * @param screenText Text visible on screen (if available)
     * @return List of contextual suggestions
     */
    @Suppress("UNUSED_PARAMETER")
    fun generateSmartSuggestions(
        appContext: AppContext?,
        screenText: String? = null
    ): List<SmartSuggestion> {
        if (appContext == null) {
            return getDefaultSuggestions()
        }

        return when (appContext.category) {
            AppCategory.BROWSER -> listOf(
                SmartSuggestion("Summarize page", "summarize_page", "summary"),
                SmartSuggestion("Translate", "translate_page", "translate"),
                SmartSuggestion("Read aloud", "read_aloud", "volume_up"),
                SmartSuggestion("Save for later", "save_page", "bookmark")
            )

            AppCategory.MESSAGING -> listOf(
                SmartSuggestion("Reply", "reply_message", "reply"),
                SmartSuggestion("Translate message", "translate_message", "translate"),
                SmartSuggestion("Voice message", "voice_message", "mic"),
                SmartSuggestion("Quick response", "quick_reply", "chat")
            )

            AppCategory.EMAIL -> listOf(
                SmartSuggestion("Compose reply", "compose_reply", "reply"),
                SmartSuggestion("Summarize email", "summarize_email", "summary"),
                SmartSuggestion("Schedule send", "schedule_send", "schedule"),
                SmartSuggestion("Add to calendar", "add_calendar", "event")
            )

            AppCategory.SOCIAL -> listOf(
                SmartSuggestion("Caption this", "generate_caption", "edit"),
                SmartSuggestion("Translate post", "translate_post", "translate"),
                SmartSuggestion("Reply", "reply_post", "reply"),
                SmartSuggestion("Share", "share_post", "share")
            )

            AppCategory.PRODUCTIVITY -> listOf(
                SmartSuggestion("Summarize", "summarize_doc", "summary"),
                SmartSuggestion("Proofread", "proofread", "spellcheck"),
                SmartSuggestion("Translate", "translate_doc", "translate"),
                SmartSuggestion("Continue writing", "continue_writing", "edit")
            )

            AppCategory.MAPS -> listOf(
                SmartSuggestion("Directions", "get_directions", "directions"),
                SmartSuggestion("Traffic update", "traffic_update", "traffic"),
                SmartSuggestion("Share location", "share_location", "share"),
                SmartSuggestion("Save place", "save_place", "bookmark")
            )

            AppCategory.SHOPPING -> listOf(
                SmartSuggestion("Compare prices", "compare_prices", "compare"),
                SmartSuggestion("Read reviews", "read_reviews", "star"),
                SmartSuggestion("Find similar", "find_similar", "search"),
                SmartSuggestion("Add to list", "add_to_list", "checklist")
            )

            AppCategory.MEDIA -> listOf(
                SmartSuggestion("What's this song?", "identify_song", "music_note"),
                SmartSuggestion("Lyrics", "get_lyrics", "lyrics"),
                SmartSuggestion("Similar content", "similar_content", "recommend"),
                SmartSuggestion("Share", "share_media", "share")
            )

            AppCategory.OTHER -> getDefaultSuggestions()
        }
    }

    /**
     * Default suggestions when no context available
     */
    private fun getDefaultSuggestions(): List<SmartSuggestion> {
        return listOf(
            SmartSuggestion("Search", "search", "search"),
            SmartSuggestion("Translate", "translate", "translate"),
            SmartSuggestion("Reminder", "set_reminder", "alarm"),
            SmartSuggestion("Note", "create_note", "note")
        )
    }

    /**
     * Update screen text from accessibility service
     */
    fun updateScreenText(text: String?) {
        _screenText.value = text
    }

    /**
     * Check if usage stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return false
        }

        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000

        val stats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            beginTime,
            endTime
        )

        return stats != null && stats.isNotEmpty()
    }
}

/**
 * App context data model
 */
data class AppContext(
    val packageName: String,
    val appName: String,
    val category: AppCategory
)

/**
 * App category classification
 */
enum class AppCategory {
    BROWSER,
    MESSAGING,
    EMAIL,
    SOCIAL,
    PRODUCTIVITY,
    MAPS,
    SHOPPING,
    MEDIA,
    OTHER
}

/**
 * Smart suggestion with icon
 */
data class SmartSuggestion(
    val label: String,
    val action: String,
    val icon: String
)
