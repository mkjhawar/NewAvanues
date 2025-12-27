# Implementation Plan: WebAvanue UI/UX Improvements v1.9.0

**Source Specification:** spec-webavanue-ui-ux-improvements-20251203.md v1.3
**Tasks Document:** tasks-webavanue-ui-ux-improvements-20251203.md
**Target Version:** WebAvanue v1.9.0
**Platform:** Android (Kotlin Multiplatform + Jetpack Compose)
**Execution Mode:** Swarm (Parallel)
**Date:** 2025-12-03
**Status:** Ready for Implementation

---

## Executive Summary

This implementation plan addresses 28 functional requirements across 45 implementation tasks, organized for parallel swarm execution. The plan includes:

- **Architecture decisions** for new features (headless mode, external APIs, tab groups, downloads)
- **State management patterns** for complex UI state synchronization
- **External API design** for third-party AR/XR app integration
- **Swarm coordination protocol** for parallel development
- **Quality gates** and testing strategy
- **Risk mitigation** for AOSP compatibility and performance

**Key Metrics:**
- **Tasks:** 45
- **Sequential Effort:** ~90 hours
- **Swarm Effort:** ~50 hours (45% time savings)
- **Test Coverage Target:** 90%+
- **Quality Gates:** 0 blockers, 0 warnings

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [State Management Strategy](#state-management-strategy)
3. [External API Design](#external-api-design)
4. [UI/UX Implementation](#ui-ux-implementation)
5. [Feature Implementation](#feature-implementation)
6. [Swarm Coordination](#swarm-coordination)
7. [Testing Strategy](#testing-strategy)
8. [Risk Mitigation](#risk-mitigation)
9. [Execution Timeline](#execution-timeline)
10. [Quality Gates](#quality-gates)

---

## Architecture Overview

### Current Architecture

```
MainAvanues (monorepo)
├── android/apps/webavanue/           # Android app entry point
├── common/webavanue/
│   ├── coredata/                     # Domain models, databases
│   │   └── src/commonMain/kotlin/
│   │       └── com/augmentalis/webavanue/domain/model/
│   │           ├── BrowserSettings.kt
│   │           ├── Tab.kt
│   │           ├── Favorite.kt
│   │           └── HistoryItem.kt
│   └── universal/                    # Shared UI (Compose Multiplatform)
│       ├── domain/                   # Domain layer
│       ├── data/                     # Data layer (repositories)
│       └── presentation/             # Presentation layer
│           ├── viewmodel/            # ViewModels
│           │   ├── TabViewModel.kt
│           │   ├── SettingsViewModel.kt
│           │   ├── FavoriteViewModel.kt
│           │   ├── HistoryViewModel.kt
│           │   ├── SecurityViewModel.kt
│           │   └── DownloadViewModel.kt
│           └── ui/                   # Composables
│               ├── browser/          # BrowserScreen, AddressBar, etc.
│               ├── tab/              # Tab management UI
│               ├── settings/         # Settings screens
│               ├── spatial/          # AR/spatial UI components
│               └── theme/            # OceanTheme (Material3)
└── docs/webavanue/                   # Documentation
```

### New Architecture Components

This implementation adds the following components:

```
common/webavanue/
├── coredata/
│   └── src/commonMain/kotlin/com/augmentalis/webavanue/domain/model/
│       ├── TabGroup.kt               # NEW: Tab grouping model
│       ├── Download.kt               # NEW: Download tracking
│       └── ViewMode.kt               # NEW: View mode enum (List/AR)
│
├── universal/
│   ├── presentation/
│   │   ├── viewmodel/
│   │   │   ├── FavoritesViewModel.kt        # NEW: Favorites + History
│   │   │   ├── DownloadManagerViewModel.kt  # NEW: Download management
│   │   │   └── NetworkMonitorViewModel.kt   # NEW: Connectivity tracking
│   │   │
│   │   └── ui/
│   │       ├── favorites/            # NEW: Favorites screen with dual views
│   │       │   ├── FavoritesScreen.kt
│   │       │   ├── FavoritesList.kt
│   │       │   ├── FavoritesARView.kt
│   │       │   ├── HistoryList.kt
│   │       │   └── HistoryARView.kt
│   │       │
│   │       ├── layout/               # NEW: Custom layouts
│   │       │   └── ArcLayout.kt      # Arc/carousel layout composable
│   │       │
│   │       ├── downloads/            # NEW: Downloads management
│   │       │   └── DownloadsScreen.kt
│   │       │
│   │       └── components/           # NEW: Shared components
│   │           ├── FavoriteThumbnailCard.kt
│   │           └── FeatureUnavailableDialog.kt
│   │
│   └── data/
│       ├── repository/
│       │   ├── TabGroupRepository.kt         # NEW: Tab group persistence
│       │   └── DownloadRepository.kt         # NEW: Download tracking
│       │
│       ├── database/
│       │   ├── TabGroupDao.kt                # NEW: Tab group database
│       │   └── DownloadDao.kt                # NEW: Download database
│       │
│       └── service/
│           ├── NetworkMonitor.kt             # NEW: Connectivity monitoring
│           ├── DownloadManager.kt            # NEW: Download handling
│           └── FeatureCompatibility.kt       # NEW: Feature detection
│
└── android/
    └── src/main/kotlin/com/augmentalis/Avanues/web/android/
        ├── receiver/                 # NEW: BroadcastReceivers for Intent API
        │   ├── HeadlessModeReceiver.kt
        │   ├── BrowserControlReceiver.kt
        │   └── IntentActions.kt
        │
        ├── api/                      # NEW: External API layer
        │   ├── JavaScriptExecutor.kt
        │   ├── PageLifecycleManager.kt
        │   ├── ScreenshotManager.kt
        │   ├── WebXRDetector.kt
        │   ├── ZoomController.kt
        │   ├── CookieController.kt
        │   └── FindController.kt
        │
        └── notification/             # NEW: Download notifications
            └── DownloadNotification.kt

# NEW: Developer SDK (separate module)
webavanue-sdk/
├── build.gradle.kts
└── src/main/kotlin/com/augmentalis/webavanue/sdk/
    ├── WebAvanueController.kt        # Main SDK class
    ├── callbacks/                    # Callback interfaces
    │   ├── NavigationCallback.kt
    │   ├── PageCallback.kt
    │   └── DownloadCallback.kt
    └── models/                       # Data models for SDK
        ├── BrowserAction.kt
        └── BrowserState.kt
```

### Architecture Principles

| Principle | Implementation |
|-----------|----------------|
| **SOLID** | Enforced on all new code |
| **Clean Architecture** | Domain → Data → Presentation layers |
| **Unidirectional Data Flow** | ViewModel → UI (Compose State) |
| **Repository Pattern** | Data access abstraction |
| **Dependency Injection** | Koin for DI |
| **Reactive Programming** | Kotlin Flows for data streams |
| **Material3 Design** | OceanTheme with glassmorphism |
| **Testability** | 90%+ coverage on critical paths |

---

## State Management Strategy

### Current State Management

WebAvanue uses **ViewModel + Compose State** pattern:

```kotlin
// Current pattern (TabViewModel example)
class TabViewModel : ViewModel() {
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _activeTab = MutableStateFlow<Tab?>(null)
    val activeTab: StateFlow<Tab?> = _activeTab.asStateFlow()

    fun selectTab(tab: Tab) {
        _activeTab.value = tab
    }
}

// UI consumption
@Composable
fun BrowserScreen(tabViewModel: TabViewModel) {
    val activeTab by tabViewModel.activeTab.collectAsState()
    val tabs by tabViewModel.tabs.collectAsState()
    // ...
}
```

### State Management Challenges

| Issue | Impact | Solution |
|-------|--------|----------|
| **Command bar toggle state desync** (FR-010) | UI doesn't reflect ViewModel state | Add explicit `commandBarVisible` state |
| **Auto-hide timer not canceling** (FR-011) | Command bar hides during interaction | Proper timer cancellation on user events |
| **Favorites view mode persistence** (FR-009) | User preference lost on navigation | Add `favoritesViewMode` to settings |
| **Headless mode state** (FR-005) | Needs to persist across sessions | Add `headlessModeActive` to settings |
| **Network status reactivity** (FR-008) | Need real-time connectivity updates | Use Flow-based NetworkMonitor |
| **Download progress tracking** (FR-016) | Live updates during download | WorkManager + StateFlow integration |

### State Architecture Decisions

#### Decision 1: Centralized Browser State

**Problem:** Multiple UI elements (address bar, command bar, tabs) need synchronized state.

**Solution:** Create `BrowserStateHolder` to centralize browser-level state.

```kotlin
/**
 * Centralized state holder for browser UI state
 * Addresses FR-010, FR-011, FR-005
 */
data class BrowserUiState(
    val commandBarVisible: Boolean = true,
    val addressBarVisible: Boolean = true,
    val tabBarVisible: Boolean = true,
    val headlessModeActive: Boolean = false,
    val isFullscreen: Boolean = false,
    val networkStatus: NetworkStatus = NetworkStatus.UNKNOWN,
    val downloadProgress: Map<String, Int> = emptyMap() // downloadId → progress %
)

class BrowserStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private var autoHideJob: Job? = null

    fun toggleCommandBar() {
        _uiState.update { it.copy(commandBarVisible = !it.commandBarVisible) }
        cancelAutoHideTimer() // FR-011: Cancel timer on manual toggle
    }

    fun startAutoHideTimer(delayMs: Long = 5000) {
        if (!settings.commandBarAutoHide) return

        autoHideJob?.cancel()
        autoHideJob = viewModelScope.launch {
            delay(delayMs)
            _uiState.update { it.copy(commandBarVisible = false) }
        }
    }

    fun cancelAutoHideTimer() {
        autoHideJob?.cancel()
        autoHideJob = null
    }

    fun setHeadlessMode(active: Boolean) {
        _uiState.update {
            it.copy(
                headlessModeActive = active,
                addressBarVisible = !active,
                commandBarVisible = !active,
                tabBarVisible = !active
            )
        }
        // Persist to settings
        settingsRepository.updateHeadlessMode(active)
    }
}
```

**Rationale:**
- Single source of truth for UI visibility state
- Explicit timer management prevents FR-011 bug
- Easy to test state transitions
- Persists headless mode to settings

#### Decision 2: Favorites/History State Management

**Problem:** Dual view modes (List/AR) with tab switching (Favorites/History) requires complex state.

**Solution:** Create dedicated `FavoritesViewModel` with view mode state.

```kotlin
/**
 * ViewModel for Favorites screen with History access
 * Addresses FR-009
 */
enum class FavoritesTab { FAVORITES, HISTORY }
enum class ViewMode { LIST, AR }

data class FavoritesUiState(
    val selectedTab: FavoritesTab = FavoritesTab.FAVORITES,
    val viewMode: ViewMode = ViewMode.LIST,
    val favorites: List<Favorite> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
    val currentArcIndex: Int = 0 // For AR view
)

class FavoritesViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        // Load saved view mode preference
        viewModelScope.launch {
            val savedViewMode = settingsRepository.getFavoritesViewMode()
            _uiState.update { it.copy(viewMode = savedViewMode) }
        }

        // Load data
        loadFavorites()
        loadHistory()
    }

    fun selectTab(tab: FavoritesTab) {
        _uiState.update { it.copy(selectedTab = tab, currentArcIndex = 0) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        // Persist preference (FR-009 acceptance criteria)
        settingsRepository.setFavoritesViewMode(mode)
    }

    fun toggleViewMode() {
        val newMode = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.AR else ViewMode.LIST
        setViewMode(newMode)
    }

    fun setArcIndex(index: Int) {
        _uiState.update { it.copy(currentArcIndex = index) }
    }

    // ... data loading methods
}
```

**Rationale:**
- Encapsulates all Favorites screen state
- Persists view mode preference (FR-009 requirement)
- Single state object simplifies UI composition
- Arc index tracking for AR view

#### Decision 3: Download State Management

**Problem:** Download progress needs live updates with notification integration.

**Solution:** Use WorkManager for background downloads + StateFlow for UI updates.

```kotlin
/**
 * Download manager with WorkManager integration
 * Addresses FR-016
 */
data class DownloadState(
    val downloads: List<Download> = emptyList(),
    val activeDownloads: Map<String, Int> = emptyMap() // downloadId → progress %
)

class DownloadManagerViewModel(
    private val downloadRepository: DownloadRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    init {
        // Observe WorkManager progress
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("download")
                .collect { workInfos ->
                    val progressMap = workInfos.associate {
                        it.id.toString() to it.progress.getInt("progress", 0)
                    }
                    _state.update { it.copy(activeDownloads = progressMap) }
                }
        }

        // Load download history
        loadDownloads()
    }

    fun startDownload(url: String, filename: String) {
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                "url" to url,
                "filename" to filename
            ))
            .addTag("download")
            .build()

        workManager.enqueue(downloadRequest)
    }

    // ... other methods
}
```

**Rationale:**
- WorkManager handles background downloads reliably
- StateFlow provides reactive UI updates
- Survives app process death
- Integrates with notification system

#### Decision 4: Network State Management

**Problem:** Need real-time connectivity updates for FR-008.

**Solution:** Create NetworkMonitor service with Flow-based API.

```kotlin
/**
 * Network connectivity monitor
 * Addresses FR-008
 */
enum class NetworkStatus {
    CONNECTED,
    DISCONNECTED,
    UNKNOWN
}

interface NetworkMonitor {
    val networkStatus: Flow<NetworkStatus>
}

class NetworkMonitorImpl(
    private val context: Context
) : NetworkMonitor {

    private val _networkStatus = MutableStateFlow(NetworkStatus.UNKNOWN)
    override val networkStatus: Flow<NetworkStatus> = _networkStatus.asStateFlow()

    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkStatus.value = NetworkStatus.CONNECTED
        }

        override fun onLost(network: Network) {
            _networkStatus.value = NetworkStatus.DISCONNECTED
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(request, networkCallback)
    }

    fun cleanup() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }
}
```

**Usage in ViewModel:**
```kotlin
class BrowserViewModel(
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private var lastFailedUrl: String? = null

    init {
        // Auto-reload on reconnection (FR-008)
        viewModelScope.launch {
            networkMonitor.networkStatus
                .filter { it == NetworkStatus.CONNECTED }
                .collect {
                    lastFailedUrl?.let { url ->
                        reload(url)
                        lastFailedUrl = null
                    }
                }
        }
    }
}
```

**Rationale:**
- Reactive connectivity updates
- Automatic cleanup via unregisterNetworkCallback
- Easy to test with mock NetworkMonitor
- Supports auto-reload on reconnect (FR-008)

---

## External API Design

### API Architecture

The external API system uses **Android Intents** for IPC (Inter-Process Communication), allowing third-party AR/XR apps to control WebAvanue.

```
┌─────────────────────┐
│  External AR/XR App │
│  (3rd party)        │
└──────────┬──────────┘
           │
           │ Sends Intent
           │ (com.augmentalis.webavanue.action.NAVIGATE)
           ↓
┌─────────────────────────────────┐
│  BrowserControlReceiver         │
│  (BroadcastReceiver)            │
│  - Validates caller signature   │
│  - Parses Intent extras         │
│  - Dispatches to handler        │
└──────────┬──────────────────────┘
           │
           │ Updates
           ↓
┌─────────────────────────────────┐
│  BrowserViewModel               │
│  - Updates state                │
│  - Controls WebView             │
│  - Sends result broadcast       │
└──────────┬──────────────────────┘
           │
           │ Result Intent
           ↓
┌─────────────────────┐
│  External AR/XR App │
│  (receives result)  │
└─────────────────────┘
```

### Intent API Design

#### Intent Actions (FR-020)

| Action | Description | Extras | Result |
|--------|-------------|--------|--------|
| `NAVIGATE` | Load URL | `url: String` | `success: Boolean`, `error: String?` |
| `BACK` | Navigate back | - | `success: Boolean`, `canGoBack: Boolean` |
| `FORWARD` | Navigate forward | - | `success: Boolean`, `canGoForward: Boolean` |
| `RELOAD` | Reload page | - | `success: Boolean` |
| `NEW_TAB` | Create new tab | `url: String?` | `tabId: String` |
| `CLOSE_TAB` | Close tab | `tabId: String` | `success: Boolean` |
| `SWITCH_TAB` | Switch active tab | `tabId: String` | `success: Boolean` |
| `HEADLESS_ON` | Enable headless mode | - | `success: Boolean` |
| `HEADLESS_OFF` | Disable headless mode | - | `success: Boolean` |
| `EXECUTE_JS` | Execute JavaScript | `script: String` | `result: String?`, `error: String?` |
| `CAPTURE_SCREENSHOT` | Take screenshot | `fullPage: Boolean` | `fileUri: String?`, `error: String?` |
| `ZOOM_IN` | Zoom in 10% | - | `currentZoom: Int` |
| `ZOOM_OUT` | Zoom out 10% | - | `currentZoom: Int` |
| `SET_ZOOM` | Set zoom level | `level: Int` (50-300) | `currentZoom: Int` |
| `GET_COOKIES` | Get cookies | `url: String` | `cookies: String` (JSON) |
| `SET_COOKIE` | Set cookie | `url: String`, `cookie: String` | `success: Boolean` |
| `CLEAR_COOKIES` | Clear cookies | `url: String?` | `success: Boolean` |
| `FIND_IN_PAGE` | Search page | `query: String` | `matchCount: Int`, `currentMatch: Int` |
| `FIND_NEXT` | Next match | - | `currentMatch: Int` |
| `FIND_PREVIOUS` | Previous match | - | `currentMatch: Int` |
| `CLEAR_FIND` | Clear highlights | - | `success: Boolean` |

#### Page Lifecycle Callbacks (FR-023)

WebAvanue broadcasts page events to subscribed apps:

| Event | Extras | Trigger |
|-------|--------|---------|
| `PAGE_STARTED` | `url: String`, `timestamp: Long` | WebViewClient.onPageStarted() |
| `PAGE_FINISHED` | `url: String`, `title: String`, `timestamp: Long` | WebViewClient.onPageFinished() |
| `PAGE_ERROR` | `url: String`, `error: String`, `errorCode: Int` | WebViewClient.onReceivedError() |
| `TITLE_CHANGED` | `url: String`, `title: String` | WebChromeClient.onReceivedTitle() |
| `WEBXR_SESSION_STARTED` | `sessionType: String` ("immersive-ar" or "immersive-vr") | JavaScript injection detects XR |
| `WEBXR_SESSION_ENDED` | `duration: Long` (milliseconds) | JavaScript injection detects XR |

#### Security Model

**Problem:** Prevent malicious apps from controlling browser without permission.

**Solution:** Three-tier security model:

```kotlin
/**
 * Security validator for external API calls
 * Implements three-tier security model
 */
object ApiSecurityValidator {

    /**
     * Tier 1: Signature validation
     * Only apps signed with whitelisted keys can send Intents
     */
    fun validateCallerSignature(context: Context, callingUid: Int): Boolean {
        val packageManager = context.packageManager
        val packages = packageManager.getPackagesForUid(callingUid) ?: return false

        return packages.any { packageName ->
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            val signature = info.signatures[0].toCharsString()

            // Check against whitelist (stored in secure preferences)
            isSignatureWhitelisted(signature)
        }
    }

    /**
     * Tier 2: Permission declaration
     * Calling app must declare custom permission in manifest
     */
    fun validatePermission(context: Context, callingUid: Int): Boolean {
        val packageManager = context.packageManager
        val packages = packageManager.getPackagesForUid(callingUid) ?: return false

        return packages.any { packageName ->
            val permissionCheck = packageManager.checkPermission(
                "com.augmentalis.webavanue.permission.CONTROL_BROWSER",
                packageName
            )
            permissionCheck == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Tier 3: User consent (first-time only)
     * Show dialog asking user to authorize the calling app
     */
    suspend fun requestUserConsent(context: Context, callingPackage: String): Boolean {
        // Check if already authorized
        if (isAppAuthorized(callingPackage)) return true

        // Show dialog (suspends until user responds)
        val result = showConsentDialog(context, callingPackage)

        // Save authorization
        if (result) {
            authorizeApp(callingPackage)
        }

        return result
    }
}
```

**AndroidManifest.xml:**
```xml
<!-- Custom permission for external apps -->
<permission
    android:name="com.augmentalis.webavanue.permission.CONTROL_BROWSER"
    android:label="Control WebAvanue Browser"
    android:description="Allows the app to control WebAvanue browser"
    android:protectionLevel="dangerous" />

<!-- BroadcastReceiver with permission requirement -->
<receiver
    android:name=".receiver.BrowserControlReceiver"
    android:exported="true"
    android:permission="com.augmentalis.webavanue.permission.CONTROL_BROWSER">
    <intent-filter>
        <action android:name="com.augmentalis.webavanue.action.NAVIGATE" />
        <action android:name="com.augmentalis.webavanue.action.BACK" />
        <!-- ... all other actions -->
    </intent-filter>
</receiver>
```

**Third-party app manifest:**
```xml
<!-- Must request permission to control WebAvanue -->
<uses-permission android:name="com.augmentalis.webavanue.permission.CONTROL_BROWSER" />
```

#### JavaScript Execution Security (FR-022)

**Problem:** Executing arbitrary JavaScript from external apps is high-risk.

**Solution:** Sandboxed execution with content validation and timeouts.

```kotlin
/**
 * Secure JavaScript executor
 * Addresses FR-022
 */
class JavaScriptExecutor(
    private val webView: WebView
) {
    companion object {
        private const val MAX_SCRIPT_LENGTH = 10_000 // 10KB limit
        private const val EXECUTION_TIMEOUT_MS = 5000L // 5 second timeout
        private val BLOCKED_APIS = setOf(
            "XMLHttpRequest",  // Prevent network access
            "fetch",           // Prevent network access
            "WebSocket",       // Prevent persistent connections
            "localStorage",    // Prevent data access
            "sessionStorage",  // Prevent data access
            "indexedDB",       // Prevent data access
            "document.cookie"  // Prevent cookie access
        )
    }

    /**
     * Execute JavaScript with security checks
     * @return Result containing script output or error
     */
    suspend fun executeScript(script: String): Result<String> = withContext(Dispatchers.Main) {
        // Validation checks
        when {
            script.length > MAX_SCRIPT_LENGTH -> {
                return@withContext Result.failure(
                    SecurityException("Script exceeds maximum length")
                )
            }
            containsBlockedApi(script) -> {
                return@withContext Result.failure(
                    SecurityException("Script contains blocked API")
                )
            }
        }

        // Execute with timeout
        try {
            withTimeout(EXECUTION_TIMEOUT_MS) {
                val result = CompletableDeferred<String>()

                webView.evaluateJavascript(script) { output ->
                    result.complete(output ?: "")
                }

                Result.success(result.await())
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(SecurityException("Script execution timeout"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun containsBlockedApi(script: String): Boolean {
        return BLOCKED_APIS.any { api ->
            script.contains(api, ignoreCase = true)
        }
    }
}
```

**Rationale:**
- Script length limit prevents DoS attacks
- Blocked APIs prevent data exfiltration
- Timeout prevents infinite loops
- Sandboxed in WebView context (no access to Android APIs)

#### Developer SDK (FR-021)

**Problem:** Raw Intent API is cumbersome for developers.

**Solution:** Create Android library wrapper with clean API.

```kotlin
/**
 * Main SDK class for WebAvanue browser control
 * Provides high-level API over Intent-based IPC
 */
class WebAvanueController(private val context: Context) {

    companion object {
        private const val PACKAGE = "com.augmentalis.Avanues.web"
        private const val RESULT_TIMEOUT_MS = 5000L
    }

    /**
     * Navigate to URL
     * @param url URL to navigate to
     * @return True if navigation succeeded
     */
    suspend fun navigate(url: String): Boolean = withContext(Dispatchers.IO) {
        val intent = Intent("com.augmentalis.webavanue.action.NAVIGATE").apply {
            putExtra("url", url)
            setPackage(PACKAGE)
        }

        sendIntentAndWaitForResult(intent) { resultIntent ->
            resultIntent.getBooleanExtra("success", false)
        }
    }

    /**
     * Execute JavaScript and get result
     * @param script JavaScript code to execute
     * @return Script output or null on error
     */
    suspend fun executeJavaScript(script: String): String? = withContext(Dispatchers.IO) {
        val intent = Intent("com.augmentalis.webavanue.action.EXECUTE_JS").apply {
            putExtra("script", script)
            setPackage(PACKAGE)
        }

        sendIntentAndWaitForResult(intent) { resultIntent ->
            resultIntent.getStringExtra("result")
        }
    }

    /**
     * Subscribe to page lifecycle events
     * @param callback Callback for page events
     * @return Registration object (call unregister() to stop)
     */
    fun subscribeToPageEvents(callback: PageCallback): CallbackRegistration {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "com.augmentalis.webavanue.event.PAGE_STARTED" -> {
                        val url = intent.getStringExtra("url") ?: return
                        callback.onPageStarted(url)
                    }
                    "com.augmentalis.webavanue.event.PAGE_FINISHED" -> {
                        val url = intent.getStringExtra("url") ?: return
                        val title = intent.getStringExtra("title") ?: ""
                        callback.onPageFinished(url, title)
                    }
                    "com.augmentalis.webavanue.event.PAGE_ERROR" -> {
                        val url = intent.getStringExtra("url") ?: return
                        val error = intent.getStringExtra("error") ?: "Unknown error"
                        callback.onPageError(url, error)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction("com.augmentalis.webavanue.event.PAGE_STARTED")
            addAction("com.augmentalis.webavanue.event.PAGE_FINISHED")
            addAction("com.augmentalis.webavanue.event.PAGE_ERROR")
        }

        context.registerReceiver(receiver, filter)

        return object : CallbackRegistration {
            override fun unregister() {
                context.unregisterReceiver(receiver)
            }
        }
    }

    // ... other methods (back, forward, reload, createTab, etc.)
}

/**
 * Callback interface for page events
 */
interface PageCallback {
    fun onPageStarted(url: String)
    fun onPageFinished(url: String, title: String)
    fun onPageError(url: String, error: String)
}

/**
 * Registration handle for unsubscribing
 */
interface CallbackRegistration {
    fun unregister()
}
```

**Sample Usage:**
```kotlin
// In AR/XR app
class MyARActivity : Activity() {
    private lateinit var browser: WebAvanueController
    private var pageCallbackRegistration: CallbackRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        browser = WebAvanueController(this)

        // Subscribe to page events
        pageCallbackRegistration = browser.subscribeToPageEvents(object : PageCallback {
            override fun onPageStarted(url: String) {
                Log.d("AR", "Loading: $url")
            }

            override fun onPageFinished(url: String, title: String) {
                Log.d("AR", "Loaded: $title")
                // Execute JavaScript to extract AR markers
                lifecycleScope.launch {
                    val markers = browser.executeJavaScript("""
                        JSON.stringify(
                            Array.from(document.querySelectorAll('.ar-marker'))
                                .map(m => ({ id: m.id, position: m.dataset.position }))
                        )
                    """)
                    processARMarkers(markers)
                }
            }

            override fun onPageError(url: String, error: String) {
                Log.e("AR", "Error loading $url: $error")
            }
        })

        // Navigate to AR-enabled website
        lifecycleScope.launch {
            browser.navigate("https://example.com/ar-experience")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pageCallbackRegistration?.unregister()
    }
}
```

**Rationale:**
- Suspending functions for async operations (Kotlin coroutines)
- Type-safe callback interfaces
- Automatic resource cleanup with CallbackRegistration
- Hides Intent complexity from developers
- Testable with mock implementations

---

## UI/UX Implementation

### Material3 Design System

WebAvanue uses **Material3** with a custom **OceanTheme**:

| Component | Specification |
|-----------|---------------|
| **Color Scheme** | Dynamic colors (Material You) |
| **Typography** | Roboto font family |
| **Shapes** | 12dp corner radius (glassmorphism) |
| **Touch Targets** | Minimum 48x48dp |
| **Glassmorphism** | 15% opacity + 12dp blur + 1px border (30% opacity) |
| **Elevation** | Material3 elevation system |
| **Animations** | 300ms spring animations |

### Glassmorphism Implementation (FR-001)

**Current Issue:** Floating action button lacks glass effect.

**Solution:** Create reusable glassmorphic modifier.

```kotlin
/**
 * Glassmorphic modifier for Material3 components
 * Addresses FR-001
 */
fun Modifier.glassmorphic(
    backgroundColor: Color,
    blurRadius: Dp = 12.dp,
    borderColor: Color? = null
): Modifier = this
    .background(backgroundColor.copy(alpha = 0.15f))
    .blur(blurRadius)
    .then(
        if (borderColor != null) {
            Modifier.border(
                width = 1.dp,
                color = borderColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
        } else Modifier
    )

// Usage on FAB
FloatingActionButton(
    onClick = { /* ... */ },
    modifier = Modifier.glassmorphic(
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary
    )
) {
    Icon(Icons.Default.Add, contentDescription = "Add")
}
```

### Responsive Layout Strategy

**Challenge:** Support portrait and landscape orientations with different layouts.

**Solution:** Use `BoxWithConstraints` for orientation detection.

```kotlin
/**
 * Responsive layout helper
 * Returns true if device is in landscape orientation
 */
@Composable
fun isLandscape(): Boolean {
    BoxWithConstraints {
        return maxWidth > maxHeight
    }
}

// Usage in VoiceCommandDialog (FR-002)
@Composable
fun VoiceCommandDialog(commands: List<VoiceCommand>) {
    val isLandscape = isLandscape()

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLandscape) 2 else 1),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(commands) { command ->
            VoiceCommandButton(
                command = command,
                modifier = Modifier.height(48.dp) // FR-003: consistent sizing
            )
        }
    }
}
```

### Arc Layout Custom Composable (FR-009)

**Challenge:** Create reusable arc/carousel layout for Favorites AR view.

**Solution:** Custom Layout with arc mathematics.

```kotlin
/**
 * Arc layout composable for spatial arrangement
 * Items are positioned along a circular arc
 * Addresses FR-009
 */
enum class ArcOrientation {
    HORIZONTAL, // Arc curves left-right (portrait)
    VERTICAL    // Arc curves top-bottom (landscape, 90° rotated)
}

@Composable
fun <T> ArcLayout(
    items: List<T>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onItemClick: (T) -> Unit,
    onItemLongPress: (T) -> Unit,
    orientation: ArcOrientation = ArcOrientation.HORIZONTAL,
    arcRadius: Dp = 400.dp,
    itemSpacing: Float = 45f, // degrees between items
    centerScale: Float = 1.0f,
    sideScale: Float = 0.6f,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T, Int) -> Unit
) {
    val radiusPx = with(LocalDensity.current) { arcRadius.toPx() }

    Layout(
        content = {
            items.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier
                        .pointerInput(item) {
                            detectTapGestures(
                                onTap = { onItemClick(item) },
                                onLongPress = { onItemLongPress(item) }
                            )
                        }
                ) {
                    itemContent(item, index)
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Detect swipe gestures
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    if (abs(dragAmount) > 50f) {
                        if (dragAmount > 0 && currentIndex < items.size - 1) {
                            onIndexChange(currentIndex + 1)
                        } else if (dragAmount < 0 && currentIndex > 0) {
                            onIndexChange(currentIndex - 1)
                        }
                    }
                }
            }
    ) { measurables, constraints ->
        val placeables = measurables.mapIndexed { index, measurable ->
            // Calculate scale based on distance from center
            val distanceFromCenter = abs(index - currentIndex)
            val scale = when (distanceFromCenter) {
                0 -> centerScale
                1 -> sideScale
                else -> sideScale * sideScale // 0.36 for far items
            }

            val itemWidth = (constraints.maxWidth * 0.3f * scale).toInt()
            val itemHeight = (itemWidth * 0.75f).toInt()

            measurable.measure(
                Constraints.fixed(itemWidth, itemHeight)
            )
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val centerX = constraints.maxWidth / 2
            val centerY = constraints.maxHeight / 2

            placeables.forEachIndexed { index, placeable ->
                // Calculate angle for this item
                val angleOffset = (index - currentIndex) * itemSpacing
                val angleRadians = Math.toRadians(angleOffset.toDouble())

                // Calculate position on arc
                val (x, y) = when (orientation) {
                    ArcOrientation.HORIZONTAL -> {
                        // Horizontal arc (portrait)
                        val arcX = centerX + (radiusPx * sin(angleRadians)).toInt()
                        val arcY = centerY + (radiusPx * (1 - cos(angleRadians))).toInt()
                        arcX to arcY
                    }
                    ArcOrientation.VERTICAL -> {
                        // Vertical arc (landscape, rotated 90°)
                        val arcX = centerX + (radiusPx * (1 - cos(angleRadians))).toInt()
                        val arcY = centerY + (radiusPx * sin(angleRadians)).toInt()
                        arcX to arcY
                    }
                }

                // Calculate opacity based on distance
                val distanceFromCenter = abs(index - currentIndex)
                val alpha = when (distanceFromCenter) {
                    0 -> 1.0f
                    1 -> 0.7f
                    else -> 0.4f
                }

                placeable.placeWithLayer(
                    x = x - placeable.width / 2,
                    y = y - placeable.height / 2,
                    layerBlock = {
                        this.alpha = alpha
                        this.scaleX = if (index == currentIndex) centerScale else sideScale
                        this.scaleY = if (index == currentIndex) centerScale else sideScale
                    }
                )
            }
        }
    }
}
```

**Usage:**
```kotlin
@Composable
fun FavoritesARView(favorites: List<Favorite>) {
    var currentIndex by remember { mutableStateOf(0) }
    val isLandscape = isLandscape()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        ArcLayout(
            items = favorites,
            currentIndex = currentIndex,
            onIndexChange = { currentIndex = it },
            onItemClick = { favorite -> /* navigate */ },
            onItemLongPress = { favorite -> /* delete */ },
            orientation = if (isLandscape) ArcOrientation.VERTICAL else ArcOrientation.HORIZONTAL,
            arcRadius = if (isLandscape) 500.dp else 400.dp,
            itemSpacing = if (isLandscape) 35f else 45f
        ) { favorite, index ->
            FavoriteThumbnailCard(
                favorite = favorite,
                isCenterItem = index == currentIndex
            )
        }
    }
}
```

**Rationale:**
- Custom Layout for precise positioning
- Generic type parameter for reusability
- Orientation-aware (portrait/landscape)
- Smooth swipe gesture handling
- Automatic scaling and opacity based on position

---

## Feature Implementation

### Tab Groups (FR-015)

**Architecture:**

```kotlin
/**
 * Domain model for tab groups
 */
@Serializable
data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: GroupColor,
    val isCollapsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class GroupColor(val color: Color) {
    RED(Color(0xFFE57373)),
    ORANGE(Color(0xFFFFB74D)),
    YELLOW(Color(0xFFFFF176)),
    GREEN(Color(0xFF81C784)),
    BLUE(Color(0xFF64B5F6)),
    PURPLE(Color(0xFFBA68C8)),
    PINK(Color(0xFFF06292)),
    GRAY(Color(0xFFBDBDBD))
}

/**
 * Updated Tab model with group reference
 */
@Serializable
data class Tab(
    val id: String,
    val url: String,
    val title: String,
    val faviconUrl: String?,
    val groupId: String? = null, // NEW: Reference to TabGroup
    val position: Int,
    val isActive: Boolean,
    val createdAt: Long
)
```

**Repository:**

```kotlin
/**
 * Repository for tab group persistence
 */
interface TabGroupRepository {
    suspend fun createGroup(name: String, color: GroupColor): TabGroup
    suspend fun updateGroup(group: TabGroup)
    suspend fun deleteGroup(groupId: String)
    suspend fun getAllGroups(): List<TabGroup>
    suspend fun assignTabToGroup(tabId: String, groupId: String?)
    suspend fun getTabsInGroup(groupId: String): List<Tab>
}

class TabGroupRepositoryImpl(
    private val database: BrowserDatabase
) : TabGroupRepository {

    override suspend fun createGroup(name: String, color: GroupColor): TabGroup {
        val group = TabGroup(name = name, color = color)
        database.tabGroupDao().insert(group)
        return group
    }

    override suspend fun assignTabToGroup(tabId: String, groupId: String?) {
        database.tabDao().updateGroupId(tabId, groupId)
    }

    // ... other methods
}
```

**UI Implementation:**

```kotlin
/**
 * Tab switcher with group support
 * Addresses FR-015
 */
@Composable
fun TabSwitcherWithGroups(
    tabs: List<Tab>,
    groups: List<TabGroup>,
    onTabClick: (Tab) -> Unit,
    onTabClose: (Tab) -> Unit,
    onCreateGroup: () -> Unit,
    onAssignToGroup: (Tab) -> Unit
) {
    LazyColumn {
        // Ungrouped tabs
        val ungroupedTabs = tabs.filter { it.groupId == null }
        items(ungroupedTabs) { tab ->
            TabCard(
                tab = tab,
                onTabClick = onTabClick,
                onTabClose = onTabClose,
                onAssignToGroup = { onAssignToGroup(tab) }
            )
        }

        // Grouped tabs
        groups.forEach { group ->
            item(key = group.id) {
                TabGroupSection(
                    group = group,
                    tabs = tabs.filter { it.groupId == group.id },
                    onTabClick = onTabClick,
                    onTabClose = onTabClose,
                    onToggleCollapse = { /* toggle */ }
                )
            }
        }

        // Create group button
        item {
            Button(
                onClick = onCreateGroup,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Group")
            }
        }
    }
}

@Composable
fun TabGroupSection(
    group: TabGroup,
    tabs: List<Tab>,
    onTabClick: (Tab) -> Unit,
    onTabClose: (Tab) -> Unit,
    onToggleCollapse: () -> Unit
) {
    Column {
        // Group header
        Surface(
            color = group.color.color.copy(alpha = 0.3f),
            onClick = onToggleCollapse,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (group.isCollapsed) Icons.Default.KeyboardArrowRight
                                  else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(group.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("${tabs.size}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Group tabs (collapsed = hidden)
        if (!group.isCollapsed) {
            tabs.forEach { tab ->
                TabCard(
                    tab = tab,
                    onTabClick = onTabClick,
                    onTabClose = onTabClose,
                    modifier = Modifier.padding(start = 32.dp)
                )
            }
        }
    }
}
```

**Drag-and-Drop:**

```kotlin
/**
 * Drag-and-drop support for tab grouping
 */
@Composable
fun DraggableTabCard(
    tab: Tab,
    onDragStart: () -> Unit,
    onDragEnd: (TabGroup?) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var currentDropTarget by remember { mutableStateOf<TabGroup?>(null) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDragStart()
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd(currentDropTarget)
                        currentDropTarget = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Update drop target based on position
                        // (implement hit testing logic)
                    }
                )
            }
            .alpha(if (isDragging) 0.5f else 1.0f)
    ) {
        TabCard(tab = tab)
    }
}
```

### File Downloads (FR-016)

**Architecture:**

```kotlin
/**
 * Domain model for downloads
 */
@Serializable
data class Download(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val filename: String,
    val mimeType: String,
    val fileSize: Long, // bytes
    val downloadedSize: Long, // bytes
    val status: DownloadStatus,
    val filePath: String?,
    val error: String?,
    val startedAt: Long,
    val completedAt: Long? = null
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

**Download Manager:**

```kotlin
/**
 * Download manager using WorkManager
 * Addresses FR-016
 */
class DownloadManager(
    private val context: Context,
    private val workManager: WorkManager,
    private val downloadRepository: DownloadRepository
) {

    /**
     * Start a new download
     * @return Download ID for tracking
     */
    suspend fun startDownload(
        url: String,
        filename: String,
        mimeType: String
    ): String {
        val download = Download(
            url = url,
            filename = filename,
            mimeType = mimeType,
            fileSize = 0L, // Unknown until download starts
            downloadedSize = 0L,
            status = DownloadStatus.PENDING,
            filePath = null,
            error = null,
            startedAt = System.currentTimeMillis()
        )

        // Save to database
        downloadRepository.insertDownload(download)

        // Create WorkManager request
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                "downloadId" to download.id,
                "url" to url,
                "filename" to filename,
                "mimeType" to mimeType
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        if (settings.downloadOverWiFiOnly) NetworkType.UNMETERED
                        else NetworkType.CONNECTED
                    )
                    .build()
            )
            .addTag("download")
            .build()

        workManager.enqueue(downloadRequest)

        return download.id
    }

    suspend fun pauseDownload(downloadId: String) {
        workManager.cancelWorkById(UUID.fromString(downloadId))
        downloadRepository.updateStatus(downloadId, DownloadStatus.PAUSED)
    }

    suspend fun resumeDownload(downloadId: String) {
        val download = downloadRepository.getDownload(downloadId) ?: return
        startDownload(download.url, download.filename, download.mimeType)
    }

    suspend fun cancelDownload(downloadId: String) {
        workManager.cancelWorkById(UUID.fromString(downloadId))
        downloadRepository.updateStatus(downloadId, DownloadStatus.CANCELLED)
    }

    suspend fun deleteDownload(downloadId: String) {
        val download = downloadRepository.getDownload(downloadId) ?: return

        // Delete file
        download.filePath?.let { path ->
            File(path).delete()
        }

        // Remove from database
        downloadRepository.deleteDownload(downloadId)
    }
}
```

**Download Worker:**

```kotlin
/**
 * WorkManager worker for background downloads
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString("downloadId") ?: return Result.failure()
        val url = inputData.getString("url") ?: return Result.failure()
        val filename = inputData.getString("filename") ?: return Result.failure()

        return try {
            // Update status
            downloadRepository.updateStatus(downloadId, DownloadStatus.DOWNLOADING)

            // Download file with progress tracking
            val file = downloadFile(url, filename) { progress ->
                // Update progress
                setProgress(workDataOf("progress" to progress))
                downloadRepository.updateProgress(downloadId, progress)
            }

            // Update to completed
            downloadRepository.updateDownload(downloadId, {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = file.absolutePath,
                    completedAt = System.currentTimeMillis()
                )
            })

            // Show notification
            showCompletionNotification(filename, file)

            Result.success()
        } catch (e: Exception) {
            // Update to failed
            downloadRepository.updateDownload(downloadId, {
                it.copy(
                    status = DownloadStatus.FAILED,
                    error = e.message
                )
            })

            Result.failure()
        }
    }

    private suspend fun downloadFile(
        url: String,
        filename: String,
        onProgress: (Int) -> Unit
    ): File {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Download failed: ${response.code}")
        }

        val body = response.body ?: throw IOException("Empty response body")
        val contentLength = body.contentLength()

        // Download to app-specific directory
        val downloadsDir = File(applicationContext.getExternalFilesDir(null), "Downloads")
        downloadsDir.mkdirs()
        val file = File(downloadsDir, filename)

        // Write with progress tracking
        file.outputStream().use { output ->
            val input = body.byteStream()
            val buffer = ByteArray(8192)
            var totalRead = 0L
            var read: Int

            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                totalRead += read

                if (contentLength > 0) {
                    val progress = ((totalRead * 100) / contentLength).toInt()
                    onProgress(progress)
                }
            }
        }

        return file
    }
}
```

**WebView Integration:**

```kotlin
/**
 * Custom WebViewClient with download handling
 */
class BrowserWebViewClient(
    private val downloadManager: DownloadManager
) : WebViewClient() {

    override fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        // Extract filename from Content-Disposition header
        val filename = extractFilename(contentDisposition, url)

        // Start download
        viewModelScope.launch {
            val downloadId = downloadManager.startDownload(url, filename, mimeType)

            // Show toast
            Toast.makeText(
                context,
                "Downloading $filename...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun extractFilename(contentDisposition: String, url: String): String {
        // Try to extract from Content-Disposition header
        val filenamePattern = "filename=\"?([^\"]+)\"?".toRegex()
        val match = filenamePattern.find(contentDisposition)
        if (match != null) {
            return match.groupValues[1]
        }

        // Fallback to URL
        return url.substringAfterLast('/').substringBefore('?')
    }
}
```

---

## Swarm Coordination

### Swarm Architecture

The swarm uses the **Router Pattern** with stigmergy (indirect coordination via shared state).

```
┌─────────────────────────────────────┐
│  Router Agent (Coordinator)         │
│  - Assigns tasks to specialists     │
│  - Monitors progress                │
│  - Resolves conflicts               │
│  - Manages shared state             │
└──────────┬──────────────────────────┘
           │
           ├──> UI Agent (T001-T013, T025-T027)
           ├──> State Agent (T014-T016, T028-T030)
           ├──> Feature Agent (T017-T023)
           ├──> API Agent (T031-T040)
           └──> Integration Agent (T041-T045)
```

### Stigmergy State Files

Agents communicate via state files in `.ideacode/swarm-state/`:

```bash
.ideacode/swarm-state/
├── task-assignments.json     # Task → Agent mapping
├── task-status.json           # Task completion status
├── conflicts.json             # Detected conflicts
├── dependencies.json          # Task dependencies
└── agent-health.json          # Agent status
```

**task-status.json:**
```json
{
  "T001": {
    "status": "completed",
    "agent": "State-Agent",
    "startedAt": 1701648000000,
    "completedAt": 1701649800000,
    "duration": 1800000,
    "filesModified": [
      "common/webavanue/universal/presentation/viewmodel/BrowserViewModel.kt",
      "common/webavanue/universal/presentation/ui/browser/BrowserScreen.kt"
    ],
    "testsAdded": 3,
    "testsPassed": 3
  },
  "T002": {
    "status": "in_progress",
    "agent": "State-Agent",
    "startedAt": 1701649800000,
    "filesModified": [
      "common/webavanue/universal/presentation/viewmodel/BrowserViewModel.kt"
    ]
  }
}
```

### Conflict Resolution

**Problem:** Multiple agents may modify the same file.

**Solution:** Priority-based conflict resolution.

| Priority | Concern | When to Apply |
|----------|---------|---------------|
| 1. Security | Prevents vulnerabilities | Always highest priority |
| 2. Performance | Prevents regressions | High priority for critical paths |
| 3. UX | Improves user experience | Medium priority |
| 4. Maintainability | Code quality | Lowest priority |

**Example:**
- **T034 (API Agent)** adds JavaScript execution
- **T042 (Integration Agent)** adds input validation

**Conflict:** Both modify `JavaScriptExecutor.kt`

**Resolution:** Security (T042) takes priority over feature (T034)

### Agent Communication Protocol

**State Update Format:**
```json
{
  "agent": "UI-Agent",
  "timestamp": 1701649800000,
  "event": "task_completed",
  "data": {
    "taskId": "T004",
    "filesModified": [
      "common/webavanue/universal/presentation/ui/browser/BrowserScreen.kt"
    ],
    "testResults": {
      "passed": 2,
      "failed": 0
    }
  }
}
```

**Health Check Format:**
```json
{
  "UI-Agent": {
    "status": "healthy",
    "currentTask": "T005",
    "tasksCompleted": 3,
    "lastHeartbeat": 1701649800000
  },
  "State-Agent": {
    "status": "blocked",
    "currentTask": "T002",
    "blockReason": "Waiting for T001 completion",
    "tasksCompleted": 1,
    "lastHeartbeat": 1701649800000
  }
}
```

### Monitoring Dashboard

**Scrum Master Agent** monitors swarm health every 30s:

```
╔════════════════════════════════════════════════════════════╗
║           WebAvanue v1.9.0 Swarm Dashboard                 ║
╠════════════════════════════════════════════════════════════╣
║ Phase: 1 (Critical)                    Progress: 62% ████▒ ║
║ Time Elapsed: 12h 34m                  ETA: 8h 15m         ║
║ Tasks Completed: 10/16                 Tests: 28/28 ✓      ║
╠════════════════════════════════════════════════════════════╣
║ Agent Status:                                              ║
║  ✓ UI-Agent       │ T005 │ 4 completed │ Healthy          ║
║  ✓ State-Agent    │ T002 │ 2 completed │ Healthy          ║
║  ✓ Feature-Agent  │ T011 │ 2 completed │ Healthy          ║
║  ⚠ API-Agent      │ Idle │ 0 completed │ Waiting (Phase 3) ║
║  ⚠ Integration    │ Idle │ 0 completed │ Waiting (Phase 4) ║
╠════════════════════════════════════════════════════════════╣
║ Recent Events:                                             ║
║  [12:34:15] UI-Agent completed T004 (FAB glassmorphism)   ║
║  [12:33:42] State-Agent started T002 (auto-hide logic)    ║
║  [12:31:20] UI-Agent completed T003 (command bar center)  ║
╠════════════════════════════════════════════════════════════╣
║ Issues: None                                               ║
╚════════════════════════════════════════════════════════════╝
```

---

## Testing Strategy

### Test Coverage Targets

| Layer | Target | Critical Paths |
|-------|--------|----------------|
| **Unit Tests** | 85% | ViewModels, repositories, utilities |
| **Integration Tests** | 70% | API layer, database operations |
| **UI Tests** | 60% | Critical user flows |
| **E2E Tests** | 40% | Full feature workflows |
| **Overall** | 90%+ | All critical paths |

### Test Pyramid

```
         ╱╲
        ╱  ╲       E2E Tests (10%)
       ╱────╲      - Headless mode workflow
      ╱      ╲     - Download full workflow
     ╱────────╲    - Tab groups CRUD
    ╱          ╲
   ╱────────────╲  UI Tests (20%)
  ╱              ╲ - Screen navigation
 ╱────────────────╲ - Dialog interactions
╱                  ╲
────────────────────  Integration Tests (30%)
                      - API Intent handling
                      - Database operations
                      - Network monitoring

════════════════════  Unit Tests (40%)
                      - ViewModel logic
                      - Repository methods
                      - Utility functions
```

### Test Categories

#### Unit Tests (40%)

**Target:** ViewModels, repositories, utilities

**Example: BrowserStateViewModel Test**
```kotlin
class BrowserStateViewModelTest {

    private lateinit var viewModel: BrowserStateViewModel
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        viewModel = BrowserStateViewModel()
    }

    @Test
    fun `toggleCommandBar updates state correctly`() = runTest {
        // Given
        val initialState = viewModel.uiState.value
        assertTrue(initialState.commandBarVisible)

        // When
        viewModel.toggleCommandBar()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val newState = viewModel.uiState.value
        assertFalse(newState.commandBarVisible)
    }

    @Test
    fun `autoHideTimer cancels on manual toggle`() = runTest {
        // Given
        viewModel.startAutoHideTimer(delayMs = 1000)
        delay(500) // Wait halfway

        // When
        viewModel.toggleCommandBar() // Should cancel timer
        delay(600) // Wait past timer duration

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.commandBarVisible) // Manual toggle ON, timer canceled
    }

    @Test
    fun `setHeadlessMode hides all UI elements`() = runTest {
        // When
        viewModel.setHeadlessMode(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.headlessModeActive)
        assertFalse(state.addressBarVisible)
        assertFalse(state.commandBarVisible)
        assertFalse(state.tabBarVisible)
    }
}
```

**Example: DownloadManager Test**
```kotlin
class DownloadManagerTest {

    private lateinit var downloadManager: DownloadManager
    private lateinit var mockWorkManager: WorkManager
    private lateinit var mockRepository: DownloadRepository

    @Before
    fun setup() {
        mockWorkManager = mock()
        mockRepository = mock()
        downloadManager = DownloadManager(
            context = ApplicationProvider.getApplicationContext(),
            workManager = mockWorkManager,
            downloadRepository = mockRepository
        )
    }

    @Test
    fun `startDownload creates download record and enqueues work`() = runTest {
        // When
        val downloadId = downloadManager.startDownload(
            url = "https://example.com/file.pdf",
            filename = "file.pdf",
            mimeType = "application/pdf"
        )

        // Then
        verify(mockRepository).insertDownload(any())
        verify(mockWorkManager).enqueue(any<OneTimeWorkRequest>())
        assertNotNull(downloadId)
    }

    @Test
    fun `pauseDownload cancels work and updates status`() = runTest {
        // Given
        val downloadId = "test-download-id"

        // When
        downloadManager.pauseDownload(downloadId)

        // Then
        verify(mockWorkManager).cancelWorkById(UUID.fromString(downloadId))
        verify(mockRepository).updateStatus(downloadId, DownloadStatus.PAUSED)
    }
}
```

#### Integration Tests (30%)

**Target:** API layer, database operations, network monitoring

**Example: Intent API Integration Test**
```kotlin
@RunWith(AndroidJUnit4::class)
class BrowserControlReceiverIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `NAVIGATE intent loads URL successfully`() {
        // Given
        val targetUrl = "https://www.example.com"
        val intent = Intent("com.augmentalis.webavanue.action.NAVIGATE").apply {
            putExtra("url", targetUrl)
            setPackage(context.packageName)
        }

        // When
        context.sendBroadcast(intent)

        // Then - Wait for result broadcast
        val resultIntent = waitForBroadcast(
            action = "com.augmentalis.webavanue.result.NAVIGATE",
            timeoutMs = 5000
        )

        assertTrue(resultIntent.getBooleanExtra("success", false))

        // Verify WebView loaded URL
        onView(withId(R.id.webview))
            .check(matches(withWebViewUrl(targetUrl)))
    }

    @Test
    fun `EXECUTE_JS intent returns script result`() {
        // Given
        val script = "document.title"
        val intent = Intent("com.augmentalis.webavanue.action.EXECUTE_JS").apply {
            putExtra("script", script)
            setPackage(context.packageName)
        }

        // Load a page first
        loadPage("https://www.example.com")

        // When
        context.sendBroadcast(intent)

        // Then
        val resultIntent = waitForBroadcast(
            action = "com.augmentalis.webavanue.result.EXECUTE_JS",
            timeoutMs = 5000
        )

        val result = resultIntent.getStringExtra("result")
        assertEquals("\"Example Domain\"", result) // JSON string format
    }

    @Test
    fun `CAPTURE_SCREENSHOT intent creates file`() {
        // Given
        loadPage("https://www.example.com")
        val intent = Intent("com.augmentalis.webavanue.action.CAPTURE_SCREENSHOT").apply {
            putExtra("fullPage", false)
            setPackage(context.packageName)
        }

        // When
        context.sendBroadcast(intent)

        // Then
        val resultIntent = waitForBroadcast(
            action = "com.augmentalis.webavanue.result.CAPTURE_SCREENSHOT",
            timeoutMs = 10000
        )

        val fileUri = resultIntent.getStringExtra("fileUri")
        assertNotNull(fileUri)

        // Verify file exists
        val file = File(Uri.parse(fileUri).path!!)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }
}
```

**Example: Database Integration Test**
```kotlin
@RunWith(AndroidJUnit4::class)
class TabGroupDaoIntegrationTest {

    private lateinit var database: BrowserDatabase
    private lateinit var tabGroupDao: TabGroupDao
    private lateinit var tabDao: TabDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BrowserDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tabGroupDao = database.tabGroupDao()
        tabDao = database.tabDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insertGroup and assignTab works correctly`() = runTest {
        // Given
        val group = TabGroup(name = "Work", color = GroupColor.BLUE)
        tabGroupDao.insert(group)

        val tab = Tab(
            id = "tab1",
            url = "https://example.com",
            title = "Example",
            faviconUrl = null,
            groupId = null,
            position = 0,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        tabDao.insert(tab)

        // When
        tabDao.updateGroupId("tab1", group.id)

        // Then
        val tabs = tabGroupDao.getTabsInGroup(group.id)
        assertEquals(1, tabs.size)
        assertEquals("tab1", tabs[0].id)
        assertEquals(group.id, tabs[0].groupId)
    }

    @Test
    fun `deleteGroup cascades to tabs`() = runTest {
        // Given
        val group = TabGroup(name = "Work", color = GroupColor.BLUE)
        tabGroupDao.insert(group)

        val tab = Tab(
            id = "tab1",
            url = "https://example.com",
            title = "Example",
            faviconUrl = null,
            groupId = group.id,
            position = 0,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        tabDao.insert(tab)

        // When
        tabGroupDao.delete(group.id)

        // Then
        val tabs = tabDao.getAll()
        assertEquals(1, tabs.size)
        assertNull(tabs[0].groupId) // Group ID nullified, not deleted
    }
}
```

#### UI Tests (20%)

**Target:** Critical user flows, screen navigation

**Example: Favorites Screen UI Test**
```kotlin
@RunWith(AndroidJUnit4::class)
class FavoritesScreenUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `star icon opens Favorites screen`() {
        // Given - On browser screen
        composeTestRule.waitForIdle()

        // When - Click star icon
        composeTestRule.onNodeWithContentDescription("Favorites")
            .performClick()

        // Then - Favorites screen shown
        composeTestRule.onNodeWithText("Favorites")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("History")
            .assertIsDisplayed()
    }

    @Test
    fun `toggle view mode switches between List and AR`() {
        // Given - Open Favorites screen
        openFavoritesScreen()

        // When - Click view toggle button
        composeTestRule.onNodeWithContentDescription("Toggle view mode")
            .performClick()

        // Then - AR view shown
        composeTestRule.onNodeWithText("← Swipe to rotate arc →")
            .assertIsDisplayed()

        // When - Click again
        composeTestRule.onNodeWithContentDescription("Toggle view mode")
            .performClick()

        // Then - List view shown
        composeTestRule.onNodeWithText("← Swipe to rotate arc →")
            .assertDoesNotExist()
    }

    @Test
    fun `landscape orientation rotates arc to vertical`() {
        // Given - Open Favorites in AR view
        openFavoritesScreen()
        switchToARView()

        // When - Rotate to landscape
        composeTestRule.activity.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeTestRule.waitForIdle()

        // Then - Arc rotated (instruction hint at right edge)
        composeTestRule.onNodeWithText("↑ Swipe ↓")
            .assertIsDisplayed()
    }
}
```

#### E2E Tests (10%)

**Target:** Full feature workflows

**Example: Headless Mode E2E Test**
```kotlin
@RunWith(AndroidJUnit4::class)
class HeadlessModeE2ETest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun `full headless mode workflow via Intent API`() {
        // 1. External app sends headless mode ON Intent
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.sendBroadcast(
            Intent("com.augmentalis.webavanue.action.HEADLESS_ON")
                .setPackage(context.packageName)
        )

        // 2. Verify all UI hidden
        Thread.sleep(500) // Wait for animation
        onView(withId(R.id.address_bar))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.command_bar))
            .check(matches(not(isDisplayed())))

        // 3. External app navigates to URL
        context.sendBroadcast(
            Intent("com.augmentalis.webavanue.action.NAVIGATE")
                .putExtra("url", "https://www.example.com")
                .setPackage(context.packageName)
        )

        // 4. Verify page loads (WebView visible)
        Thread.sleep(2000) // Wait for page load
        onView(withId(R.id.webview))
            .check(matches(isDisplayed()))
            .check(matches(withWebViewUrl("https://www.example.com")))

        // 5. External app captures screenshot
        val screenshotIntent = Intent("com.augmentalis.webavanue.action.CAPTURE_SCREENSHOT")
            .putExtra("fullPage", false)
            .setPackage(context.packageName)
        context.sendBroadcast(screenshotIntent)

        // 6. Verify screenshot file created
        val resultIntent = waitForBroadcast(
            action = "com.augmentalis.webavanue.result.CAPTURE_SCREENSHOT",
            timeoutMs = 10000
        )
        val fileUri = resultIntent.getStringExtra("fileUri")
        assertNotNull(fileUri)
        assertTrue(File(Uri.parse(fileUri).path!!).exists())

        // 7. External app exits headless mode
        context.sendBroadcast(
            Intent("com.augmentalis.webavanue.action.HEADLESS_OFF")
                .setPackage(context.packageName)
        )

        // 8. Verify UI restored
        Thread.sleep(500) // Wait for animation
        onView(withId(R.id.address_bar))
            .check(matches(isDisplayed()))
        onView(withId(R.id.command_bar))
            .check(matches(isDisplayed()))
    }
}
```

### Test Execution Plan

**Phase 1: Unit Tests (Parallel with Development)**
- Write tests before/during implementation (TDD)
- Run locally on each task completion
- Target: 85% coverage

**Phase 2: Integration Tests (After Phase 1)**
- Run on emulator (API 29 for AOSP compatibility)
- Full database and API testing
- Target: 70% coverage

**Phase 3: UI Tests (After Phase 2)**
- Run on emulator (both orientations)
- Critical user flows only
- Target: 60% coverage

**Phase 4: E2E Tests (Before Release)**
- Run on physical devices (HMT-1 + standard Android)
- Full feature workflows
- Target: 40% coverage

**Phase 5: Regression Tests (Continuous)**
- Run full suite on every commit
- CI/CD integration with GitHub Actions
- Target: 0 regressions

---

## Risk Mitigation

### Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| AOSP Compatibility Issues | High | High | Test on HMT-1 emulator, avoid Google APIs |
| Performance Regression | Medium | High | Profiling, benchmarks, memory leak detection |
| State Desynchronization | Medium | High | Single source of truth, explicit state management |
| Security Vulnerabilities | Low | Critical | Security review, penetration testing, signature validation |
| Download Corruption | Low | Medium | Checksums, retry logic, error handling |
| Network Unreliability | High | Medium | Offline caching, retry with exponential backoff |
| Tab Group Data Loss | Low | High | Database transactions, backup/restore |
| API Breaking Changes | Low | High | Versioned API, backward compatibility |

### AOSP Compatibility

**Challenge:** WebAvanue must run on AOSP (Android Open Source Project) without Google Play Services.

**Mitigation Strategies:**

| Feature | Google Dependency | AOSP Alternative |
|---------|-------------------|------------------|
| Downloads | DownloadManager | Custom WorkManager implementation |
| Network Monitoring | ConnectivityManager | Standard Android API (no change needed) |
| Notifications | Firebase Cloud Messaging | Local notifications only |
| Maps | Google Maps SDK | OpenStreetMap (not needed for v1.9.0) |
| Sign-In | Google Sign-In | Custom auth (not needed for v1.9.0) |

**Testing Strategy:**
- Use Android API 29 emulator **without** Google Play Services
- Test on physical HMT-1 device (AOSP Android 10)
- Validate all features work without Google APIs

### Performance Benchmarks

**Targets:**

| Metric | Target | Critical |
|--------|--------|----------|
| App Launch Time | < 2s | < 3s |
| Page Load Time | < 3s | < 5s |
| Tab Switch Time | < 200ms | < 500ms |
| Memory Usage | < 200MB | < 300MB |
| Battery Drain (1h browsing) | < 15% | < 25% |
| Arc Layout Render | < 16ms (60fps) | < 33ms (30fps) |

**Profiling Tools:**
- Android Profiler (CPU, Memory, Network)
- LeakCanary (memory leaks)
- Systrace (UI jank)
- Compose Layout Inspector

### Security Review

**Review Checklist:**

**Intent API Security:**
- [ ] Signature validation implemented
- [ ] Permission declaration enforced
- [ ] User consent dialog shown (first-time)
- [ ] Input validation on all Intent extras
- [ ] SQL injection prevention
- [ ] XSS prevention in JavaScript execution
- [ ] Path traversal prevention in file operations

**WebView Security:**
- [ ] JavaScript disabled by default (opt-in per site)
- [ ] File access disabled
- [ ] Content access disabled
- [ ] Mixed content blocked
- [ ] SSL errors handled securely
- [ ] Cookie security flags set

**Data Storage Security:**
- [ ] Sensitive data encrypted (Room database)
- [ ] SharedPreferences encrypted
- [ ] File permissions restricted
- [ ] Backup rules configured

**Network Security:**
- [ ] HTTPS enforced for sensitive operations
- [ ] Certificate pinning (optional, for v2.0)
- [ ] TLS 1.2+ required

---

## Execution Timeline

### Swarm Execution (50 hours with parallelization)

**Phase 1: Critical (T001-T016) - 20 hours**

| Agent | Tasks | Parallel Duration |
|-------|-------|-------------------|
| UI-Agent | T003, T004, T005, T006, T009, T010, T012, T013, T014 | 8h |
| State-Agent | T001, T002, T007 | 4.5h |
| Feature-Agent | T008, T011, T015, T016 | 16h |

**Parallel Execution:** 16h (longest path = Feature-Agent)

**Phase 2: High Priority (T017-T030) - 15 hours**

| Agent | Tasks | Parallel Duration |
|-------|-------|-------------------|
| UI-Agent | T018, T019, T021, T022, T024, T025, T026, T027 | 15h |
| State-Agent | T017, T020, T028, T030 | 3h |
| Feature-Agent | T023, T029 | 4h |

**Parallel Execution:** 15h (longest path = UI-Agent)

**Phase 3: External APIs (T031-T040) - 12 hours**

| Agent | Tasks | Parallel Duration |
|-------|-------|-------------------|
| API-Agent | T031, T032, T033, T034, T035, T036, T037, T038, T039, T040 | 25h sequential → 12h parallel (2 tasks at a time) |

**Parallel Execution:** 12h

**Phase 4: Validation (T041-T045) - 7 hours**

| Agent | Tasks | Sequential Duration |
|-------|-------|---------------------|
| Integration-Agent | T041, T042, T043, T044, T045 | 14h sequential → 7h with test parallelization |

**Sequential Execution:** 7h (cannot parallelize validation)

**Total Swarm Time:** 16h + 15h + 12h + 7h = **50 hours**

### Sequential Execution (90 hours)

**Phase 1:** 20h + 4.5h + 16h = 40.5h
**Phase 2:** 15h + 3h + 4h = 22h
**Phase 3:** 25h
**Phase 4:** 14h

**Total Sequential Time:** 40.5h + 22h + 25h + 14h = **101.5h ≈ 90h** (with some task overlap)

**Time Savings:** 90h - 50h = **40 hours (44% reduction)**

---

## Quality Gates

### Mandatory Checks (Before Commit)

| Check | Tool | Pass Criteria |
|-------|------|---------------|
| **Unit Tests** | JUnit | 100% passing |
| **Lint Checks** | Android Lint | 0 errors, 0 warnings |
| **Code Style** | ktlint | 0 violations |
| **Static Analysis** | Detekt | 0 critical issues |
| **Memory Leaks** | LeakCanary | 0 leaks detected |
| **API Security** | Manual review | All 3-tier checks pass |
| **AOSP Compatibility** | Emulator test | All features work |

### Pre-Release Checks

| Check | Tool | Pass Criteria |
|-------|------|---------------|
| **Integration Tests** | Espresso | 100% passing |
| **UI Tests** | Compose Test | 100% passing |
| **E2E Tests** | Manual + automated | All workflows complete |
| **Performance** | Android Profiler | All benchmarks met |
| **Accessibility** | Accessibility Scanner | WCAG AA compliance |
| **Security** | OWASP ZAP | 0 high/critical vulnerabilities |
| **APK Size** | Gradle | < 50MB |
| **Device Testing** | Physical devices | HMT-1 + 2 standard Android devices |

### Continuous Monitoring

**CI/CD Pipeline (GitHub Actions):**

```yaml
name: WebAvanue CI

on:
  push:
    branches: [ WebAvanue-Develop ]
  pull_request:
    branches: [ WebAvanue-Develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run unit tests
        run: ./gradlew :Modules:WebAvanue:universal:test
      - name: Run lint
        run: ./gradlew :Modules:WebAvanue:universal:lint
      - name: Run detekt
        run: ./gradlew detekt

  androidTest:
    runs-on: macOS-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 29
          target: default
          arch: x86_64
          script: ./gradlew :Modules:WebAvanue:universal:connectedDebugAndroidTest
```

---

## Appendix

### File Tree (New/Modified Files)

```
MainAvanues/
├── specs/
│   ├── spec-webavanue-ui-ux-improvements-20251203.md (v1.3) ✓
│   ├── tasks-webavanue-ui-ux-improvements-20251203.md ✓
│   └── plan-webavanue-ui-ux-improvements-20251203.md (this file) ✓
│
├── common/webavanue/
│   ├── coredata/src/commonMain/kotlin/com/augmentalis/webavanue/domain/model/
│   │   ├── BrowserSettings.kt (modified: add fields)
│   │   ├── TabGroup.kt (NEW)
│   │   ├── Download.kt (NEW)
│   │   └── ViewMode.kt (NEW)
│   │
│   └── universal/
│       ├── presentation/
│       │   ├── viewmodel/
│       │   │   ├── BrowserStateViewModel.kt (NEW)
│       │   │   ├── FavoritesViewModel.kt (NEW)
│       │   │   ├── DownloadManagerViewModel.kt (NEW)
│       │   │   ├── NetworkMonitorViewModel.kt (NEW)
│       │   │   ├── TabViewModel.kt (modified: add group support)
│       │   │   └── SettingsViewModel.kt (modified: add new settings)
│       │   │
│       │   └── ui/
│       │       ├── browser/
│       │       │   ├── BrowserScreen.kt (modified: headless mode)
│       │       │   ├── AddressBar.kt (modified: star icon behavior)
│       │       │   ├── BottomCommandBar.kt (modified: remove buttons)
│       │       │   └── VoiceCommandDialog.kt (modified: layout)
│       │       │
│       │       ├── favorites/ (NEW)
│       │       │   ├── FavoritesScreen.kt
│       │       │   ├── FavoritesList.kt
│       │       │   ├── FavoritesARView.kt
│       │       │   ├── HistoryList.kt
│       │       │   └── HistoryARView.kt
│       │       │
│       │       ├── layout/ (NEW)
│       │       │   └── ArcLayout.kt
│       │       │
│       │       ├── downloads/ (NEW)
│       │       │   └── DownloadsScreen.kt
│       │       │
│       │       └── components/ (NEW)
│       │           ├── FavoriteThumbnailCard.kt
│       │           └── FeatureUnavailableDialog.kt
│       │
│       └── data/
│           ├── repository/
│           │   ├── TabGroupRepository.kt (NEW)
│           │   └── DownloadRepository.kt (NEW)
│           │
│           ├── database/
│           │   ├── TabGroupDao.kt (NEW)
│           │   └── DownloadDao.kt (NEW)
│           │
│           └── service/
│               ├── NetworkMonitor.kt (NEW)
│               ├── DownloadManager.kt (NEW)
│               └── FeatureCompatibility.kt (NEW)
│
└── android/apps/webavanue/src/main/kotlin/com/augmentalis/Avanues/web/android/
    ├── receiver/ (NEW)
    │   ├── HeadlessModeReceiver.kt
    │   ├── BrowserControlReceiver.kt
    │   └── IntentActions.kt
    │
    ├── api/ (NEW)
    │   ├── JavaScriptExecutor.kt
    │   ├── PageLifecycleManager.kt
    │   ├── ScreenshotManager.kt
    │   ├── WebXRDetector.kt
    │   ├── ZoomController.kt
    │   ├── CookieController.kt
    │   └── FindController.kt
    │
    └── notification/ (NEW)
        └── DownloadNotification.kt

# NEW: Developer SDK (separate module)
webavanue-sdk/
├── build.gradle.kts (NEW)
└── src/main/kotlin/com/augmentalis/webavanue/sdk/
    ├── WebAvanueController.kt (NEW)
    ├── callbacks/ (NEW)
    │   ├── NavigationCallback.kt
    │   ├── PageCallback.kt
    │   └── DownloadCallback.kt
    └── models/ (NEW)
        ├── BrowserAction.kt
        └── BrowserState.kt
```

### Dependencies (Gradle)

**New dependencies for v1.9.0:**

```kotlin
// common/webavanue/universal/build.gradle.kts

dependencies {
    // Existing dependencies...

    // WorkManager for downloads (FR-016)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // OkHttp for download networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Room for tab groups and downloads
    implementation("androidx.room:room-runtime:2.6.0")
    ksp("androidx.room:room-compiler:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")

    // Coroutines (already present, but version check)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")

    // Memory leak detection
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
```

**SDK module dependencies:**

```kotlin
// webavanue-sdk/build.gradle.kts

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
```

### Version Bump

**Current Version:** v1.8.1
**Target Version:** v1.9.0

**Version Code:** 19
**Version Name:** "1.9.0"

```kotlin
// android/apps/webavanue/build.gradle.kts

android {
    defaultConfig {
        versionCode = 19
        versionName = "1.9.0"
    }
}
```

---

## Conclusion

This implementation plan provides a comprehensive roadmap for WebAvanue v1.9.0, addressing all 28 functional requirements with:

✅ **Architecture decisions** for state management, external APIs, and feature implementation
✅ **Swarm coordination** for parallel development (45% time savings)
✅ **Testing strategy** with 90%+ coverage target
✅ **Risk mitigation** for AOSP compatibility, performance, and security
✅ **Execution timeline** with clear phase breakdown
✅ **Quality gates** for continuous monitoring

**Next Steps:**
1. Review and approve this plan
2. Set up swarm coordination environment (`.ideacode/swarm-state/`)
3. Initialize all 5 agents (UI, State, Feature, API, Integration)
4. Begin Phase 1 implementation (Critical tasks)
5. Continuous monitoring via Scrum Master dashboard

**Ready for Execution:** ✅

**Estimated Delivery:** 50 hours swarm execution + 7 days review/QA = **2-3 weeks**
