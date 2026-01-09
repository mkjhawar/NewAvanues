/**
 * StaticCommandRegistry.kt - Predefined static voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Registry of predefined voice commands that work system-wide,
 * independent of the current screen or app context.
 */
package com.augmentalis.voiceoscoreng.common

import com.augmentalis.voiceoscoreng.common.CommandActionType

/**
 * Registry of predefined static voice commands.
 *
 * These commands are always available regardless of screen context.
 * They provide system-level voice control functionality.
 *
 * Categories:
 * - Navigation: back, home, recent apps
 * - Media: play, pause, volume
 * - System: settings, notifications
 * - VoiceOS: mute, wake, dictation
 */
object StaticCommandRegistry {

    // ═══════════════════════════════════════════════════════════════════
    // Navigation Commands
    // ═══════════════════════════════════════════════════════════════════

    val navigationCommands = listOf(
        StaticCommand(
            phrases = listOf("go back", "navigate back", "back", "previous screen"),
            actionType = CommandActionType.BACK,
            category = CommandCategory.NAVIGATION,
            description = "Navigate to previous screen"
        ),
        StaticCommand(
            phrases = listOf("go home", "home", "navigate home", "open home"),
            actionType = CommandActionType.HOME,
            category = CommandCategory.NAVIGATION,
            description = "Go to home screen"
        ),
        StaticCommand(
            phrases = listOf("show recent apps", "recent apps", "open recents", "app switcher"),
            actionType = CommandActionType.RECENT_APPS,
            category = CommandCategory.NAVIGATION,
            description = "Show recent apps"
        ),
        StaticCommand(
            phrases = listOf("open app drawer", "app drawer", "all apps"),
            actionType = CommandActionType.APP_DRAWER,
            category = CommandCategory.NAVIGATION,
            description = "Open app drawer"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Media Commands
    // ═══════════════════════════════════════════════════════════════════

    val mediaCommands = listOf(
        StaticCommand(
            phrases = listOf("play music", "play", "resume"),
            actionType = CommandActionType.MEDIA_PLAY,
            category = CommandCategory.MEDIA,
            description = "Play/resume media"
        ),
        StaticCommand(
            phrases = listOf("pause music", "pause", "stop music"),
            actionType = CommandActionType.MEDIA_PAUSE,
            category = CommandCategory.MEDIA,
            description = "Pause media"
        ),
        StaticCommand(
            phrases = listOf("next song", "next track", "skip"),
            actionType = CommandActionType.MEDIA_NEXT,
            category = CommandCategory.MEDIA,
            description = "Next track"
        ),
        StaticCommand(
            phrases = listOf("previous song", "previous track", "go back"),
            actionType = CommandActionType.MEDIA_PREVIOUS,
            category = CommandCategory.MEDIA,
            description = "Previous track"
        ),
        StaticCommand(
            phrases = listOf("increase volume", "volume up", "louder"),
            actionType = CommandActionType.VOLUME_UP,
            category = CommandCategory.MEDIA,
            description = "Increase volume"
        ),
        StaticCommand(
            phrases = listOf("decrease volume", "volume down", "lower volume", "quieter"),
            actionType = CommandActionType.VOLUME_DOWN,
            category = CommandCategory.MEDIA,
            description = "Decrease volume"
        ),
        StaticCommand(
            phrases = listOf("mute volume", "mute", "silence"),
            actionType = CommandActionType.VOLUME_MUTE,
            category = CommandCategory.MEDIA,
            description = "Mute audio"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // System Commands
    // ═══════════════════════════════════════════════════════════════════

    val systemCommands = listOf(
        StaticCommand(
            phrases = listOf("open settings", "settings", "show settings", "device settings"),
            actionType = CommandActionType.OPEN_SETTINGS,
            category = CommandCategory.SYSTEM,
            description = "Open system settings"
        ),
        StaticCommand(
            phrases = listOf("show notifications", "notifications", "notification panel"),
            actionType = CommandActionType.NOTIFICATIONS,
            category = CommandCategory.SYSTEM,
            description = "Show notifications"
        ),
        StaticCommand(
            phrases = listOf("clear notifications", "dismiss notifications"),
            actionType = CommandActionType.CLEAR_NOTIFICATIONS,
            category = CommandCategory.SYSTEM,
            description = "Clear all notifications"
        ),
        StaticCommand(
            phrases = listOf("take screenshot", "screenshot", "capture screen"),
            actionType = CommandActionType.SCREENSHOT,
            category = CommandCategory.SYSTEM,
            description = "Take a screenshot"
        ),
        StaticCommand(
            phrases = listOf("turn on flashlight", "flashlight on", "torch on"),
            actionType = CommandActionType.FLASHLIGHT_ON,
            category = CommandCategory.SYSTEM,
            description = "Turn on flashlight"
        ),
        StaticCommand(
            phrases = listOf("turn off flashlight", "flashlight off", "torch off"),
            actionType = CommandActionType.FLASHLIGHT_OFF,
            category = CommandCategory.SYSTEM,
            description = "Turn off flashlight"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // VoiceOS Control Commands
    // ═══════════════════════════════════════════════════════════════════

    val voiceOSCommands = listOf(
        StaticCommand(
            phrases = listOf("mute voice", "stop listening", "voice off"),
            actionType = CommandActionType.VOICE_MUTE,
            category = CommandCategory.VOICE_CONTROL,
            description = "Mute voice recognition"
        ),
        StaticCommand(
            phrases = listOf("wake up voice", "start listening", "voice on"),
            actionType = CommandActionType.VOICE_WAKE,
            category = CommandCategory.VOICE_CONTROL,
            description = "Wake voice recognition"
        ),
        StaticCommand(
            phrases = listOf("start dictation", "dictation", "type mode"),
            actionType = CommandActionType.DICTATION_START,
            category = CommandCategory.VOICE_CONTROL,
            description = "Start dictation mode"
        ),
        StaticCommand(
            phrases = listOf("stop dictation", "end dictation", "command mode"),
            actionType = CommandActionType.DICTATION_STOP,
            category = CommandCategory.VOICE_CONTROL,
            description = "Stop dictation mode"
        ),
        StaticCommand(
            phrases = listOf("show voice commands", "what can I say", "help"),
            actionType = CommandActionType.SHOW_COMMANDS,
            category = CommandCategory.VOICE_CONTROL,
            description = "Show available commands"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // App Launcher Commands
    // ═══════════════════════════════════════════════════════════════════

    val appCommands = listOf(
        StaticCommand(
            phrases = listOf("open browser", "browser", "open web browser"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open web browser",
            metadata = mapOf("app_type" to "browser")
        ),
        StaticCommand(
            phrases = listOf("open camera", "camera", "take photo"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open camera",
            metadata = mapOf("app_type" to "camera")
        ),
        StaticCommand(
            phrases = listOf("open gallery", "gallery", "photos"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open photo gallery",
            metadata = mapOf("app_type" to "gallery")
        ),
        StaticCommand(
            phrases = listOf("open calculator", "calculator"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open calculator",
            metadata = mapOf("app_type" to "calculator")
        ),
        StaticCommand(
            phrases = listOf("open calendar", "calendar"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open calendar",
            metadata = mapOf("app_type" to "calendar")
        ),
        StaticCommand(
            phrases = listOf("open phone", "phone", "dialer"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open phone dialer",
            metadata = mapOf("app_type" to "phone")
        ),
        StaticCommand(
            phrases = listOf("open messages", "messages", "sms"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open messages app",
            metadata = mapOf("app_type" to "messages")
        ),
        StaticCommand(
            phrases = listOf("open contacts", "contacts"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open contacts",
            metadata = mapOf("app_type" to "contacts")
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Accessibility Commands
    // ═══════════════════════════════════════════════════════════════════

    val accessibilityCommands = listOf(
        StaticCommand(
            phrases = listOf("scroll down", "page down"),
            actionType = CommandActionType.SCROLL_DOWN,
            category = CommandCategory.ACCESSIBILITY,
            description = "Scroll down"
        ),
        StaticCommand(
            phrases = listOf("scroll up", "page up"),
            actionType = CommandActionType.SCROLL_UP,
            category = CommandCategory.ACCESSIBILITY,
            description = "Scroll up"
        ),
        StaticCommand(
            phrases = listOf("scroll left"),
            actionType = CommandActionType.SCROLL_LEFT,
            category = CommandCategory.ACCESSIBILITY,
            description = "Scroll left"
        ),
        StaticCommand(
            phrases = listOf("scroll right"),
            actionType = CommandActionType.SCROLL_RIGHT,
            category = CommandCategory.ACCESSIBILITY,
            description = "Scroll right"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Registry Access Methods
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all static commands
     */
    fun all(): List<StaticCommand> =
        navigationCommands +
        mediaCommands +
        systemCommands +
        voiceOSCommands +
        appCommands +
        accessibilityCommands

    /**
     * Get all phrase strings (for speech engine vocabulary)
     */
    fun allPhrases(): List<String> = all().flatMap { it.phrases }

    /**
     * Get commands by category
     */
    fun byCategory(category: CommandCategory): List<StaticCommand> =
        all().filter { it.category == category }

    /**
     * Find command matching phrase
     */
    fun findByPhrase(phrase: String): StaticCommand? {
        val normalized = phrase.lowercase().trim()
        return all().find { cmd ->
            cmd.phrases.any { it.lowercase() == normalized }
        }
    }

    /**
     * Get command count
     */
    val commandCount: Int get() = all().size

    /**
     * Get phrase count
     */
    val phraseCount: Int get() = allPhrases().size

    // ═══════════════════════════════════════════════════════════════════
    // NLU/LLM Integration - QuantizedCommand Export
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all static commands as QuantizedCommand objects for NLU/LLM.
     *
     * Each phrase variant becomes a separate QuantizedCommand with:
     * - targetVuid = null (system commands, no element target)
     * - confidence = 1.0 (always available)
     * - metadata includes category, description, source
     *
     * @return List of QuantizedCommand for all static commands
     */
    fun allAsQuantized(): List<QuantizedCommand> {
        return all().flatMap { it.toQuantizedCommands() }
    }

    /**
     * Get static commands by category as QuantizedCommand.
     *
     * @param category Command category
     * @return List of QuantizedCommand for category
     */
    fun byCategoryAsQuantized(category: CommandCategory): List<QuantizedCommand> {
        return byCategory(category).flatMap { it.toQuantizedCommands() }
    }

    /**
     * Export all commands in AVU CMD format for NLU/LLM.
     *
     * Format: CMD:uuid:trigger:action:element_uuid:confidence
     * Static commands have empty element_uuid (system commands).
     *
     * @return Multi-line string with all commands in CMD format
     */
    fun toAvuFormat(): String {
        return allAsQuantized().joinToString("\n") { it.toCmdLine() }
    }

    /**
     * Get a concise NLU schema describing available commands.
     *
     * Returns a structured format suitable for LLM prompts:
     * - Category grouping
     * - Primary phrase + action type
     * - Description for context
     */
    fun toNluSchema(): String {
        return buildString {
            appendLine("# Static Voice Commands")
            appendLine()

            CommandCategory.entries.forEach { category ->
                val commands = byCategory(category)
                if (commands.isNotEmpty()) {
                    appendLine("## ${category.name}")
                    commands.forEach { cmd ->
                        appendLine("- ${cmd.primaryPhrase}: ${cmd.actionType.name} - ${cmd.description}")
                        if (cmd.phrases.size > 1) {
                            appendLine("  Aliases: ${cmd.phrases.drop(1).joinToString(", ")}")
                        }
                    }
                    appendLine()
                }
            }
        }
    }
}

/**
 * Represents a static/predefined voice command
 */
data class StaticCommand(
    /**
     * Alternative phrases that trigger this command
     */
    val phrases: List<String>,

    /**
     * Action type to execute
     */
    val actionType: CommandActionType,

    /**
     * Command category for organization
     */
    val category: CommandCategory,

    /**
     * Human-readable description
     */
    val description: String,

    /**
     * Additional metadata for command execution
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Primary phrase (first in list)
     */
    val primaryPhrase: String get() = phrases.first()

    /**
     * Convert to QuantizedCommand objects for NLU/LLM.
     *
     * Creates one QuantizedCommand per phrase variant.
     * Static commands have:
     * - targetVuid = null (system command, no element target)
     * - confidence = 1.0 (always available)
     * - uuid = static__{category}__{normalized_phrase}
     *
     * @return List of QuantizedCommand for each phrase
     */
    fun toQuantizedCommands(): List<QuantizedCommand> {
        return phrases.map { phrase ->
            val normalizedPhrase = phrase.lowercase().replace(" ", "_")
            val uuid = "static__${category.name.lowercase()}__$normalizedPhrase"

            QuantizedCommand(
                uuid = uuid,
                phrase = phrase,
                actionType = actionType,
                targetVuid = null, // Static commands have no target element
                confidence = 1.0f, // Static commands are always available
                metadata = metadata + mapOf(
                    "source" to "static",
                    "category" to category.name,
                    "description" to description,
                    "primary_phrase" to primaryPhrase
                )
            )
        }
    }

    /**
     * Convert primary phrase to a single QuantizedCommand.
     *
     * @return QuantizedCommand for primary phrase only
     */
    fun toQuantizedCommand(): QuantizedCommand {
        val normalizedPhrase = primaryPhrase.lowercase().replace(" ", "_")
        val uuid = "static__${category.name.lowercase()}__$normalizedPhrase"

        return QuantizedCommand(
            uuid = uuid,
            phrase = primaryPhrase,
            actionType = actionType,
            targetVuid = null,
            confidence = 1.0f,
            metadata = metadata + mapOf(
                "source" to "static",
                "category" to category.name,
                "description" to description,
                "aliases" to phrases.drop(1).joinToString("|")
            )
        )
    }
}

/**
 * Command categories for organization and filtering
 */
enum class CommandCategory {
    NAVIGATION,
    MEDIA,
    SYSTEM,
    VOICE_CONTROL,
    APP_LAUNCH,
    ACCESSIBILITY,
    CUSTOM
}
