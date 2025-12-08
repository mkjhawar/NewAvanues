# BrowserAvanue Feature Implementation TODO
**Created:** 2025-11-03 14:50 PST
**Status:** Phase 1 Complete (27 files) | 61 tasks remaining
**Goal:** World-class WebView browser with 85/100 full browser parity

---

## Phase 1: Foundation ✅ COMPLETE (27 files)

### Domain Layer (5 files)
- ✅ `Tab.kt` - Domain model (139 lines)
- ✅ `Favorite.kt` - Domain model (157 lines)
- ✅ `BrowserSettings.kt` - Domain model with enums (187 lines)
- ✅ `BrowserResult.kt` - Type-safe error handling (100 lines)
- ✅ `BrowserError.kt` - Comprehensive error types (113 lines)

### Data Layer (12 files)
- ✅ `TabEntity.kt` - Room entity
- ✅ `FavoriteEntity.kt` - Room entity with indices
- ✅ `BrowserSettingsEntity.kt` - Room entity (single row)
- ✅ `BrowserTabDao.kt` - 30+ queries with Flow support
- ✅ `BrowserFavoriteDao.kt` - 40+ queries with Flow support
- ✅ `BrowserSettingsDao.kt` - Settings CRUD + bulk updates
- ✅ `BrowserAvanueDatabase.kt` - Room database (shared/exportable)
- ✅ `TabMapper.kt` - Entity ↔ Domain conversion
- ✅ `FavoriteMapper.kt` - Entity ↔ Domain with tag parsing
- ✅ `BrowserSettingsMapper.kt` - Entity ↔ Domain with enum conversion
- ✅ `BrowserRepository.kt` - Repository interface (60+ methods)
- ✅ `BrowserRepositoryImpl.kt` - Repository implementation (450 lines)

### UseCase Layer (10 files)
- ✅ `GetAllTabsUseCase.kt` - Observe tabs reactively
- ✅ `CreateTabUseCase.kt` - Create tab with URL validation
- ✅ `DeleteTabUseCase.kt` - Delete tab(s)
- ✅ `UpdateTabUseCase.kt` - Update tab, mark accessed
- ✅ `GetAllFavoritesUseCase.kt` - Observe favorites, folders
- ✅ `AddFavoriteUseCase.kt` - Add favorite (duplicate check)
- ✅ `DeleteFavoriteUseCase.kt` - Delete favorite(s)
- ✅ `NavigateUseCase.kt` - Navigate (URL, back, forward, reload)
- ✅ `GetSettingsUseCase.kt` - Observe settings reactively
- ✅ `UpdateSettingsUseCase.kt` - Update settings, reset

**Total Lines:** ~3,000 lines (estimated)

---

## Phase 2: WebView Integration + Security Fixes (7 tasks)

### 2A. Port BrowserWebView.kt ⏳
**Source:** `/Volumes/M-Drive/Coding/Warp/Avanue4/modules/browser/.../BrowserWebView.kt`
**Lines:** 274 lines
**Function:** Core WebView with enhanced features
**Features:**
- JavaScript, DOM storage, IndexedDB enabled
- Zoom controls (5 levels)
- Scroll controls (6 directions: up, down, left, right, top, bottom)
- Desktop mode user agent switching
- Page loading callbacks (onPageStarted, onPageFinished)
- Progress tracking
- Title updates
- New tab creation via onCreateWindow

### 2B. Fix SSL Error Handling 🔴 CRITICAL
**Current:** `handler?.proceed()` - bypasses ALL SSL errors
**Risk:** Man-in-the-middle attacks
**Fix:**
```kotlin
override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
    // Show dialog: "This site's security certificate is not trusted"
    // Options: [Go Back] [Advanced/Proceed Anyway]
    showSSLErrorDialog(error) { proceed ->
        if (proceed) handler?.proceed()
        else handler?.cancel()
    }
}
```

### 2C. Fix Mixed Content 🔴 CRITICAL
**Current:** `MIXED_CONTENT_ALWAYS_ALLOW`
**Risk:** HTTP content on HTTPS pages
**Fix:**
```kotlin
mixedContentMode = if (settings.enableStrictSecurity) {
    WebSettings.MIXED_CONTENT_NEVER_ALLOW
} else {
    WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
}
```

### 2D. Add CookieManager 🟡 HIGH
**Function:** Manage browser cookies
**Features:**
```kotlin
class BrowserCookieManager(context: Context) {
    private val cookieManager = CookieManager.getInstance()

    fun setCookie(url: String, value: String)
    fun getCookie(url: String): String?
    fun getAllCookies(): List<Cookie>
    fun removeCookie(url: String)
    fun removeAllCookies()
    fun setAcceptCookies(accept: Boolean)
    fun setAcceptThirdPartyCookies(webView: WebView, accept: Boolean)
}
```
**Integration:** Add to BrowserSettings entity + repository

### 2E. Add Download Support 🟡 HIGH
**Function:** Handle file downloads
**Features:**
```kotlin
webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
    // 1. Show download dialog (filename, size, location)
    // 2. Create DownloadManager request
    // 3. Track download progress
    // 4. Show notification when complete
    // 5. Save download metadata to database
}
```
**Database:** Add `DownloadEntity` (id, url, filename, filepath, size, mimeType, timestamp, status)
**UI:** Create `DownloadManagerScreen.kt` to view/manage downloads

### 2F. Create BrowserWebViewCompose 🟢
**Function:** Compose wrapper for WebView
**Lines:** ~100 lines
**Usage:**
```kotlin
@Composable
fun BrowserWebViewCompose(
    tab: Tab,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onProgressChanged: (Int) -> Unit
) {
    AndroidView(
        factory = { context ->
            BrowserWebView(context).apply {
                setCurrentTab(tab)
                setOnPageStarted(onPageStarted)
                // ...
            }
        }
    )
}
```

---

## Phase 3: Enhanced Features (6 tasks)

### 3A. Add Find in Page 🟡 MEDIUM
**Function:** Search within page
**WebView API:**
```kotlin
fun findInPage(query: String) {
    webView.findAllAsync(query) // Highlights all matches
    webView.findNext(forward = true) // Navigate matches
    webView.clearMatches() // Clear highlighting
}
```
**UI:** `FindInPageBar.kt` - Search bar with next/prev buttons
**State:** Track current match (e.g., "3 of 12")

### 3B. Add Dark Mode 🟡 MEDIUM
**Function:** Force dark mode on websites
**WebView API (API 29+):**
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    webView.settings.forceDark = if (darkMode) {
        WebSettings.FORCE_DARK_ON
    } else {
        WebSettings.FORCE_DARK_OFF
    }
}
```
**Integration:** Add to BrowserSettings + UI toggle

### 3C. Add Clear Browsing Data 🟡 MEDIUM
**Function:** Comprehensive data clearing
**Features:**
```kotlin
class ClearDataManager(context: Context, database: BrowserAvanueDatabase) {
    suspend fun clearCache()
    suspend fun clearCookies()
    suspend fun clearHistory()
    suspend fun clearLocalStorage()
    suspend fun clearFormData()
    suspend fun clearAll()
}
```
**UI:** `ClearDataDialog.kt` with checkboxes:
- [ ] Browsing history
- [ ] Cookies and site data
- [ ] Cached images and files
- [ ] Saved passwords
- [ ] Autofill form data
- [ ] Site settings

**Time Range:** Last hour / Last 24 hours / Last 7 days / Last 4 weeks / All time

### 3D. Add Popup Handling 🟡 MEDIUM
**Function:** Support window.open() popups
**WebView API:**
```kotlin
webView.settings.setSupportMultipleWindows(true)
webView.settings.javaScriptCanOpenWindowsAutomatically = settings.enablePopups

webChromeClient = object : WebChromeClient() {
    override fun onCreateWindow(...): Boolean {
        if (settings.enablePopups) {
            // Create new tab with popup URL
            onNewTab(targetUrl)
            return true
        }
        return false // Block popup
    }
}
```
**Settings:** `enablePopups: Boolean` (default: false)

### 3E. Add Permission Handling 🟡 MEDIUM
**Function:** Handle camera, mic, location permissions
**WebView API:**
```kotlin
webChromeClient = object : WebChromeClient() {
    override fun onPermissionRequest(request: PermissionRequest) {
        // Show dialog: "example.com wants to access your camera"
        showPermissionDialog(request) { granted ->
            if (granted) request.grant(request.resources)
            else request.deny()
        }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        // Show dialog: "Share your location with example.com?"
        showGeolocationDialog(origin) { granted ->
            callback.invoke(origin, granted, false)
        }
    }
}
```
**UI:** `PermissionsDialog.kt` - Request dialogs for each permission type

---

## Phase 4: Advanced Features (5 tasks)

### 4A. Add Ad Blocking 🟢 MEDIUM
**Function:** Block ads and trackers
**WebView API:**
```kotlin
webViewClient = object : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null

        // Check against blocklist
        if (adBlocker.isBlocked(url)) {
            // Return empty response
            return WebResourceResponse("text/plain", "utf-8", null)
        }

        return super.shouldInterceptRequest(view, request)
    }
}
```
**Blocklist:** EasyList format (can use existing lists)
**Settings:** `enableAdBlocking: Boolean` (default: false)
**Stats:** Track blocked ads count

### 4B. Add Private/Incognito Mode 🟢 MEDIUM
**Function:** Browse without saving history
**Implementation:**
- Separate WebView instance with no cookies/history
- In-memory only (no database writes)
- Clear on exit
```kotlin
class IncognitoWebView(context: Context) : BrowserWebView(context) {
    init {
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        CookieManager.getInstance().setAcceptCookie(false)
    }

    // Override: Don't save to database
    override fun saveToHistory() { /* no-op */ }
}
```
**UI:** Private mode indicator (purple tab bar)

### 4C. Add Web Notifications 🟢 LOW
**Function:** Handle web push notifications
**WebView API:**
```kotlin
webChromeClient = object : WebChromeClient() {
    override fun onShowNotification(...) {
        // Show Android notification
        // Requires service worker support
    }
}
```
**Note:** Limited support in WebView, requires Chrome Custom Tabs for full support

### 4D. Add Do Not Track 🟢 LOW
**Function:** Send DNT header
**Implementation:**
```kotlin
webViewClient = object : WebViewClient() {
    override fun shouldInterceptRequest(...): WebResourceResponse? {
        val headers = request?.requestHeaders?.toMutableMap() ?: mutableMapOf()
        if (settings.doNotTrack) {
            headers["DNT"] = "1"
        }
        // Create new request with headers
        return super.shouldInterceptRequest(view, modifiedRequest)
    }
}
```

### 4E. Add Console Logging 🟢 LOW
**Function:** Capture JavaScript console.log() for debugging
**WebView API:**
```kotlin
webChromeClient = object : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            Log.d("BrowserJS", "[${it.sourceId()}:${it.lineNumber()}] ${it.message()}")
        }
        return true
    }
}
```
**Settings:** `enableDevTools: Boolean` (default: false)
**Remote Debugging:**
```kotlin
if (settings.enableDevTools) {
    WebView.setWebContentsDebuggingEnabled(true)
    // Connect via chrome://inspect
}
```

---

## Phase 5: Export/Import System (13 tasks) 🆕

### Architecture

```
BrowserDataExporter
├── exportTabs() -> JSON
├── exportFavorites() -> JSON
├── exportSettings() -> JSON
├── exportCookies() -> JSON
├── exportHistory() -> JSON
└── exportAll() -> ZIP file

BrowserDataImporter
├── importTabs(json)
├── importFavorites(json)
├── importSettings(json)
├── importCookies(json)
├── importHistory(json)
└── importAll(zipFile)
```

### 5A-F. Data Exporters (6 tasks)

**5A. Create BrowserDataExporter**
```kotlin
class BrowserDataExporter(
    private val database: BrowserAvanueDatabase,
    private val cookieManager: BrowserCookieManager
) {
    suspend fun exportAll(outputFile: File): BrowserResult<ExportMetadata>
    private suspend fun createExportBundle(): ExportBundle
}
```

**5B. Export Tabs**
```kotlin
data class TabExport(
    val id: String,
    val url: String,
    val title: String?,
    val favicon: String?, // Base64 encoded
    val isDesktopMode: Boolean,
    val createdAt: Long,
    val lastAccessed: Long
)

suspend fun exportTabs(): List<TabExport> {
    return repository.getAllTabs()
        .getOrDefault(emptyList())
        .map { it.toExport() }
}
```
**Format:** JSON array
**Example:**
```json
{
  "version": "1.0",
  "exportDate": "2025-11-03T14:50:00Z",
  "tabs": [
    {
      "id": "tab-1",
      "url": "https://google.com",
      "title": "Google",
      "favicon": "data:image/png;base64,...",
      "isDesktopMode": false,
      "createdAt": 1699035000000,
      "lastAccessed": 1699036000000
    }
  ]
}
```

**5C. Export Favorites**
```kotlin
data class FavoriteExport(
    val id: String,
    val url: String,
    val title: String,
    val favicon: String?,
    val folder: String?,
    val createdAt: Long,
    val visitCount: Int,
    val lastVisited: Long?,
    val tags: List<String>,
    val notes: String?
)

suspend fun exportFavorites(): List<FavoriteExport>
```
**Format:** JSON array with folder structure
**Compatible:** Chrome bookmarks format (can import into Chrome)

**5D. Export Settings**
```kotlin
data class SettingsExport(
    val zoomLevel: String,
    val enableJavaScript: Boolean,
    val enableCookies: Boolean,
    val defaultSearchEngine: String,
    val enableVoiceCommands: Boolean,
    // ... all 30+ settings
)

suspend fun exportSettings(): SettingsExport
```
**Format:** JSON object

**5E. Export Cookies**
```kotlin
data class CookieExport(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expires: Long?,
    val secure: Boolean,
    val httpOnly: Boolean,
    val sameSite: String?
)

suspend fun exportCookies(): List<CookieExport> {
    return cookieManager.getAllCookies().map { it.toExport() }
}
```
**Format:** JSON array
**Security:** Optionally encrypt cookies (user choice)

**5F. Export History**
```kotlin
data class HistoryExport(
    val url: String,
    val title: String?,
    val visitCount: Int,
    val lastVisit: Long,
    val visits: List<HistoryVisit>
)

data class HistoryVisit(
    val timestamp: Long,
    val transitionType: String // "LINK", "TYPED", "BOOKMARK", etc.
)
```
**Database:** Need to add `BrowserHistoryEntity` + DAO
**Format:** JSON array

### 5G-L. Data Importers (6 tasks)

**5G. Create BrowserDataImporter**
```kotlin
class BrowserDataImporter(
    private val database: BrowserAvanueDatabase,
    private val cookieManager: BrowserCookieManager
) {
    suspend fun importAll(inputFile: File): BrowserResult<ImportMetadata>
    suspend fun importTabs(json: String): BrowserResult<Int> // Returns count imported
    suspend fun importFavorites(json: String): BrowserResult<Int>
    suspend fun importSettings(json: String): BrowserResult<Unit>
    suspend fun importCookies(json: String): BrowserResult<Int>
    suspend fun importHistory(json: String): BrowserResult<Int>

    private suspend fun validateImportData(data: Any): BrowserResult<Unit>
    private suspend fun handleConflicts(strategy: ConflictStrategy)
}
```

**Conflict Resolution Strategies:**
```kotlin
enum class ConflictStrategy {
    SKIP,           // Skip if exists
    REPLACE,        // Replace existing
    MERGE,          // Merge data (e.g., combine visit counts)
    CREATE_COPY     // Create duplicate with suffix
}
```

**5H. Import Tabs**
- Parse JSON
- Validate URLs
- Check for duplicates
- Insert into database
- Download favicons (optional)

**5I. Import Favorites**
- Parse JSON
- Validate URLs
- Recreate folder structure
- Handle duplicates (by URL)
- Preserve tags and notes

**5J. Import Settings**
- Parse JSON
- Validate enum values (ZoomLevel, SearchEngine, etc.)
- Fallback to defaults for invalid values
- Update database (single row, ID=1)

**5K. Import Cookies**
- Parse JSON
- Validate domains
- Check expiry dates (skip expired)
- Set via CookieManager
- **Security:** Decrypt if encrypted

**5L. Import History**
- Parse JSON
- Insert into BrowserHistoryEntity
- Update visit counts
- Preserve timestamps

### 5M. Backup/Restore UI (1 task)

**UI Components:**

**1. Backup Screen**
```kotlin
@Composable
fun BackupScreen() {
    // Select what to backup
    Checkbox("Tabs (${tabCount})")
    Checkbox("Favorites (${favoriteCount})")
    Checkbox("Settings")
    Checkbox("Cookies (${cookieCount})")
    Checkbox("History (${historyCount})")

    // Options
    Checkbox("Include favicons")
    Checkbox("Encrypt cookies")

    // Actions
    Button("Export to File") { /* Save to /Download/browser-backup-YYMMDD-HHMM.zip */ }
    Button("Export to Cloud") { /* Upload to Google Drive / Dropbox */ }
}
```

**2. Restore Screen**
```kotlin
@Composable
fun RestoreScreen() {
    // Select backup file
    Button("Choose Backup File") { /* File picker */ }

    // Show backup metadata
    Text("Backup from: ${backup.date}")
    Text("Tabs: ${backup.tabs.size}")
    Text("Favorites: ${backup.favorites.size}")

    // Conflict resolution
    RadioButton("Skip existing")
    RadioButton("Replace existing")
    RadioButton("Merge data")

    // Actions
    Button("Restore") { /* Import data */ }
}
```

**3. Auto-Backup Settings**
```kotlin
@Composable
fun AutoBackupSettings() {
    Switch("Enable auto-backup")
    Dropdown("Frequency", options = ["Daily", "Weekly", "Monthly"])
    Dropdown("Backup location", options = ["Local", "Google Drive", "Dropbox"])
    TextField("Max backups to keep", value = "5")

    Button("Backup Now")
    Button("Restore from Backup")
}
```

**Export File Format:**
```
browser-backup-251103-1450.zip
├── manifest.json          # Version, export date, checksums
├── tabs.json
├── favorites.json
├── settings.json
├── cookies.json           # Optionally encrypted
├── history.json
└── favicons/              # PNG files
    ├── favicon-hash1.png
    └── favicon-hash2.png
```

**manifest.json:**
```json
{
  "version": "1.0",
  "browserVersion": "5.0.0",
  "exportDate": "2025-11-03T14:50:00Z",
  "deviceInfo": {
    "model": "Pixel 6",
    "androidVersion": "14",
    "appVersion": "1.0.0"
  },
  "contents": {
    "tabs": { "count": 12, "checksum": "sha256..." },
    "favorites": { "count": 156, "checksum": "sha256..." },
    "settings": { "checksum": "sha256..." },
    "cookies": { "count": 842, "encrypted": true, "checksum": "sha256..." },
    "history": { "count": 3421, "checksum": "sha256..." }
  }
}
```

---

## Phase 6: Voice Commands (3 tasks)

### 6A. Port VoiceCommandProcessor.kt ⏳
**Source:** `/Volumes/M-Drive/Coding/Warp/Avanue4/modules/browser/.../VoiceCommandProcessor.kt`
**Lines:** 206 lines
**Commands:** 17+ voice commands

**Existing Commands:**
1. "new tab" - Create new tab
2. "close tab" - Close current tab
3. "go back" / "back" - Navigate back
4. "go forward" / "forward" - Navigate forward
5. "reload" / "refresh" - Reload page
6. "go to [url]" / "open [url]" - Navigate to URL
7. "scroll up" - Scroll up
8. "scroll down" - Scroll down
9. "scroll left" - Scroll left
10. "scroll right" - Scroll right
11. "scroll to top" / "top" - Scroll to top
12. "scroll to bottom" / "bottom" - Scroll to bottom
13. "zoom in" - Increase zoom
14. "zoom out" - Decrease zoom
15. "set zoom level [1-5]" - Set specific zoom
16. "desktop mode" / "mobile mode" - Toggle desktop mode
17. "add to favorites" / "bookmark this" - Add current page to favorites

### 6B. Create VoiceOSBridge.kt 🔄
**Function:** Bridge to VoiceOSCore
**Features:**
- Register voice command handlers
- Send events to VoiceOS
- Receive commands from VoiceOS
- Handle command confirmation/feedback

### 6C. Add Voice Commands for New Features 🆕
**New Commands:**
18. "find [text]" - Find in page
19. "find next" / "next match" - Next find result
20. "clear find" - Clear find highlighting
21. "dark mode on/off" - Toggle dark mode
22. "clear history" - Clear browsing history
23. "clear cookies" - Clear cookies
24. "clear all data" - Clear all browsing data
25. "download [file]" - Initiate download
26. "show downloads" - Open download manager
27. "enable ad blocking" / "disable ad blocking" - Toggle ad blocker
28. "private mode" / "incognito mode" - Open private tab
29. "export data" / "backup browser" - Export browser data
30. "import data" / "restore backup" - Import browser data

**Total:** 30 voice commands

---

## Phase 7: Presentation Layer (4 tasks)

### 7A. Create BrowserViewModel
**Lines:** ~400 lines
**State Management:**
```kotlin
class BrowserViewModel(
    private val getAllTabsUseCase: GetAllTabsUseCase,
    private val createTabUseCase: CreateTabUseCase,
    // ... inject all 10 UseCases
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    fun onEvent(event: BrowserEvent) {
        when (event) {
            is BrowserEvent.CreateTab -> createTab(event.url)
            is BrowserEvent.DeleteTab -> deleteTab(event.tabId)
            is BrowserEvent.NavigateToUrl -> navigateToUrl(event.tabId, event.url)
            // ... handle all events
        }
    }
}
```

### 7B. Create BrowserState
**Lines:** ~150 lines
```kotlin
data class BrowserState(
    val tabs: List<Tab> = emptyList(),
    val currentTabId: String? = null,
    val favorites: List<Favorite> = emptyList(),
    val settings: BrowserSettings = BrowserSettings.default(),
    val isLoading: Boolean = false,
    val error: BrowserError? = null,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val findInPageQuery: String = "",
    val findInPageResults: Int = 0,
    val findInPageCurrentMatch: Int = 0,
    val showDownloadDialog: Boolean = false,
    val pendingDownload: Download? = null,
    val downloads: List<Download> = emptyList()
)
```

### 7C. Create BrowserEvent
**Lines:** ~100 lines
```kotlin
sealed class BrowserEvent {
    // Tab events
    data class CreateTab(val url: String) : BrowserEvent()
    data class DeleteTab(val tabId: String) : BrowserEvent()
    data class SwitchTab(val tabId: String) : BrowserEvent()

    // Navigation events
    data class NavigateToUrl(val tabId: String, val url: String) : BrowserEvent()
    data class NavigateBack(val tabId: String) : BrowserEvent()
    data class NavigateForward(val tabId: String) : BrowserEvent()
    data class Reload(val tabId: String) : BrowserEvent()

    // Favorite events
    data class AddFavorite(val url: String, val title: String) : BrowserEvent()
    data class DeleteFavorite(val favoriteId: String) : BrowserEvent()

    // Find in page events
    data class FindInPage(val query: String) : BrowserEvent()
    object FindNext : BrowserEvent()
    object FindPrevious : BrowserEvent()
    object ClearFind : BrowserEvent()

    // Download events
    data class StartDownload(val download: Download) : BrowserEvent()
    data class CancelDownload(val downloadId: String) : BrowserEvent()

    // Settings events
    data class UpdateSettings(val settings: BrowserSettings) : BrowserEvent()
    object ClearBrowsingData : BrowserEvent()

    // Export/Import events
    object ExportData : BrowserEvent()
    data class ImportData(val file: File) : BrowserEvent()

    // ... more events
}
```

### 7D. Wire UseCases to ViewModel
- Inject all 10 UseCases via constructor
- Map events to UseCase calls
- Handle BrowserResult (success/error)
- Update state based on results
- Emit side effects (navigation, toasts, dialogs)

---

## Phase 8: UI Components + IPC (10 tasks)

### 8A. Create AvaUIComponents.kt
**Lines:** ~200 lines
**Function:** Abstraction layer for Compose → IDEAMagic migration
**Example:**
```kotlin
// Today: Uses Compose
@Composable
fun MagicButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) { Text(text) }
}

// Tomorrow (1-line change): Uses IDEAMagic
@Composable
fun MagicButton(text: String, onClick: () -> Unit) {
    AvaUI.Button(text = text, onClick = onClick).Render()
}

// UI code never changes!
MagicButton("New Tab") { /* ... */ }
```

### 8B-I. UI Components (8 screens/dialogs)

**8B. BrowserScreen.kt** (~300 lines)
- Main screen with WebView, address bar, tab bar
- Integrates all components

**8C. TabBar.kt** (~171 lines - port from original)
- Visual tab switcher
- Horizontal scroll list
- Close button per tab

**8D. VoiceCommandBar.kt** (~125 lines - port from original)
- Voice feedback UI
- Command recognition status
- Visual feedback for commands

**8E. AuthenticationDialog.kt** (~156 lines - port from original)
- HTTP basic auth dialog
- Username/password fields
- Remember credentials option

**8F. FindInPageBar.kt** (~80 lines - NEW)
- Search input field
- Next/Previous buttons
- Match counter (e.g., "3 of 12")
- Close button

**8G. DownloadManagerScreen.kt** (~200 lines - NEW)
- List of downloads
- Progress bars for active downloads
- Open/Share/Delete actions
- Filter by status (active, completed, failed)

**8H. PermissionsDialog.kt** (~100 lines - NEW)
- Permission request dialog
- "Allow" / "Block" buttons
- "Remember my choice" checkbox

**8I. ClearDataDialog.kt** (~150 lines - NEW)
- Checkboxes for data types
- Time range selector
- "Clear Data" button

### 8J. Create IPCBridge.kt
**Lines:** ~150 lines
**Function:** IDEAMagic IPC for inter-module communication
**Features:**
```kotlin
class BrowserIPCBridge(private val viewModel: BrowserViewModel) {

    fun initialize() {
        IPCBus.register("browser", this)
    }

    @IPCHandler("browser.openUrl")
    fun handleOpenUrl(url: String) {
        viewModel.onEvent(BrowserEvent.CreateTab(url))
    }

    @IPCHandler("browser.getCurrentUrl")
    fun handleGetCurrentUrl(): String? {
        return viewModel.state.value.currentTab?.url
    }

    // Broadcast to other modules
    fun broadcastTabChanged(tab: Tab) {
        IPCBus.broadcast("browser.tabChanged", tab)
    }
}
```
**Use Cases:**
- FileManager → Browser: Open downloaded file URL
- VoiceOS → Browser: Open URL from voice command
- Notepad → Browser: Embed web link
- Browser → VoiceOS: Announce page loaded

---

## Phase 9: Testing (5 tasks)

### 9A. Domain Model Tests
**Files:** 3 test files
**Tests:** ~72 tests total
- `TabTest.kt` - Test Tab domain logic (24 tests)
- `FavoriteTest.kt` - Test Favorite domain logic (24 tests)
- `BrowserSettingsTest.kt` - Test BrowserSettings domain logic (24 tests)

### 9B. UseCase Tests
**Files:** 10 test files
**Tests:** ~50 tests total
- Test each UseCase with success/error scenarios
- Mock repository
- Verify BrowserResult handling

### 9C. Repository Integration Tests
**Files:** 1 test file
**Tests:** ~30 tests
- Test repository with in-memory Room database
- Test mappers (entity ↔ domain)
- Test error handling

### 9D. ViewModel Tests
**Files:** 1 test file
**Tests:** ~25 tests
- Test state updates
- Test event handling
- Mock UseCases

### 9E. Export/Import Tests
**Files:** 2 test files
**Tests:** ~20 tests
- Test export/import data integrity
- Test conflict resolution
- Test encryption/decryption

**Total Tests:** ~200 tests
**Coverage Target:** 80%+

---

## Phase 10: Integration & Polish (7 tasks)

### 10A. VoiceOSCore Integration Testing
- Test voice command handling end-to-end
- Test VoiceOS event broadcasting
- Test command confirmation feedback

### 10B. IPC Communication Testing
- Test browser ↔ filemanager communication
- Test browser ↔ notepad communication
- Test browser ↔ voiceos communication

### 10C. Manual Testing - All Features
- Test all 30 voice commands
- Test WebView features (zoom, scroll, desktop mode)
- Test tabs (create, switch, close)
- Test favorites (add, delete, organize)
- Test navigation (back, forward, reload)
- Test find in page
- Test downloads
- Test clear data
- Test permissions
- Test dark mode
- Test ad blocking

### 10D. Manual Testing - Export/Import
- Export all data
- Import into clean browser
- Verify data integrity
- Test conflict resolution strategies

### 10E. Bug Fixes and Polish
- Fix any bugs found in testing
- Polish UI/UX
- Add loading states
- Add error messages

### 10F. Performance Optimization
- Optimize database queries
- Optimize WebView rendering
- Reduce memory usage
- Improve startup time

### 10G. Documentation and README
- Create README.md
- Document architecture
- Document export/import format
- Document voice commands
- Document IPC API

---

## Summary

| Phase | Tasks | Estimated Lines | Status |
|-------|-------|----------------|--------|
| Phase 1: Foundation | 27 files | ~3,000 | ✅ COMPLETE |
| Phase 2: WebView + Security | 7 tasks | ~800 | ⏳ Pending |
| Phase 3: Enhanced Features | 6 tasks | ~600 | ⏳ Pending |
| Phase 4: Advanced Features | 5 tasks | ~400 | ⏳ Pending |
| Phase 5: Export/Import | 13 tasks | ~1,000 | ⏳ Pending |
| Phase 6: Voice Commands | 3 tasks | ~300 | ⏳ Pending |
| Phase 7: Presentation | 4 tasks | ~650 | ⏳ Pending |
| Phase 8: UI + IPC | 10 tasks | ~1,500 | ⏳ Pending |
| Phase 9: Testing | 5 tasks | ~2,700 | ⏳ Pending |
| Phase 10: Integration | 7 tasks | ~500 | ⏳ Pending |
| **TOTAL** | **87 tasks** | **~11,450 lines** | **Phase 1 ✅** |

**Current Progress:** 27/87 tasks complete (31%)
**Lines Written:** ~3,000 / ~11,450 (26%)

**Timeline Estimate:**
- Phase 1: ✅ 4 hours (COMPLETE)
- Phase 2: 3 hours
- Phase 3: 3 hours
- Phase 4: 2 hours
- Phase 5: 4 hours (export/import system)
- Phase 6: 2 hours
- Phase 7: 2 hours
- Phase 8: 4 hours
- Phase 9: 4 hours
- Phase 10: 3 hours

**Total:** ~31 hours (~4 days @ 8 hrs/day)

---

## Next Steps

**Ready to Continue:** Phase 2A - Port BrowserWebView.kt (274 lines)

When you're ready, say "GO" and I'll continue with Phase 2!
