package com.augmentalis.webavanue

import com.augmentalis.webavanue.WebViewController
import com.augmentalis.webavanue.TabViewModel
import com.augmentalis.webavanue.BrowserSettings

/**
 * Execute a text command from voice input or text field
 *
 * Supported commands:
 * - Navigation: back, forward, refresh, home
 * - Tab management: new tab
 * - Navigation screens: bookmarks, downloads, history, settings
 * - URL navigation: "go to [url]"
 *
 * @param command The command string to execute
 * @param webViewController WebView controller for browser actions
 * @param tabViewModel ViewModel for tab operations
 * @param settings Browser settings for default values
 * @param onBookmarks Navigate to bookmarks callback
 * @param onDownloads Navigate to downloads callback
 * @param onHistory Navigate to history callback
 * @param onSettings Navigate to settings callback
 */
fun executeTextCommand(
    command: String,
    webViewController: WebViewController,
    tabViewModel: TabViewModel,
    settings: BrowserSettings?,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    val normalizedCommand = command.lowercase().trim()

    when {
        normalizedCommand == "back" || normalizedCommand == "go back" -> {
            webViewController.goBack()
        }
        normalizedCommand == "forward" || normalizedCommand == "go forward" -> {
            webViewController.goForward()
        }
        normalizedCommand == "refresh" || normalizedCommand == "reload" -> {
            webViewController.reload()
        }
        normalizedCommand == "home" || normalizedCommand == "go home" -> {
            val homeUrl = settings?.homePage ?: "https://www.google.com"
            tabViewModel.navigateToUrl(homeUrl)
        }
        normalizedCommand == "new tab" -> {
            tabViewModel.createTab(url = "", title = "New Tab", isDesktopMode = settings?.useDesktopMode == true)
        }
        normalizedCommand == "bookmarks" -> onBookmarks()
        normalizedCommand == "downloads" -> onDownloads()
        normalizedCommand == "history" -> onHistory()
        normalizedCommand == "settings" -> onSettings()
        normalizedCommand.startsWith("go to ") -> {
            val url = normalizedCommand.removePrefix("go to ").trim()
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            tabViewModel.navigateToUrl(formattedUrl)
        }
    }
}

/**
 * Format URL with protocol if missing
 */
fun formatUrlWithProtocol(url: String): String {
    return if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
    } else {
        url
    }
}
