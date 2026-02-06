package com.augmentalis.voiceoscore.dsl.plugin

/**
 * Cross-platform permission model for AVU DSL plugins.
 * Determines what system capabilities a plugin can access.
 *
 * Mirrors the Android-only PluginPermission enum in androidMain but lives in commonMain
 * for KMP compatibility. The plugin loader validates that declared codes are covered
 * by the plugin's granted permissions.
 */
enum class PluginPermission(val displayName: String, val description: String) {
    GESTURES("Gestures", "Perform touch/gesture actions on screen"),
    APPS("Apps", "Launch and manage applications"),
    NOTIFICATIONS("Notifications", "Read and manage notifications"),
    NETWORK("Network", "Make network requests"),
    STORAGE("Storage", "Read/write files"),
    LOCATION("Location", "Access device location"),
    SENSORS("Sensors", "Access device sensors"),
    CAMERA("Camera", "Access camera"),
    MICROPHONE("Microphone", "Access microphone"),
    CONTACTS("Contacts", "Access contacts"),
    CALENDAR("Calendar", "Access calendar"),
    SMS("SMS", "Send/read SMS"),
    PHONE("Phone", "Make phone calls"),
    ACCESSIBILITY("Accessibility", "Interact with UI elements via accessibility"),
    SYSTEM("System", "Access system-level functions");

    companion object {
        fun fromString(name: String): PluginPermission? =
            entries.find { it.name.equals(name, ignoreCase = true) }

        fun parseList(names: List<String>): Set<PluginPermission> =
            names.mapNotNull { fromString(it) }.toSet()
    }
}
