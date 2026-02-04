package com.avanues.cockpit.ui

/**
 * Control Rail - Floating toolbar for system functions
 *
 * Location: Top or side (floating), voice-controlled
 *
 * Voice Commands:
 * - "Show controls" / "Hide controls"
 * - "Go home"
 * - "Switch workspace"
 * - "Change layout"
 * - "Settings"
 */
data class ControlRail(
    val visible: Boolean = true,
    val position: RailPosition = RailPosition.TOP_CENTER,
    val buttons: List<RailButton> = defaultButtons()
)

data class RailButton(
    val id: String,
    val label: String,
    val icon: String,
    val voiceCommand: String,
    val action: RailAction
)

enum class RailPosition {
    TOP_CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    LEFT_SIDE,
    RIGHT_SIDE
}

enum class RailAction {
    HOME,
    WORKSPACE_SELECTOR,
    LAYOUT_SELECTOR,
    VOICE_SETTINGS,
    SYSTEM_SETTINGS
}

fun defaultButtons() = listOf(
    RailButton(
        id = "home",
        label = "Home",
        icon = "ic_home",
        voiceCommand = "Go home",
        action = RailAction.HOME
    ),
    RailButton(
        id = "workspace",
        label = "Workspace",
        icon = "ic_workspace",
        voiceCommand = "Switch workspace",
        action = RailAction.WORKSPACE_SELECTOR
    ),
    RailButton(
        id = "layout",
        label = "Layout",
        icon = "ic_layout",
        voiceCommand = "Change layout",
        action = RailAction.LAYOUT_SELECTOR
    ),
    RailButton(
        id = "voice",
        label = "Voice",
        icon = "ic_mic",
        voiceCommand = "Voice settings",
        action = RailAction.VOICE_SETTINGS
    ),
    RailButton(
        id = "settings",
        label = "Settings",
        icon = "ic_settings",
        voiceCommand = "Settings",
        action = RailAction.SYSTEM_SETTINGS
    )
)
