# WebAvanue Feature Reference

**Version:** 1.0.0
**Last Updated:** 2025-12-01
**Status:** Complete
**Total Features:** 100+

---

## Overview

This document provides a complete reference of all WebAvanue browser features for developers. Each feature includes its implementation location, API, and testing requirements.

---

## Feature Summary

| Category | Count | Module |
|----------|-------|--------|
| Navigation | 9 | `universal/presentation/ui/browser` |
| Tab Management | 8 | `universal/presentation/ui/browser` |
| Favorites/Bookmarks | 10 | `universal/presentation/ui/bookmark` |
| Downloads | 8 | `universal/presentation/ui/download` |
| History | 7 | `universal/presentation/ui/history` |
| Settings | 15 | `universal/presentation/ui/settings` |
| Command Bar | 30+ | `universal/presentation/ui/browser/commandbar` |
| WebGL/OpenGL | 5 | Android WebView / Platform specific |
| WebXR | 8 | `android/webavanue/xr` |
| Voice/Text | 10 | `universal/commands` |
| UI/Display | 8 | `universal/presentation/ui` |

---

## 1. Navigation Features

### Implementation
- **Package:** `com.augmentalis.Avanues.web.universal.presentation.ui.browser`
- **Main File:** `BrowserScreen.kt`
- **State:** `BrowserScreenState.kt`

### Features

| Feature | Implementation | Manager Method |
|---------|---------------|----------------|
| Back | `navigateBack()` | `WebViewManager.goBack()` |
| Forward | `navigateForward()` | `WebViewManager.goForward()` |
| Refresh | `refresh()` | `WebViewManager.reload()` |
| Home | `navigateHome()` | Uses `BrowserSettingsManager.homePageUrl` |
| URL Entry | `AddressBar` composable | `WebViewManager.loadUrl()` |
| Auto-prefix | `normalizeUrl()` | In `BrowserViewModel` |
| Loading Indicator | `LinearProgressIndicator` | Bound to `state.loadingProgress` |
| History Enable | `canGoBack`, `canGoForward` | WebView state callbacks |

### Code References
```kotlin
// BrowserViewModel.kt
fun navigateBack() {
    webViewManager.goBack()
}

fun navigateForward() {
    webViewManager.goForward()
}
```

---

## 2. Tab Management Features

### Implementation
- **Package:** `com.augmentalis.Avanues.web.universal.presentation.ui.browser`
- **Main File:** `TabBar.kt`
- **State:** `TabState.kt`

### Features

| Feature | Implementation | Data Layer |
|---------|---------------|------------|
| New Tab | `createNewTab()` | `TabRepository.insertTab()` |
| Switch Tab | `switchToTab(id)` | `TabRepository.updateCurrentTab()` |
| Close Tab | `closeTab(id)` | `TabRepository.deleteTab()` |
| Tab Title | `Tab.title` property | From `WebView.title` callback |
| Active Indicator | `selectedTabId` state | UI state in `BrowserScreenState` |
| Empty State | `EmptyTabState` composable | Shown when `tabs.isEmpty()` |

### Database Schema
```sql
CREATE TABLE tabs (
    id TEXT PRIMARY KEY,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    last_accessed INTEGER NOT NULL,
    is_current INTEGER DEFAULT 0
);
```

---

## 3. Favorites/Bookmarks Features

### Implementation
- **Package:** `com.augmentalis.Avanues.web.universal.presentation.ui.bookmark`
- **Files:** `BookmarkListScreen.kt`, `AddBookmarkDialog.kt`
- **Repository:** `BookmarkRepository.kt`

### Features

| Feature | Implementation | API |
|---------|---------------|-----|
| Add Favorite | `AddBookmarkDialog` | `BookmarkRepository.insertBookmark()` |
| Edit Favorite | `EditBookmarkDialog` | `BookmarkRepository.updateBookmark()` |
| Delete Favorite | `deleteBookmark()` | `BookmarkRepository.deleteBookmark()` |
| Navigate | `onBookmarkClick()` | Triggers `loadUrl()` |
| Star Indicator | `isCurrentPageFavorited` | Query bookmark by URL |
| Search | `searchQuery` state | `BookmarkRepository.searchBookmarks()` |
| Folders | `BookmarkFolder` model | `FolderRepository` |

### Domain Models
```kotlin
data class Bookmark(
    val id: String,
    val url: String,
    val title: String,
    val description: String?,
    val favicon: String?,
    val folderId: String?,
    val createdAt: Long,
    val lastVisited: Long?
)

data class BookmarkFolder(
    val id: String,
    val name: String,
    val parentId: String?,
    val createdAt: Long
)
```

---

## 4. Downloads Features

### Implementation
- **Package:** `com.augmentalis.Avanues.web.universal.presentation.ui.download`
- **Files:** `DownloadListScreen.kt`, `DownloadItem.kt`
- **Repository:** `DownloadRepository.kt`

### Features

| Feature | Implementation | Status |
|---------|---------------|--------|
| Detection | `DownloadListener` on WebView | Auto-triggered |
| Progress | `LinearProgressIndicator` | Bound to `download.progress` |
| Filter | `FilterChips` composable | All/Active/Complete/Failed |
| Cancel | `cancelDownload()` | `DownloadManager.remove()` |
| Retry | `retryDownload()` | `DownloadManager.enqueue()` |
| Delete | `deleteDownload()` | `DownloadRepository.delete()` |

### Download States
```kotlin
enum class DownloadStatus {
    PENDING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

---

## 5. History Features

### Implementation
- **Package:** `com.augmentalis.Avanues.web.universal.presentation.ui.history`
- **Files:** `HistoryListScreen.kt`
- **Repository:** `HistoryRepository.kt`

### Features

| Feature | Implementation | Grouping |
|---------|---------------|----------|
| Recording | Auto on page load | `HistoryRepository.insertHistory()` |
| Grouping | `groupByDate()` | Today, Yesterday, This Week, Older |
| Search | `searchQuery` state | `HistoryRepository.searchHistory()` |
| Navigate | `onHistoryClick()` | Triggers `loadUrl()` |
| Delete | `deleteHistoryEntry()` | `HistoryRepository.deleteById()` |
| Clear All | `clearHistory()` | `HistoryRepository.clearAll()` |

---

## 6. Settings Features

### Implementation
- **Package:** `com.augmentalis.Avanues.web.universal.presentation.ui.settings`
- **Files:** `SettingsScreen.kt`, `SettingsSection.kt`
- **Manager:** `BrowserSettingsManager.kt`

### Categories

#### General Settings
| Setting | Key | Type | Default |
|---------|-----|------|---------|
| Search Engine | `searchEngine` | String | "google" |
| Homepage | `homePageUrl` | String | "https://www.google.com" |

#### Appearance Settings
| Setting | Key | Type | Default |
|---------|-----|------|---------|
| Theme | `theme` | Enum | SYSTEM |
| Theme Options | - | - | LIGHT, DARK, SYSTEM, AUTO |

#### Privacy Settings
| Setting | Key | Type | Default |
|---------|-----|------|---------|
| JavaScript | `javascriptEnabled` | Boolean | true |
| Cookies | `cookiesEnabled` | Boolean | true |
| Block Popups | `blockPopups` | Boolean | true |
| Block Ads | `blockAds` | Boolean | false |
| Block Trackers | `blockTrackers` | Boolean | false |

#### Advanced Settings
| Setting | Key | Type | Default |
|---------|-----|------|---------|
| Desktop Mode | `desktopMode` | Boolean | false |
| Media Auto-play | `mediaAutoplay` | Enum | ON_WIFI |
| Voice Commands | `voiceEnabled` | Boolean | true |

---

## 7. Command Bar Features

### Implementation
- **Package:** `com.augmentalis.Avanues.web.universal.presentation.ui.browser.commandbar`
- **Main File:** `CommandBar.kt`
- **Submenus:** `CommandBarSubmenu.kt`

### Command Categories

#### Main Level
| Command | Icon | Action |
|---------|------|--------|
| Back | `arrow_back` | `navigateBack()` |
| Home | `home` | `navigateHome()` |
| Add Page | `add` | `showNewTabDialog()` |
| Navigation | `explore` | `showNavSubmenu()` |
| Web | `language` | `showWebSubmenu()` |
| Menu | `menu` | `showMenuSubmenu()` |

#### Scroll Commands
| Command | Action | JavaScript |
|---------|--------|------------|
| Up | Scroll up | `window.scrollBy(0, -window.innerHeight)` |
| Down | Scroll down | `window.scrollBy(0, window.innerHeight)` |
| Left | Scroll left | `window.scrollBy(-100, 0)` |
| Right | Scroll right | `window.scrollBy(100, 0)` |
| Top | Scroll to top | `window.scrollTo(0, 0)` |
| Bottom | Scroll to bottom | `window.scrollTo(0, document.body.scrollHeight)` |
| Freeze | Toggle freeze | Sets `isScrollFrozen` state |

#### Zoom Commands
| Level | Zoom % | Scale Factor |
|-------|--------|--------------|
| 1 | 50% | 0.5f |
| 2 | 75% | 0.75f |
| 3 | 100% | 1.0f |
| 4 | 125% | 1.25f |
| 5 | 150% | 1.5f |

---

## 8. WebGL/OpenGL Features

### Implementation
- **Platform:** Android WebView (Chromium-based)
- **Configuration:** WebView settings

### Supported APIs

| API | Android Support | Configuration |
|-----|-----------------|---------------|
| WebGL 1.0 | Full | `setDomStorageEnabled(true)` |
| WebGL 2.0 | Full | Hardware acceleration enabled |
| Canvas 2D | Full | Default enabled |
| OffscreenCanvas | Partial | Chrome 69+ |

### WebView Configuration
```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    setRenderPriority(WebSettings.RenderPriority.HIGH)
}
webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
```

### Test Sites
| Site | URL | Purpose |
|------|-----|---------|
| WebGL Report | https://webglreport.com | Capability check |
| Get WebGL | https://get.webgl.org | Basic test |
| Aquarium | https://webglsamples.org/aquarium/aquarium.html | Benchmark |
| Three.js | https://threejs.org/examples | Library test |
| Babylon.js | https://playground.babylonjs.com | Engine test |
| Shadertoy | https://www.shadertoy.com | Shader test |

---

## 9. WebXR Features

### Implementation
- **Package:** `com.augmentalis.Avanues.web.android.xr`
- **Files:** `WebXRManager.kt`, `XRSessionHandler.kt`
- **Settings:** `WebXRSettingsScreen.kt`

### Features

| Feature | Implementation | Requirement |
|---------|---------------|-------------|
| Enable WebXR | `WebXRManager.enable()` | Android 7.0+ |
| AR Sessions | `XRSessionHandler.startAR()` | ARCore support |
| VR Sessions | `XRSessionHandler.startVR()` | VR capable device |
| Performance Mode | `XRPerformanceManager` | GPU capability |
| Auto-Pause | `XRSessionMonitor` | Battery optimization |
| FPS Indicator | `XRDebugOverlay` | Debug build |

### Configuration
```kotlin
data class WebXRConfig(
    val enabled: Boolean = false,
    val arEnabled: Boolean = true,
    val vrEnabled: Boolean = true,
    val performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    val autoPauseTimeout: Long = 60000L,
    val showFps: Boolean = false,
    val wifiOnly: Boolean = false
)

enum class PerformanceMode {
    HIGH_QUALITY,  // 90 FPS target
    BALANCED,      // 60 FPS target
    BATTERY_SAVER  // 30 FPS target
}
```

### Test Sites
| Site | URL | Type |
|------|-----|------|
| WebXR Samples | https://immersive-web.github.io/webxr-samples/ | Official |
| XR Viewer | https://xr.foo/ | Capability |
| A-Frame | https://aframe.io/examples/ | VR Framework |
| Hello WebXR | https://mixedreality.mozilla.org/hello-webxr/ | Intro |

---

## 10. Voice/Text Commands

### Implementation
- **Package:** `com.augmentalis.Avanues.web.universal.commands`
- **Parser:** `CommandParser.kt`
- **Handler:** `CommandHandler.kt`

### Command Mapping

| Text Command | Voice Command | Handler Method |
|--------------|---------------|----------------|
| back | "go back" | `navigateBack()` |
| forward | "go forward" | `navigateForward()` |
| refresh | "refresh", "reload" | `refresh()` |
| home | "go home" | `navigateHome()` |
| new tab | "new tab" | `createNewTab()` |
| bookmarks | "show bookmarks" | `showBookmarks()` |
| downloads | "show downloads" | `showDownloads()` |
| history | "show history" | `showHistory()` |
| settings | "settings" | `showSettings()` |
| go to [url] | "go to [url]" | `navigateTo(url)` |

---

## Testing Requirements

### Unit Tests
| Component | Test File | Coverage Target |
|-----------|-----------|-----------------|
| Repositories | `*RepositoryTest.kt` | 90% |
| ViewModels | `*ViewModelTest.kt` | 85% |
| Managers | `*ManagerTest.kt` | 90% |
| Commands | `CommandParserTest.kt` | 95% |

### Integration Tests
| Feature | Test Scope |
|---------|------------|
| Navigation | WebView integration |
| Tabs | Database persistence |
| Downloads | Android DownloadManager |
| WebXR | ARCore/VR capability |

### UI Tests
| Screen | Test File |
|--------|-----------|
| Browser | `BrowserScreenTest.kt` |
| Settings | `SettingsScreenTest.kt` |
| Bookmarks | `BookmarkScreenTest.kt` |
| Downloads | `DownloadScreenTest.kt` |

---

## Related Documentation

- [User Manual](../../USER-MANUAL.md) - End-user documentation
- [Feature Testing Guide](../../FEATURE-TESTING-GUIDE.md) - QA testing procedures
- [WebXR Integration Guide](../../developer/WEBXR-INTEGRATION-GUIDE.md) - WebXR details
- [Repository API](../api/WebAvanue-RepositoryAPI-001.00.00-2511161200.md) - Data layer API

---

**Author:** Manoj Jhawar <manoj@ideahq.net>
**License:** Proprietary - Augmentalis Inc.
**Version:** 1.0.0
**Last Updated:** 2025-12-01
