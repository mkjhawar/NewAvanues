# Browser UI Components

## Overview

This package contains the main browser UI components for WebAvanue.

## Components

### BrowserScreen
Main browser screen that integrates all browser components with voice-first UI design.

**Features:**
- Tab bar with all open tabs (dark 3D theme)
- Address bar with navigation controls
- WebView container for web content
- **Voice-first Bottom Command Bar** with contextual menus
- Voice commands help panel
- Text command input
- Desktop/Advanced mode toggle
- Navigation to bookmarks/downloads/history/settings

**Usage:**
```kotlin
BrowserScreen(
    tabViewModel = tabViewModel,
    settingsViewModel = settingsViewModel,  // Required for home URL, etc.
    historyViewModel = historyViewModel,    // Required for history tracking
    onNavigateToBookmarks = { /* navigate */ },
    onNavigateToDownloads = { /* navigate */ },
    onNavigateToHistory = { /* navigate */ },
    onNavigateToSettings = { /* navigate */ }
)
```

**Recent Updates (2025-11-19):**
- Added `settingsViewModel` parameter for configurable home URL
- Added `historyViewModel` parameter for automatic history tracking
- Added `AddPageDialog` for creating new tabs with URL prompt
- Fixed home button to use settings.homePage instead of about:blank
- Automatic history entry creation when pages load

---

### BottomCommandBar
Voice-first floating command bar with contextual menu system.

**Features:**
- Floating pill-shaped bar at bottom center
- 8 main buttons: Back, Home, Up, Down, Person, Mic, Text, Menu
- Contextual menu levels (click icon to see related options)
- Voice button with listening animation
- Text command input
- Dark 3D theme colors

**Menu Levels (Legacy Hierarchical Structure):**
- **MAIN** (Step 1): Back, Home, Add Page, Navigation Commands, Web Commands, Prev/Next Tab
- **NAVIGATION_COMMANDS** (Step 1.2): Back, Home, Scroll Commands, Cursor Commands, Zoom Commands
- **WEB_COMMANDS** (Step 1.3): Back, Home, Prev/Next Page, Reload, Desktop Mode, Touch, Favorite, Clear Cache
- **SCROLL** (Step 1.2.1): Back, Home, Scroll Up/Down/Left/Right, Page Up/Down, Freeze
- **CURSOR** (Step 1.2.2): Back, Home, Select/Click, Double Click
- **ZOOM** (Step 1.2.3): Back, Home, Zoom In, Zoom Out, Zoom Level
- **ZOOM_LEVEL** (Step 1.2.3.1): Back, Home, Zoom Level 1-5
- **TOUCH** (Step 1.3.1): Back, Home, Start/Stop Drag, Rotate, Pinch Open/Close
- **MENU**: Back, Home, Bookmarks, Downloads, History, Settings

**Recent Updates (2025-11-19):**
- Restructured to match legacy hierarchical navigation
- Added NAVIGATION_COMMANDS and WEB_COMMANDS as main submenu categories
- Added CURSOR and TOUCH command levels
- All submenus now include Back and Home buttons for consistency

**Usage:**
```kotlin
BottomCommandBar(
    onBack = { webViewController.goBack() },
    onForward = { webViewController.goForward() },
    onHome = { tabViewModel.navigateToUrl("about:blank") },
    onRefresh = { webViewController.reload() },
    onVoice = { isListening = !isListening },
    onTextCommand = { showTextCommand = !showTextCommand },
    onSettings = onNavigateToSettings,
    isListening = isListening,
    isDesktopMode = isDesktopMode,
    modifier = Modifier.align(Alignment.BottomCenter)
)
```

---

### VoiceCommandsPanel
Help panel showing available voice commands.

**Commands:**
- "go back" - Navigate back
- "go forward" - Navigate forward
- "go home" - Go to home page
- "refresh" - Reload page
- "scroll up/down" - Scroll page
- "new tab" - Open new tab
- "close tab" - Close current tab
- "go to [url]" - Navigate to URL

---

### TextCommandInput
Text input field for typing commands instead of speaking.

**Features:**
- Slide-up animation
- Command parsing (back, forward, go to, etc.)
- Dark 3D theme styling

---

### AddressBar
Browser address bar with URL input and navigation controls.

**Features:**
- URL input field with auto-complete
- Back/Forward navigation buttons
- Refresh button
- Bookmark/Downloads/History/Settings menu buttons
- Go button to navigate

**Usage:**
```kotlin
AddressBar(
    url = "https://example.com",
    canGoBack = true,
    canGoForward = false,
    onUrlChange = { newUrl -> /* handle URL change */ },
    onGo = { /* navigate to URL */ },
    onBack = { /* go back */ },
    onForward = { /* go forward */ },
    onRefresh = { /* refresh page */ },
    onBookmarkClick = { /* open bookmarks */ },
    onDownloadClick = { /* open downloads */ },
    onHistoryClick = { /* open history */ },
    onSettingsClick = { /* open settings */ }
)
```

---

### WebViewContainer
Platform-specific WebView wrapper (expect/actual pattern).

**Platforms:**
- **Android:** WebView (AndroidView)
- **iOS:** WKWebView (UIViewRepresentable) - TODO
- **Desktop:** JavaFX WebView or CEF - TODO
- **Web:** iframe - TODO

**Features:**
- URL navigation and history
- JavaScript support
- Cookie management
- Progress tracking
- Custom user agent (desktop mode)
- Title and favicon tracking

**Usage:**
```kotlin
WebViewContainer(
    url = "https://example.com",
    onUrlChange = { newUrl -> /* update address bar */ },
    onLoadingChange = { isLoading -> /* show/hide progress */ },
    onTitleChange = { title -> /* update tab title */ },
    onProgressChange = { progress -> /* update progress bar */ },
    canGoBack = { canGoBack -> /* enable/disable back button */ },
    canGoForward = { canGoForward -> /* enable/disable forward button */ },
    modifier = Modifier.fillMaxSize()
)
```

---

### WebViewController
Controller for programmatic WebView operations.

**Operations:**
- `goBack()` - Navigate back in history
- `goForward()` - Navigate forward in history
- `reload()` - Reload current page
- `stopLoading()` - Stop loading current page
- `loadUrl(url)` - Load specific URL
- `evaluateJavaScript(script, callback)` - Execute JavaScript
- `clearCache()` - Clear browser cache
- `clearCookies()` - Clear cookies
- `clearHistory()` - Clear browsing history
- `setUserAgent(userAgent)` - Set custom user agent
- `setJavaScriptEnabled(enabled)` - Enable/disable JavaScript
- `setCookiesEnabled(enabled)` - Enable/disable cookies
- `setDesktopMode(enabled)` - Request desktop version of sites

**Usage:**
```kotlin
val webViewController = remember { WebViewController() }

// Go back
webViewController.goBack()

// Execute JavaScript
webViewController.evaluateJavaScript("document.title") { result ->
    println("Page title: $result")
}

// Enable desktop mode
webViewController.setDesktopMode(true)
```

---

## Integration with TabViewModel

The browser UI components are designed to work seamlessly with TabViewModel:

```kotlin
@Composable
fun BrowserScreen(tabViewModel: TabViewModel) {
    val activeTab by tabViewModel.activeTab.collectAsState()
    var urlInput by remember { mutableStateOf("") }

    // Update URL input when active tab changes
    LaunchedEffect(activeTab) {
        urlInput = activeTab?.url ?: ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabBar(viewModel = tabViewModel)

        AddressBar(
            url = urlInput,
            canGoBack = activeTab?.canGoBack ?: false,
            canGoForward = activeTab?.canGoForward ?: false,
            onUrlChange = { urlInput = it },
            onGo = {
                if (urlInput.isNotBlank()) {
                    tabViewModel.navigateToUrl(urlInput)
                }
            },
            onBack = { /* WebView back */ },
            onForward = { /* WebView forward */ },
            onRefresh = { /* WebView reload */ }
        )

        activeTab?.let { tab ->
            WebViewContainer(
                url = tab.url,
                onUrlChange = { newUrl ->
                    tabViewModel.updateTabUrl(tab.id, newUrl)
                },
                onLoadingChange = { isLoading ->
                    tabViewModel.updateTabLoading(tab.id, isLoading)
                },
                onTitleChange = { title ->
                    tabViewModel.updateTabTitle(tab.id, title)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

---

## Platform-Specific Implementation

### Android
Uses Android `WebView` with `AndroidView` composable.

**Files:**
- `WebViewContainer.android.kt` - Android implementation
- Uses `WebViewClient` for page navigation
- Uses `WebChromeClient` for JavaScript and progress

**Settings:**
- JavaScript enabled
- DOM storage enabled
- Zoom controls
- Wide viewport
- Cache mode: default

**User Agent:**
- Mobile (default): Android WebView user agent
- Desktop mode: Chrome on Windows user agent

### iOS (TODO)
Will use `WKWebView` with `UIViewRepresentable`.

### Desktop (TODO)
Will use JavaFX WebView or Chromium Embedded Framework (CEF).

### Web (TODO)
Will use iframe with restrictions.

---

## Testing

### Unit Tests
Test WebViewController operations:
```kotlin
@Test
fun testGoBack() {
    val controller = WebViewController()
    // Set up mock WebView
    controller.goBack()
    // Verify WebView.goBack() was called
}
```

### UI Tests
Test WebViewContainer composable:
```kotlin
@Test
fun testWebViewLoadsUrl() {
    composeTestRule.setContent {
        WebViewContainer(
            url = "https://example.com",
            onUrlChange = {},
            onLoadingChange = {},
            onTitleChange = {},
            onProgressChange = {},
            canGoBack = {},
            canGoForward = {},
            modifier = Modifier.fillMaxSize()
        )
    }

    // Verify URL was loaded
}
```

---

## Known Limitations

1. **Android Only:** Currently only Android implementation exists
2. **No Download Handling:** Downloads are not handled yet
3. **No File Upload:** File upload is not implemented
4. **No Picture-in-Picture:** PiP mode not supported
5. **No WebRTC:** WebRTC features not enabled

---

## Future Enhancements

1. **Download Manager Integration:** Handle file downloads
2. **File Upload Support:** Allow file selection
3. **WebRTC Support:** Enable video/audio calls
4. **Picture-in-Picture:** PiP mode for videos
5. **Custom Context Menu:** Long-press menu for links/images
6. **AdBlock Integration:** Built-in ad blocking
7. **Reader Mode:** Simplified reading view
8. **Offline Mode:** Cache pages for offline viewing

---

## Resources

- [Android WebView Guide](https://developer.android.com/develop/ui/views/layout/webapps/webview)
- [Jetpack Compose Interop](https://developer.android.com/jetpack/compose/migrate/interoperability-apis/views-in-compose)
- [WKWebView (iOS)](https://developer.apple.com/documentation/webkit/wkwebview)
