package com.augmentalis.ava.core.domain.resolution

import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import com.augmentalis.ava.core.domain.model.AppPlatform
import com.augmentalis.ava.core.domain.model.KnownApp

/**
 * Registry of all app capabilities AVA can resolve.
 *
 * Defines how to detect apps for each capability (email, SMS, music, etc.)
 * and provides a list of known popular apps for recommendations.
 *
 * Part of Intelligent Resolution System (Chapter 71)
 *
 * Author: Manoj Jhawar
 */
object CapabilityRegistry {

    val capabilities: Map<String, CapabilityDefinition> = mapOf(
        // ==================== Communication ====================
        "email" to CapabilityDefinition(
            id = "email",
            displayName = "Email",
            category = CapabilityCategory.COMMUNICATION,
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_SENDTO, dataScheme = "mailto"),
                IntentSpec(Intent.ACTION_SEND, mimeType = "message/rfc822")
            ),
            knownApps = listOf(
                KnownApp("com.google.android.gm", "Gmail", AppPlatform.ANDROID,
                    "https://play.google.com/store/apps/details?id=com.google.android.gm"),
                KnownApp("com.microsoft.office.outlook", "Outlook", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.microsoft.office.outlook"),
                KnownApp("com.yahoo.mobile.client.android.mail", "Yahoo Mail", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.yahoo.mobile.client.android.mail"),
                KnownApp("com.samsung.android.email.provider", "Samsung Email", AppPlatform.ANDROID),
                KnownApp("me.proton.android.mail", "Proton Mail", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=ch.protonmail.android")
            )
        ),

        "sms" to CapabilityDefinition(
            id = "sms",
            displayName = "Text Messages",
            category = CapabilityCategory.COMMUNICATION,
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_SENDTO, dataScheme = "sms"),
                IntentSpec(Intent.ACTION_SENDTO, dataScheme = "smsto")
            ),
            knownApps = listOf(
                KnownApp("com.google.android.apps.messaging", "Messages", AppPlatform.ANDROID,
                    "https://play.google.com/store/apps/details?id=com.google.android.apps.messaging"),
                KnownApp("com.samsung.android.messaging", "Samsung Messages", AppPlatform.ANDROID),
                KnownApp("com.whatsapp", "WhatsApp", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.whatsapp"),
                KnownApp("org.telegram.messenger", "Telegram", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=org.telegram.messenger"),
                KnownApp("com.facebook.orca", "Messenger", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.facebook.orca")
            )
        ),

        "phone" to CapabilityDefinition(
            id = "phone",
            displayName = "Phone Calls",
            category = CapabilityCategory.COMMUNICATION,
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_DIAL, dataScheme = "tel")
            ),
            knownApps = listOf(
                KnownApp("com.google.android.dialer", "Phone", AppPlatform.ANDROID),
                KnownApp("com.samsung.android.dialer", "Samsung Phone", AppPlatform.ANDROID)
            )
        ),

        // ==================== Media ====================
        "music" to CapabilityDefinition(
            id = "music",
            displayName = "Music Player",
            category = CapabilityCategory.MEDIA,
            androidIntents = listOf(
                IntentSpec("android.media.action.MEDIA_PLAY_FROM_SEARCH"),
                IntentSpec(Intent.ACTION_VIEW, mimeType = "audio/*")
            ),
            knownApps = listOf(
                KnownApp("com.spotify.music", "Spotify", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.spotify.music"),
                KnownApp("com.google.android.apps.youtube.music", "YouTube Music", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.google.android.apps.youtube.music"),
                KnownApp("com.amazon.mp3", "Amazon Music", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.amazon.mp3"),
                KnownApp("com.apple.android.music", "Apple Music", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.apple.android.music"),
                KnownApp("deezer.android.app", "Deezer", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=deezer.android.app")
            )
        ),

        "video" to CapabilityDefinition(
            id = "video",
            displayName = "Video Player",
            category = CapabilityCategory.MEDIA,
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_VIEW, mimeType = "video/*"),
                IntentSpec(Intent.ACTION_SEARCH, packageName = "com.google.android.youtube")
            ),
            knownApps = listOf(
                KnownApp("com.google.android.youtube", "YouTube", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.google.android.youtube"),
                KnownApp("com.netflix.mediaclient", "Netflix", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.netflix.mediaclient"),
                KnownApp("com.amazon.avod.thirdpartyclient", "Prime Video", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.amazon.avod.thirdpartyclient"),
                KnownApp("com.disney.disneyplus", "Disney+", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.disney.disneyplus"),
                KnownApp("com.mxtech.videoplayer.ad", "MX Player", AppPlatform.ANDROID,
                    "https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad")
            )
        ),

        // ==================== Navigation ====================
        "maps" to CapabilityDefinition(
            id = "maps",
            displayName = "Maps & Navigation",
            category = CapabilityCategory.NAVIGATION,
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_VIEW, dataScheme = "geo"),
                IntentSpec(Intent.ACTION_VIEW, dataScheme = "google.navigation")
            ),
            knownApps = listOf(
                KnownApp("com.google.android.apps.maps", "Google Maps", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.google.android.apps.maps"),
                KnownApp("com.waze", "Waze", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.waze"),
                KnownApp("com.here.app.maps", "HERE WeGo", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.here.app.maps"),
                KnownApp("com.sygic.aura", "Sygic", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.sygic.aura")
            )
        ),

        // ==================== Productivity ====================
        "calendar" to CapabilityDefinition(
            id = "calendar",
            displayName = "Calendar",
            category = CapabilityCategory.PRODUCTIVITY,
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_INSERT, mimeType = "vnd.android.cursor.item/event"),
                IntentSpec(Intent.ACTION_VIEW, dataScheme = "content",
                    dataAuthority = "com.android.calendar")
            ),
            knownApps = listOf(
                KnownApp("com.google.android.calendar", "Google Calendar", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.google.android.calendar"),
                KnownApp("com.microsoft.office.outlook", "Outlook", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.microsoft.office.outlook"),
                KnownApp("com.samsung.android.calendar", "Samsung Calendar", AppPlatform.ANDROID),
                KnownApp("org.tasks", "Tasks.org", AppPlatform.ANDROID,
                    "https://play.google.com/store/apps/details?id=org.tasks")
            )
        ),

        "notes" to CapabilityDefinition(
            id = "notes",
            displayName = "Notes",
            category = CapabilityCategory.PRODUCTIVITY,
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_SEND, mimeType = "text/plain")
            ),
            knownApps = listOf(
                KnownApp("com.google.android.keep", "Google Keep", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.google.android.keep"),
                KnownApp("com.microsoft.office.onenote", "OneNote", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.microsoft.office.onenote"),
                KnownApp("com.evernote", "Evernote", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.evernote"),
                KnownApp("com.samsung.android.app.notes", "Samsung Notes", AppPlatform.ANDROID),
                KnownApp("md.obsidian", "Obsidian", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=md.obsidian")
            )
        ),

        // ==================== Browser ====================
        "browser" to CapabilityDefinition(
            id = "browser",
            displayName = "Web Browser",
            category = CapabilityCategory.BROWSER,
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_VIEW, dataScheme = "http"),
                IntentSpec(Intent.ACTION_VIEW, dataScheme = "https")
            ),
            knownApps = listOf(
                KnownApp("com.android.chrome", "Chrome", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.android.chrome"),
                KnownApp("org.mozilla.firefox", "Firefox", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=org.mozilla.firefox"),
                KnownApp("com.brave.browser", "Brave", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.brave.browser"),
                KnownApp("com.microsoft.emmx", "Edge", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.microsoft.emmx"),
                KnownApp("com.opera.browser", "Opera", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.opera.browser"),
                KnownApp("com.sec.android.app.sbrowser", "Samsung Internet", AppPlatform.ANDROID)
            )
        ),

        // ==================== Services ====================
        "rideshare" to CapabilityDefinition(
            id = "rideshare",
            displayName = "Ride Sharing",
            category = CapabilityCategory.SERVICES,
            androidIntents = listOf(),
            knownApps = listOf(
                KnownApp("com.ubercab", "Uber", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.ubercab"),
                KnownApp("com.lyft.android", "Lyft", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.lyft.android"),
                KnownApp("com.ola.cabs", "Ola", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.olacabs.customer")
            )
        ),

        "food_delivery" to CapabilityDefinition(
            id = "food_delivery",
            displayName = "Food Delivery",
            category = CapabilityCategory.SERVICES,
            androidIntents = listOf(),
            knownApps = listOf(
                KnownApp("com.ubercab.eats", "Uber Eats", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.ubercab.eats"),
                KnownApp("com.dd.doordash", "DoorDash", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.dd.doordash"),
                KnownApp("com.grubhub.android", "Grubhub", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.grubhub.android"),
                KnownApp("com.application.zomato", "Zomato", AppPlatform.BOTH,
                    "https://play.google.com/store/apps/details?id=com.application.zomato")
            )
        )
    )

    /**
     * Get capability by ID.
     */
    fun get(capability: String): CapabilityDefinition? = capabilities[capability]

    /**
     * Get all capabilities in a category.
     */
    fun getByCategory(category: CapabilityCategory): List<CapabilityDefinition> =
        capabilities.values.filter { it.category == category }

    /**
     * Get all capability IDs.
     */
    fun getAllIds(): Set<String> = capabilities.keys
}

/**
 * Definition of a capability (what apps can do).
 */
data class CapabilityDefinition(
    val id: String,
    val displayName: String,
    val category: CapabilityCategory,
    val androidIntents: List<IntentSpec>,
    val knownApps: List<KnownApp>
)

/**
 * Categories for organizing capabilities.
 */
enum class CapabilityCategory {
    COMMUNICATION,
    MEDIA,
    NAVIGATION,
    PRODUCTIVITY,
    BROWSER,
    SERVICES
}

/**
 * Specification for an Android intent used to find apps.
 */
data class IntentSpec(
    val action: String,
    val dataScheme: String? = null,
    val dataAuthority: String? = null,
    val mimeType: String? = null,
    val packageName: String? = null
)
