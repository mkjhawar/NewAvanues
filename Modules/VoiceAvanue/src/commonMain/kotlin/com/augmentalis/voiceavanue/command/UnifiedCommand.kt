/**
 * UnifiedCommand.kt - Unified command model for VoiceAvanue
 *
 * Common command types used across VoiceOSCore, WebAvanue, and VoiceCursor.
 * This is the canonical source of truth for command definitions.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.command

import kotlinx.serialization.Serializable

/**
 * Base interface for all commands in VoiceAvanue
 */
interface ICommand {
    val id: String
    val phrase: String
    val category: CommandCategory
    val actionType: CommandActionType
    val priority: Int
}

/**
 * Command categories - unified across all subsystems
 */
@Serializable
enum class CommandCategory(val displayName: String) {
    // Voice OS Categories
    NAVIGATION("Navigation"),
    CLICK("Click"),
    SCROLL("Scroll"),
    INPUT("Input"),
    SELECTION("Selection"),
    SYSTEM("System"),
    APP("App"),
    DEVICE("Device"),
    ACCESSIBILITY("Accessibility"),
    GESTURE("Gesture"),
    HELP("Help"),

    // Browser Categories
    BROWSER_NAVIGATION("Browser Navigation"),
    BROWSER_TABS("Tabs"),
    BROWSER_BOOKMARKS("Bookmarks"),
    BROWSER_HISTORY("History"),
    BROWSER_DOWNLOADS("Downloads"),
    BROWSER_SETTINGS("Browser Settings"),
    BROWSER_PAGE("Page"),
    BROWSER_FORM("Form"),

    // Cursor Categories
    CURSOR_MOVEMENT("Cursor Movement"),
    CURSOR_CLICK("Cursor Click"),
    CURSOR_DWELL("Dwell Click"),
    CURSOR_DRAG("Drag"),
    CURSOR_SETTINGS("Cursor Settings"),

    // Custom/Learned Categories
    CUSTOM("Custom"),
    LEARNED("Learned"),

    // Utility
    UNKNOWN("Unknown")
}

/**
 * Action types for command execution
 */
@Serializable
enum class CommandActionType {
    // UI Actions
    CLICK,
    DOUBLE_CLICK,
    LONG_PRESS,
    SCROLL_UP,
    SCROLL_DOWN,
    SCROLL_LEFT,
    SCROLL_RIGHT,
    SWIPE,
    DRAG,
    PINCH,
    ZOOM,

    // Navigation
    NAVIGATE,
    GO_BACK,
    GO_FORWARD,
    GO_HOME,
    OPEN_URL,
    NEW_TAB,
    CLOSE_TAB,
    SWITCH_TAB,

    // Input
    TYPE_TEXT,
    DICTATE,
    PASTE,
    COPY,
    CUT,
    SELECT_ALL,
    CLEAR,

    // System
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE,
    SCREENSHOT,
    SETTINGS,
    NOTIFICATIONS,
    RECENT_APPS,

    // App
    LAUNCH_APP,
    CLOSE_APP,
    SWITCH_APP,

    // Cursor
    MOVE_CURSOR,
    LOCK_CURSOR,
    UNLOCK_CURSOR,
    SHOW_CURSOR,
    HIDE_CURSOR,
    TOGGLE_DWELL,

    // Browser
    REFRESH,
    STOP,
    FIND_IN_PAGE,
    ADD_BOOKMARK,
    SHOW_BOOKMARKS,
    SHOW_HISTORY,
    SHOW_DOWNLOADS,
    TOGGLE_DESKTOP_MODE,
    READ_MODE,

    // Custom
    CUSTOM_ACTION,

    // No-op
    NONE
}

/**
 * Universal command model
 */
@Serializable
data class UnifiedCommand(
    override val id: String,
    override val phrase: String,
    override val category: CommandCategory,
    override val actionType: CommandActionType,
    override val priority: Int = 0,
    val aliases: List<String> = emptyList(),
    val parameters: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val sourceModule: SourceModule = SourceModule.UNKNOWN,
    val contextRestrictions: List<ContextRestriction> = emptyList()
) : ICommand

/**
 * Source module for command origin tracking
 */
@Serializable
enum class SourceModule {
    VOICE_OS_CORE,
    WEB_AVANUE,
    VOICE_CURSOR,
    GAZE_TRACKER,
    LEARNED,
    CUSTOM,
    UNKNOWN
}

/**
 * Context restrictions for when commands are valid
 */
@Serializable
sealed class ContextRestriction {
    @Serializable
    data class AppRestriction(val packageName: String) : ContextRestriction()

    @Serializable
    data class ScreenRestriction(val screenType: String) : ContextRestriction()

    @Serializable
    data class BrowserRestriction(val urlPattern: String) : ContextRestriction()

    @Serializable
    object CursorActiveRestriction : ContextRestriction()

    @Serializable
    object BrowserActiveRestriction : ContextRestriction()
}

/**
 * Command execution result
 */
@Serializable
sealed class CommandResult {
    @Serializable
    data class Success(
        val commandId: String,
        val message: String? = null,
        val data: Map<String, String> = emptyMap()
    ) : CommandResult()

    @Serializable
    data class Failure(
        val commandId: String,
        val error: String,
        val errorCode: ErrorCode = ErrorCode.UNKNOWN
    ) : CommandResult()

    @Serializable
    data class Pending(
        val commandId: String,
        val status: String
    ) : CommandResult()

    @Serializable
    object NotFound : CommandResult()
}

/**
 * Error codes for command failures
 */
@Serializable
enum class ErrorCode {
    UNKNOWN,
    NOT_FOUND,
    INVALID_PARAMETERS,
    TIMEOUT,
    PERMISSION_DENIED,
    CONTEXT_INVALID,
    EXECUTION_FAILED,
    NOT_SUPPORTED,
    CANCELLED
}
