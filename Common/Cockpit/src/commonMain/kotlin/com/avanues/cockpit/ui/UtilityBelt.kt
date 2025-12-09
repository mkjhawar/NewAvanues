package com.avanues.cockpit.ui

/**
 * Utility Belt - Corner mini-panels
 *
 * Quick access widgets for:
 * - Music player
 * - Timers/stopwatch
 * - Battery status
 * - Notifications
 *
 * Voice Commands:
 * - "Show utilities"
 * - "Hide utilities"
 * - "Play music" / "Pause music"
 * - "Set timer 5 minutes"
 * - "Battery level?"
 */
data class UtilityBelt(
    val visible: Boolean = false,  // Hidden by default
    val position: UtilityPosition = UtilityPosition.BOTTOM_CORNERS,
    val widgets: List<UtilityWidget> = defaultWidgets()
)

data class UtilityWidget(
    val id: String,
    val type: WidgetType,
    val visible: Boolean = true,
    val voiceCommands: List<String>
)

enum class UtilityPosition {
    BOTTOM_CORNERS,
    TOP_CORNERS,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT
}

enum class WidgetType {
    MUSIC_PLAYER,
    TIMER,
    BATTERY,
    NOTIFICATIONS,
    WEATHER,
    CALENDAR
}

fun defaultWidgets() = listOf(
    UtilityWidget(
        id = "music",
        type = WidgetType.MUSIC_PLAYER,
        voiceCommands = listOf("Play music", "Pause music", "Next song", "Previous song")
    ),
    UtilityWidget(
        id = "timer",
        type = WidgetType.TIMER,
        voiceCommands = listOf("Set timer", "Start timer", "Stop timer", "Timer status")
    ),
    UtilityWidget(
        id = "battery",
        type = WidgetType.BATTERY,
        voiceCommands = listOf("Battery level", "Battery status")
    ),
    UtilityWidget(
        id = "notifications",
        type = WidgetType.NOTIFICATIONS,
        voiceCommands = listOf("Show notifications", "Clear notifications")
    )
)
