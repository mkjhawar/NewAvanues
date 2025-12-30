package com.augmentalis.webavanue.feature.voice

import kotlinx.coroutines.flow.StateFlow

/**
 * VoiceCommandService - Handles voice recognition and command parsing
 *
 * Voice commands supported:
 * - Navigation: "go back", "go forward", "go home", "refresh"
 * - Scrolling: "scroll up", "scroll down", "scroll to top", "scroll to bottom"
 * - Tabs: "new tab", "close tab", "next tab", "previous tab"
 * - Zoom: "zoom in", "zoom out", "reset zoom"
 * - Mode: "desktop mode", "mobile mode"
 * - Navigation: "go to [url]", "search [query]"
 * - Bookmarks: "bookmark this", "open bookmarks"
 * - Other: "downloads", "history", "settings"
 */

/**
 * Recognized voice command types
 */
sealed class VoiceCommand {
    // Navigation
    object GoBack : VoiceCommand()
    object GoForward : VoiceCommand()
    object GoHome : VoiceCommand()
    object Refresh : VoiceCommand()

    // Scrolling
    object ScrollUp : VoiceCommand()
    object ScrollDown : VoiceCommand()
    object ScrollToTop : VoiceCommand()
    object ScrollToBottom : VoiceCommand()
    object FreezeScroll : VoiceCommand()

    // Tabs
    object NewTab : VoiceCommand()
    object CloseTab : VoiceCommand()
    object NextTab : VoiceCommand()
    object PreviousTab : VoiceCommand()
    object ShowTabs : VoiceCommand()      // Spatial tab switcher (3D view)
    object ShowFavorites : VoiceCommand() // Spatial favorites shelf (3D view)

    // Zoom
    object ZoomIn : VoiceCommand()
    object ZoomOut : VoiceCommand()
    object ResetZoom : VoiceCommand()

    // Mode
    object DesktopMode : VoiceCommand()
    object MobileMode : VoiceCommand()

    // Navigation with parameter
    data class GoToUrl(val url: String) : VoiceCommand()
    data class Search(val query: String) : VoiceCommand()

    // Bookmarks
    object BookmarkThis : VoiceCommand()
    object OpenBookmarks : VoiceCommand()

    // Screens
    object OpenDownloads : VoiceCommand()
    object OpenHistory : VoiceCommand()
    object OpenSettings : VoiceCommand()
    object ShowHelp : VoiceCommand()        // Voice commands help dialog

    // Unknown/Not recognized
    data class Unknown(val text: String) : VoiceCommand()
}

/**
 * Voice recognition state
 */
sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Processing : VoiceState()
    data class Result(val command: VoiceCommand, val rawText: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

/**
 * Voice command parser - converts speech text to VoiceCommand
 */
object VoiceCommandParser {

    fun parse(text: String): VoiceCommand {
        val normalized = text.lowercase().trim()

        return when {
            // Navigation
            normalized == "go back" || normalized == "back" -> VoiceCommand.GoBack
            normalized == "go forward" || normalized == "forward" -> VoiceCommand.GoForward
            normalized == "go home" || normalized == "home" -> VoiceCommand.GoHome
            normalized == "refresh" || normalized == "reload" -> VoiceCommand.Refresh

            // Scrolling
            normalized == "scroll up" || normalized == "up" -> VoiceCommand.ScrollUp
            normalized == "scroll down" || normalized == "down" -> VoiceCommand.ScrollDown
            normalized == "scroll to top" || normalized == "top" || normalized == "go to top" -> VoiceCommand.ScrollToTop
            normalized == "scroll to bottom" || normalized == "bottom" || normalized == "go to bottom" -> VoiceCommand.ScrollToBottom
            normalized == "freeze" || normalized == "freeze scroll" || normalized == "lock" -> VoiceCommand.FreezeScroll

            // Tabs
            normalized == "new tab" || normalized == "open tab" || normalized == "add tab" -> VoiceCommand.NewTab
            normalized == "close tab" || normalized == "close" -> VoiceCommand.CloseTab
            normalized == "next tab" || normalized == "next" -> VoiceCommand.NextTab
            normalized == "previous tab" || normalized == "previous" || normalized == "prev tab" -> VoiceCommand.PreviousTab
            normalized == "show tabs" || normalized == "tabs" || normalized == "view tabs" || normalized == "all tabs" -> VoiceCommand.ShowTabs
            normalized == "show favorites" || normalized == "favorites" || normalized == "view favorites" || normalized == "my favorites" -> VoiceCommand.ShowFavorites

            // Zoom
            normalized == "zoom in" || normalized == "bigger" -> VoiceCommand.ZoomIn
            normalized == "zoom out" || normalized == "smaller" -> VoiceCommand.ZoomOut
            normalized == "reset zoom" || normalized == "normal zoom" -> VoiceCommand.ResetZoom

            // Mode
            normalized == "desktop mode" || normalized == "desktop" -> VoiceCommand.DesktopMode
            normalized == "mobile mode" || normalized == "mobile" -> VoiceCommand.MobileMode

            // Bookmarks
            normalized == "bookmark" || normalized == "bookmark this" || normalized == "add bookmark" || normalized == "favorite" -> VoiceCommand.BookmarkThis
            normalized == "bookmarks" || normalized == "open bookmarks" || normalized == "favorites" -> VoiceCommand.OpenBookmarks

            // Screens
            normalized == "downloads" || normalized == "open downloads" -> VoiceCommand.OpenDownloads
            normalized == "history" || normalized == "open history" -> VoiceCommand.OpenHistory
            normalized == "settings" || normalized == "open settings" -> VoiceCommand.OpenSettings
            normalized == "show help" || normalized == "help" || normalized == "commands" || normalized == "show commands" -> VoiceCommand.ShowHelp

            // URL navigation
            normalized.startsWith("go to ") -> {
                val url = normalized.removePrefix("go to ").trim()
                VoiceCommand.GoToUrl(url)
            }
            normalized.startsWith("open ") && !normalized.contains("tab") -> {
                val url = normalized.removePrefix("open ").trim()
                VoiceCommand.GoToUrl(url)
            }

            // Search
            normalized.startsWith("search ") || normalized.startsWith("search for ") -> {
                val query = normalized
                    .removePrefix("search for ")
                    .removePrefix("search ")
                    .trim()
                VoiceCommand.Search(query)
            }
            normalized.startsWith("google ") -> {
                val query = normalized.removePrefix("google ").trim()
                VoiceCommand.Search(query)
            }

            // Unknown
            else -> VoiceCommand.Unknown(text)
        }
    }
}

/**
 * Expect declaration for platform-specific voice service
 */
expect class PlatformVoiceService {
    val voiceState: StateFlow<VoiceState>
    val isAvailable: Boolean

    fun startListening()
    fun stopListening()
    fun destroy()
}
