/**
 * OverlayModels.kt - Data models for the overlay badge system
 *
 * Extracted from the app-layer OverlayStateManager into KMP commonMain
 * so iOS can share the same overlay data types.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore

/**
 * Data for displaying numbered badges on screen elements.
 *
 * Each item represents one numbered badge positioned over an interactive element.
 * Coordinates are in absolute screen pixels.
 */
data class NumberOverlayItem(
    val number: Int,
    val label: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val avid: String
)

/**
 * Instruction bar visibility mode:
 * - ON: Always show instruction bar
 * - OFF: Never show instruction bar
 * - AUTO: Show briefly then fade out (default)
 */
enum class InstructionBarMode {
    ON, OFF, AUTO
}

/**
 * Badge color themes for numbered badges.
 *
 * Each theme defines background and text colors as ARGB Long values,
 * suitable for direct use with Compose Color(Long).
 */
enum class BadgeTheme(val backgroundColor: Long, val textColor: Long) {
    GREEN(0xFF4CAF50, 0xFFFFFFFF),
    BLUE(0xFF2196F3, 0xFFFFFFFF),
    PURPLE(0xFF9C27B0, 0xFFFFFFFF),
    ORANGE(0xFFFF9800, 0xFF000000),
    RED(0xFFF44336, 0xFFFFFFFF),
    TEAL(0xFF009688, 0xFFFFFFFF),
    PINK(0xFFE91E63, 0xFFFFFFFF)
}

/**
 * Per-app numbers mode preference.
 *
 * Controls whether numbered badges are shown automatically when the user
 * enters a recognized app (e.g., Gmail).
 */
enum class AppNumbersPreference {
    ASK, ALWAYS, AUTO, NEVER
}

/**
 * Callback interface for persisting per-app number preferences.
 *
 * Platform implementations store preferences in platform-specific storage
 * (e.g., Android DataStore, iOS UserDefaults).
 */
interface PreferenceCallback {
    fun saveAppNumbersPreference(packageName: String, preference: AppNumbersPreference)
}

/**
 * Package names of apps that benefit from list-item numbering (Tier 3).
 *
 * These apps have scrollable lists (email, messaging, social) where
 * numbered badges on list items are especially useful for voice selection.
 */
val TARGET_APPS = setOf(
    "com.google.android.gm",
    "com.microsoft.office.outlook",
    "com.samsung.android.email.provider",
    "com.google.android.apps.messaging",
    "com.whatsapp",
    "org.telegram.messenger",
    "com.discord",
    "com.Slack",
    "com.twitter.android",
    "com.instagram.android",
    "com.facebook.katana",
    "com.linkedin.android",
    "com.todoist",
    "com.google.android.keep",
    "com.microsoft.todos",
    "com.amazon.mShop.android.shopping"
)
