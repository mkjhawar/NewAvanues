# Developer Manual Chapter 107: WebAvanue v4.0 — KMP Voice-Controlled Browser

**Author**: Manoj Jhawar
**Version**: 4.0.0-alpha
**Status**: Active Development (Android phase complete, iOS/Desktop Phase 2)
**Last Updated**: 2026-02-22

---

## 1. Overview

WebAvanue v4.0 is a unified, voice-controlled browser module built with Kotlin Multiplatform (KMP). It provides hands-free navigation, tab management, and browser feature control through speech recognition integration with VoiceOS.

### Key Statistics
- **95% shared code** in `commonMain` (165+ source files)
- **40+ files** in `androidMain` for platform-specific WebView integration
- **Package**: `com.augmentalis.webavanue` (minSdk 29)
- **Version**: 4.0.0-alpha with semantic versioning
- **Module Group**: `com.augmentalis.webavanue:webavanue:4.0.0-alpha`

### Target Platforms
| Platform | Status | Driver | Phase |
|----------|--------|--------|-------|
| Android | Active | WebView + JSON-RPC | Phase 1 (Complete) |
| iOS | Planned | WKWebView | Phase 2 |
| Desktop | Planned | Chromium/Edge | Phase 2 |

---

## 2. Architecture Overview

WebAvanue follows a **three-layer architecture** with platform-agnostic business logic and thin platform adapters.

### Layer Stack

```
┌─────────────────────────────────────────┐
│  Voice Command Interface                │
│  (VoiceOS → VoiceCommandParser)         │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Domain Layer (95% Shared)              │
│  • BrowserRepository (tabs, favorites)  │
│  • SettingsRepository (47 options)      │
│  • DOMScraperBridge (web element mgmt)  │
│  • VoiceCommandService (sealed types)   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Data Layer (95% Shared)                │
│  • BrowserDatabase.sq (9 tables, 591L)  │
│  • SQLDelight + SQLCipher encryption    │
│  • Repository pattern with Result<T>    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Platform Layer                         │
│  • WebViewEngine (expect/actual)        │
│  • AndroidWebViewEngine                 │
│  • Network monitoring, downloads, auth  │
└─────────────────────────────────────────┘
```

### Dependency Graph

```
WebAvanue (commonMain)
├── Foundation (KMP abstractions)
├── AvanueUI (theme + components)
├── Logging (Napier structured logging)
├── Database (SQLDelight runtime)
├── AVID (unified voice identifiers)
├── VoiceOSCore (IWebCommandExecutor)
├── Kotlin Coroutines + Serialization
└── Compose Multiplatform

WebAvanue (androidMain)
├── androidx.webkit (WebView)
├── sqldelight.android-driver
├── sqlcipher (database encryption)
├── androidx.security (encrypted preferences)
└── io.sentry (crash reporting)
```

---

## 3. DOM Scraping Pipeline

The DOM scraping system is the **heart of WebAvanue's voice control capability**. It extracts interactive elements from web pages, generates stable identifiers (AVIDs), and enables voice command targeting.

### DOMScraperBridge Architecture

**File**: `src/commonMain/kotlin/.../scraper/DOMScraperBridge.kt` (1,430 lines)

**Core Responsibilities**:
1. JavaScript injection to traverse DOM tree
2. Element filtering (garbage text, ads, invisible elements)
3. Stable hashing for element identification
4. Interactive element detection (buttons, forms, links, etc.)
5. Accessible name generation (multi-language)
6. Element type classification
7. Structure hashing for change detection

### Element Extraction Pipeline

```
DOM Tree (from WebView)
    ↓
[1] Traverse with depth/breadth limits
    • MAX_ELEMENTS = 500 (per page)
    • MAX_DEPTH = 15 (parent chain)
    • MAX_TEXT_LENGTH = 100 chars
    ↓
[2] Filter garbage text
    • Language-specific patterns (CJK, RTL, symbols)
    • Ad/tracking class patterns
    • Hidden/disabled element detection
    ↓
[3] Classify element types
    • Interactive: button, link, input, select, checkbox
    • Composite: form group, list item, dialog
    • Content: heading, paragraph, image
    ↓
[4] Generate accessible names (priority chain)
    • aria-label or aria-labelledby
    • <label> associated with input
    • Text content + placeholder
    • title attribute
    • Role-based fallback
    ↓
[5] Hash element identity (djb2 algorithm)
    • Input: packageName + className + resourceId + text + contentDescription
    • Prefix: vos_
    • Result: 8-char hex (e.g., vos_a3f2b1c0)
    ↓
[6] Store in SQLDelight registry
    • IScrapedWebCommandRepository
    • Linked to tab context
    • Enables voice targeting
```

### Key Algorithms

**Element Visibility Detection**:
```kotlin
// Composite check: element must satisfy all conditions
fun isElementVisible(element: Element): Boolean {
    // CSS display != none
    // CSS visibility != hidden
    // Element within viewport bounds
    // Parent visibility chain (recursive)
    // No visibility=0 opacity
    // bounding box area > 0
}
```

**Stable Hashing (djb2)**:
```kotlin
fun hashElement(element: ElementInfo): String {
    val input = "${element.packageName}|${element.className}|${element.resourceId}|${element.text}|${element.contentDescription}"
    var hash = 5381L
    for (c in input) {
        hash = ((hash shl 5) + hash) + c.code.toLong()
    }
    return "vos_${hash.and(0xFFFFFFFFL).toString(16).padStart(8, '0')}"
}
```

**Structure Hashing (change detection)**:
```kotlin
fun hashPageStructure(elements: List<ElementInfo>): String {
    // CRC32 of sorted element hashes + hierarchy depth
    // Detects page navigation, modal opens, dynamic content load
    // Triggers automatic overlay dismiss or refresh
}
```

### JavaScript Action Scripts

The scraper injects XHR-safe scripts that WebView executes and return serialized results:

| Action | Signature | Example Use Case |
|--------|-----------|------------------|
| click | `click(elementHash: String)` | Voice: "click sign in button" |
| focus | `focus(elementHash: String)` | Voice: "focus search box" |
| input | `input(elementHash: String, text: String)` | Voice: "type hello" |
| scroll | `scroll(direction: String, amount: Int)` | Voice: "scroll down" |
| gesture | `gesture(type: SWIPE\|PINCH, vector: Vector)` | Voice: "zoom in" |
| form | `submitForm(elementHash: String)` | Voice: "submit form" |
| clipboard | `paste(elementHash: String, text: String)` | Voice: "paste from clipboard" |
| draw | `drawOnCanvas(points: List<Point>, color: String)` | Voice-assisted drawing |

### Nonce-Based Security

**Anti-replay protection** prevents unauthorized script execution:

```kotlin
// Sender generates nonce
val nonce = UUID.randomUUID().toString()
scriptCache[nonce] = DOMScript(
    code = "function executeWebAction(...) { ... }",
    timestamp = System.currentTimeMillis(),
    expiresAt = timestamp + 5_000_ms // 5s window
)

// Receiver validates nonce before execution
val script = scriptCache[nonce]
if (script?.isValid() == true) {
    webView.evaluateJavaScript(script.code, callback)
} else {
    Log.w("Security", "Invalid nonce or expired script")
}
```

---

## 4. Voice Command Integration

Voice commands flow from VoiceOS speech recognition through a command parser into JavaScript execution on the WebView.

### Command Types (Sealed Class Hierarchy)

```kotlin
sealed class VoiceCommandService {
    // Navigation commands
    data class Navigate(val direction: NavigationDirection) : VoiceCommandService()
    data class GoToUrl(val url: String) : VoiceCommandService()

    // Tab management
    data class ManageTab(val action: TabAction) : VoiceCommandService()

    // Element interaction
    data class ClickElement(val avid: String) : VoiceCommandService()
    data class InputText(val avid: String, val text: String) : VoiceCommandService()

    // Scrolling & zoom
    data class Scroll(val direction: ScrollDirection, val amount: Int) : VoiceCommandService()
    data class SetZoom(val percentage: Int) : VoiceCommandService()

    // Feature commands
    data class ToggleMode(val mode: BrowserMode) : VoiceCommandService()
    data class ManageBookmark(val action: BookmarkAction) : VoiceCommandService()
}

enum class NavigationDirection { BACK, FORWARD, RELOAD, HOME }
enum class TabAction { NEW, CLOSE, SWITCH, DUPLICATE }
enum class BrowserMode { READER_MODE, DARK_MODE, FULL_SCREEN }
```

### Command Parsing Pipeline

**Parser**: `VoiceCommandParser.parse(spokenText: String): VoiceCommandService?`

**Recognition flow**:
```
User Speech (e.g., "click the search button")
    ↓
VoiceOSCore → Speech Recognition
    ↓
Candidate phrases (fuzzy match, confidence scores)
    ↓
VoiceCommandParser
    • Pattern matching (regex, fuzzy string distance)
    • Context awareness (current tab, page structure)
    • Multi-language support (5 locales)
    ↓
Resolved Command (ClickElement(avid="vos_a3f2b1c0"))
    ↓
Execution
    • JavaScript injection via WebViewEngine
    • Result callback (success/error)
    • Feedback toast + optional audio
    ↓
Page Update (reload DOM scraper)
```

### Voice Command Mapping Examples

| Spoken Command | Parsed Type | Execution |
|----------------|-------------|-----------|
| "go back" | Navigate(BACK) | `window.history.back()` |
| "new tab" | ManageTab(NEW) | Platform-specific new tab handler |
| "zoom in" | SetZoom(currentZoom + 10) | WebView.setWebViewClient() zoom |
| "click the search box" | ClickElement(avid) | Inject click script with AVID lookup |
| "type hello world" | InputText(avid, "hello world") | Focus element, inject input event |
| "scroll down" | Scroll(DOWN, 5) | window.scrollBy(0, 500px) |
| "reader mode" | ToggleMode(READER_MODE) | UX overlay toggle + CSS injection |

---

## 5. Database Schema (BrowserDatabase.sq)

WebAvanue uses SQLDelight for persistent storage with SQLCipher encryption on Android.

### Table Overview (9 tables, 591 lines)

| Table | Purpose | Rows | Key Fields |
|-------|---------|------|-----------|
| **tab_group** | Browser window collections | ~5 | id, name, isActive |
| **tab** | Individual browser tabs | ~20 | id, groupId, url, title, faviconUrl |
| **favorite** | Bookmarked pages | ~50 | id, url, title, tags, lastVisited |
| **favorite_tag** | Bookmark categorization | ~100 | favoriteId, tag |
| **favorite_folder** | Bookmark hierarchies | ~10 | id, parentId, name |
| **history_entry** | Navigation history | ~1,000 | id, url, title, visitedAt, count |
| **download** | Download tracking | ~100 | id, url, filename, state, progress |
| **browser_settings** | Preferences (47 columns) | 1 | key, value pairs via JSON |
| **site_permission** | Per-domain permissions | ~50 | siteUrl, permission, status |

### Sample Queries (Named Queries)

```sql
-- Tab Management
SELECT * FROM tab WHERE groupId = ? ORDER BY createdAt DESC;
INSERT INTO tab (groupId, url, title, faviconUrl) VALUES (?, ?, ?, ?);
UPDATE tab SET url = ?, title = ? WHERE id = ?;
DELETE FROM tab WHERE id = ?;

-- Favorites
SELECT * FROM favorite WHERE createdAt > ? ORDER BY lastVisited DESC LIMIT 50;
SELECT f.*, COUNT(ft.tag) as tagCount FROM favorite f
  LEFT JOIN favorite_tag ft ON f.id = ft.favoriteId
  WHERE f.url = ? GROUP BY f.id;

-- History (with full-text search)
SELECT * FROM history_entry WHERE url LIKE ? OR title LIKE ?
  ORDER BY visitedAt DESC LIMIT 100;

-- Settings
SELECT value FROM browser_settings WHERE key = ?;
UPDATE browser_settings SET value = ? WHERE key = ?;

-- Permissions
SELECT * FROM site_permission WHERE siteUrl = ? AND permission = ?;
```

### Performance Indexes (16 total)

```sql
CREATE INDEX idx_tab_groupId ON tab(groupId);
CREATE INDEX idx_favorite_createdAt ON favorite(createdAt DESC);
CREATE INDEX idx_history_url ON history_entry(url);
CREATE INDEX idx_history_visitedAt ON history_entry(visitedAt DESC);
CREATE INDEX idx_download_state ON download(state);
CREATE INDEX idx_site_permission_siteUrl ON site_permission(siteUrl);
-- ... 10 more indices for query optimization
```

### Repository Interface Pattern

```kotlin
interface IBrowserRepository {
    // Tabs
    suspend fun getTabs(groupId: Long): Flow<List<TabEntity>>
    suspend fun createTab(groupId: Long, url: String): TabEntity
    suspend fun updateTab(id: Long, url: String, title: String)
    suspend fun deleteTab(id: Long)

    // Favorites
    suspend fun getFavorites(): Flow<List<FavoriteEntity>>
    suspend fun addFavorite(url: String, title: String)
    suspend fun removeFavorite(id: Long)

    // History
    suspend fun getHistory(limit: Int = 100): Flow<List<HistoryEntry>>
    suspend fun searchHistory(query: String): Flow<List<HistoryEntry>>
    suspend fun clearHistory(beforeDate: Instant? = null)

    // Error handling
    // All suspend functions return Result<T> for typed error handling
}
```

---

## 6. Settings System

WebAvanue maintains 47 configurable options for browser behavior, privacy, and accessibility. Settings are reactive (StateFlow-based) and persist across sessions via SQLDelight + DataStore.

### Settings Categories (47 Options)

| Category | Options | Example |
|----------|---------|---------|
| **Navigation** | 8 | homePageUrl, defaultSearchEngine, restorePreviousSession |
| **Privacy** | 12 | blockTrackers, blockPopups, doNotTrack, cookiePolicy, clearCacheOnExit |
| **Accessibility** | 8 | fontSize (100-200%), highContrast, reduceMotion, readingMode |
| **Performance** | 7 | cacheSize, maxOpenTabs, imageQuality, autoLoadImages |
| **Display** | 6 | darkMode, displayProfile (PHONE/TABLET/GLASS), zoomDefault |
| **Security** | 6 | requireAuthentication, certificatePinning, passwordManager |

### Settings Presets

Four predefined configurations for quick setup:

```kotlin
enum class SettingPreset {
    DEFAULT,        // Balanced defaults
    PRIVACY,        // Maximum privacy (block all tracking)
    PERFORMANCE,    // Minimal resources (low bandwidth)
    ACCESSIBILITY   // Maximum accessibility (large fonts, high contrast)
}

fun SettingPreset.applyToRepository(repo: ISettingsRepository) {
    when (this) {
        DEFAULT -> {
            repo.setDarkMode(false)
            repo.setFontSize(100)
            repo.setBlockTrackers(true)
        }
        PRIVACY -> {
            repo.setBlockTrackers(true)
            repo.setDoNotTrack(true)
            repo.setCookiePolicy(CookiePolicy.REJECT_THIRD_PARTY)
            repo.setClearCacheOnExit(true)
        }
        PERFORMANCE -> {
            repo.setImageQuality(ImageQuality.LOW)
            repo.setMaxOpenTabs(5)
            repo.setAutoLoadImages(false)
        }
        ACCESSIBILITY -> {
            repo.setFontSize(200)
            repo.setHighContrast(true)
            repo.setReduceMotion(true)
            repo.setReadingMode(true)
        }
    }
}
```

### Reactive Updates (StateFlow)

```kotlin
class SettingsRepository(private val db: BrowserDatabase) : ISettingsRepository {
    private val _darkModeState = MutableStateFlow(false)
    override val darkMode: StateFlow<Boolean> = _darkModeState.asStateFlow()

    override suspend fun setDarkMode(enabled: Boolean) {
        db.updateSetting("dark_mode", enabled.toString())
        _darkModeState.value = enabled
        // Compose recomposition automatically triggers
    }
}
```

### DataStore Keys

```kotlin
object SettingsKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val FONT_SIZE = intPreferencesKey("font_size")
    val HOME_PAGE_URL = stringPreferencesKey("home_page_url")
    val BLOCK_TRACKERS = booleanPreferencesKey("block_trackers")
    // ... 43 more keys
}
```

---

## 7. Security Features

WebAvanue implements defense-in-depth with multiple layers of protection.

### Database Encryption (SQLCipher)

```kotlin
// Android-specific encryption setup
val context: Context = ...
val db = BrowserDatabase(
    driver = SqlCipherDriver(
        schema = BrowserDatabase.Schema,
        context = context,
        key = "passphrase".toByteArray()  // Derived from device key store
    )
)
```

### Nonce-Based Script Validation

```kotlin
// Generate nonce for each script execution
val nonce = UUID.randomUUID().toString()
val scriptWithNonce = """
    (function() {
        const nonce = "$nonce";
        // Actual logic here
        WebViewBridge.scriptResult(nonce, result);
    })();
"""
webView.evaluateJavaScript(scriptWithNonce, callback)

// Verify nonce on result
fun onScriptResult(nonce: String, result: String) {
    if (nonceRegistry.isValid(nonce) && !nonceRegistry.isExpired(nonce)) {
        process(result)
        nonceRegistry.invalidate(nonce)
    } else {
        Log.e("Security", "Script execution without valid nonce")
    }
}
```

### Certificate Pinning

```kotlin
// Pin certificates for critical domains
val pinningRules = mapOf(
    "mail.google.com" to listOf(
        "sha256/ECk0lzzj+gIW7dWv5O84F0NnoWmWdnen2z8sSCHHTy8=",
        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    ),
    "accounts.google.com" to listOf(...)
)

val networkSecurityConfig: NetworkSecurityConfig = // Android manifest binding
```

### Per-Domain Permissions

```kotlin
sealed class SitePermission {
    data class Camera(val siteUrl: String, val state: PermissionState) : SitePermission()
    data class Microphone(val siteUrl: String, val state: PermissionState) : SitePermission()
    data class Geolocation(val siteUrl: String, val state: PermissionState) : SitePermission()
    data class Notification(val siteUrl: String, val state: PermissionState) : SitePermission()
    data class ClipboardRead(val siteUrl: String, val state: PermissionState) : SitePermission()
}

enum class PermissionState { ALLOW, DENY, ASK_EACH_TIME }
```

### JavaScript Escaping

```kotlin
fun escapeJavaScript(input: String): String {
    return input
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("<", "\\x3C")  // Prevent </script> breakout
}

// Usage
val userInput = "User's text"
val escaped = escapeJavaScript(userInput)
webView.evaluateJavaScript("document.getElementById('input').value = '$escaped'")
```

---

## 8. Repository Pattern with Result<T>

WebAvanue uses the repository pattern to abstract data sources and provide typed error handling.

### Result Type (Sealed Class)

```kotlin
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val error: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
        Loading -> Loading
    }

    suspend inline fun <R> flatMap(
        crossinline transform: suspend (T) -> Result<R>
    ): Result<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
        Loading -> Loading
    }
}
```

### BrowserRepository Implementation

```kotlin
class BrowserRepository(
    private val db: BrowserDatabase,
    private val webEngine: WebViewEngine
) : IBrowserRepository {

    override suspend fun getTabs(groupId: Long): Flow<List<TabEntity>> = flow {
        try {
            val tabs = db.tabQueries.selectByGroup(groupId).executeAsList()
            emit(tabs.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("BrowserRepository", "Error fetching tabs", e)
            emit(emptyList())
        }
    }

    override suspend fun createTab(
        groupId: Long,
        url: String
    ): Result<TabEntity> = try {
        db.tabQueries.insert(
            groupId = groupId,
            url = url,
            title = "New Tab",
            faviconUrl = null,
            createdAt = Clock.System.now()
        )
        val tabId = db.tabQueries.lastInsertRowId().executeAsOne()
        Success(TabEntity(id = tabId, groupId = groupId, url = url, ...))
    } catch (e: Exception) {
        Log.e("BrowserRepository", "Error creating tab", e)
        Failure(e)
    }
}
```

---

## 9. Android-Specific Implementation

WebAvanue's Android layer handles WebView configuration, network monitoring, and platform-specific features.

### WebViewEngine (Expect/Actual Pattern)

```kotlin
// commonMain
expect class WebViewEngine {
    suspend fun loadUrl(url: String)
    suspend fun executeScript(script: String): String
    suspend fun goBack()
    suspend fun goForward()
    suspend fun reload()
    fun setZoom(percentage: Int)
}

// androidMain
actual class WebViewEngine(
    private val webView: WebView,
    private val context: Context
) {
    actual suspend fun loadUrl(url: String) = withContext(Dispatchers.Main) {
        webView.loadUrl(url)
    }

    actual suspend fun executeScript(script: String): String = suspendCancellableCoroutine { continuation ->
        webView.evaluateJavaScript(script) { result ->
            continuation.resume(result ?: "")
        }
    }
}
```

### WebView Configuration

```kotlin
val webView = WebView(context).apply {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        cacheMode = WebSettings.LOAD_DEFAULT
        userAgentString = "VoiceOS WebAvanue/4.0.0"
    }

    webViewClient = BrowserWebViewClient(
        onPageFinished = { url ->
            // Trigger DOM scraper after page load
            domScraperBridge.scrapeDOM(url)
        }
    )

    webChromeClient = BrowserChromeClient(
        onPermissionRequest = { request ->
            // Handle camera/location/microphone permissions
        }
    )
}
```

### JSON-RPC Command Routing

```kotlin
// Bridge for WebView ↔ Native communication
class VoiceOSBridge(private val executor: IWebCommandExecutor) {
    @JavascriptInterface
    fun executeCommand(jsonRpc: String) {
        // Parse JSON-RPC 2.0 format
        val request = Json.decodeFromString<JsonRpcRequest>(jsonRpc)

        // Route to appropriate handler
        val command = when (request.method) {
            "click" -> ClickElement(request.params.getString("avid"))
            "input" -> InputText(request.params.getString("avid"), request.params.getString("text"))
            "scroll" -> Scroll(ScrollDirection.valueOf(request.params.getString("direction")), 5)
            else -> return sendError(request.id, "Unknown method")
        }

        executor.execute(command) { result ->
            sendJsonRpcResponse(request.id, result)
        }
    }
}
```

### Network Monitoring

```kotlin
class NetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            // Log network error
            throw e
        }

        val duration = System.currentTimeMillis() - startTime
        Log.d("Network", "${request.url} took ${duration}ms")

        return response
    }
}
```

### Download Management

```kotlin
class DownloadManager {
    fun startDownload(url: String, filename: String) {
        val downloadRequest = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setDescription("Downloading...")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadId = android.app.DownloadManager.enqueue(downloadRequest)

        // Persist to database
        db.downloadQueries.insert(
            url = url,
            filename = filename,
            downloadId = downloadId,
            state = DownloadState.IN_PROGRESS
        )
    }
}
```

### Screenshot Capture

```kotlin
suspend fun captureScreenshot(): Bitmap = withContext(Dispatchers.Main) {
    val bitmap = Bitmap.createBitmap(
        webView.width, webView.height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    webView.draw(canvas)
    bitmap
}
```

---

## 10. Build Configuration

### KMP Target Setup

```gradle
kotlin {
    // Android (Active)
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    // iOS (Phase 2)
    // listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { ... }

    // Desktop (Phase 2)
    // jvm("desktop") { compilations.all { ... } }
}

android {
    namespace = "com.augmentalis.webavanue"
    compileSdk = 35
    defaultConfig {
        minSdk = 29  // Raised to match VoiceOSCore dependency
    }
}
```

### Key Dependencies

| Dependency | Purpose | Version |
|------------|---------|---------|
| `androidx.webkit` | WebView APIs | Latest |
| `sqldelight-android-driver` | Database | 2.0.1 |
| `sqlcipher-android` | Database encryption | Latest |
| `androidx.security:security-crypto` | Encrypted preferences | 1.1.0-alpha06 |
| `io.napier:napier` | KMP logging | Latest |
| `org.jetbrains.compose.runtime` | KMP Compose | Latest |
| `io.sentry:sentry-android` | Crash reporting | 7.0.0 |
| `app.cash.voyager` | KMP navigation | Latest |

### Dokka Documentation

```gradle
tasks.withType<DokkaTask>().configureEach {
    moduleName.set("WebAvanue")
    moduleVersion.set("4.0.0-alpha")

    dokkaSourceSets {
        configureEach {
            reportUndocumented.set(true)
            skipDeprecated.set(false)
        }
    }
}
```

---

## 11. Testing Strategy

### Unit Tests (commonTest)

```kotlin
class DOMScraperBridgeTest {
    @Test
    fun testElementHashConsistency() {
        val element = ElementInfo(
            className = "ButtonViewHolder",
            text = "Save",
            contentDescription = "Save document"
        )

        val hash1 = element.hashElement()
        val hash2 = element.hashElement()

        assertEquals(hash1, hash2, "Hash must be deterministic")
    }

    @Test
    fun testGarbageTextFiltering() {
        val elements = listOf(
            ElementInfo(text = "AAAAAAA", isVisible = true),  // Garbage
            ElementInfo(text = "Click Me", isVisible = true)  // Valid
        )

        val filtered = elements.filter { !isGarbageText(it.text) }
        assertEquals(1, filtered.size)
    }
}
```

### Instrumentation Tests (androidInstrumentedTest)

```kotlin
@RunWith(AndroidJUnit4::class)
class WebViewEngineTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(BrowserActivity::class.java)

    @Test
    fun testScriptExecution() = runTest {
        val result = webEngine.executeScript("1 + 1")
        assertEquals("2", result)
    }

    @Test
    fun testDOMScrapingAfterPageLoad() = runTest {
        webEngine.loadUrl("https://example.com")
        advanceUntilIdle()

        val elements = domScraperBridge.scrapeDOM("https://example.com")
        assertTrue(elements.isNotEmpty())
    }
}
```

### Integration Tests

```kotlin
@Test
fun testVoiceCommandToWebAction() = runTest {
    // User says: "click the login button"
    val command = VoiceCommandParser.parse("click the login button")

    // Verify command resolved correctly
    assertTrue(command is ClickElement)
    assertEquals("vos_a3f2b1c0", (command as ClickElement).avid)

    // Execute command
    executor.execute(command) { result ->
        // Verify page updated post-click
        assertTrue(result.success)
    }
}
```

---

## 12. Next Steps & Roadmap

### Phase 2 (iOS & Desktop)
- [ ] iOS WKWebView wrapper with feature parity
- [ ] Desktop Chromium/Edge integration
- [ ] Unified WebViewEngine expect/actual completion
- [ ] iOS SQLDelight native driver integration

### Phase 3 (Advanced Features)
- [ ] Advanced DOM mutation detection (ResizeObserver, MutationObserver)
- [ ] Multi-tab state synchronization
- [ ] Offline page caching with Service Worker support
- [ ] Voice command learning & custom macro recording

### Known Limitations (Phase 1)
- iOS/Desktop targets not yet implemented (Phase 2 planned)
- WebView evaluation scripts limited by security sandbox (nonce mitigation deployed)
- Single tab group per session (multi-window Phase 3 planned)

---

## 13. Key Files Reference

### CommonMain Core Files

| File | Purpose | Lines |
|------|---------|-------|
| `domain/VoiceCommandService.kt` | Command type definitions | ~150 |
| `domain/VoiceCommandParser.kt` | Speech → command parsing | ~300 |
| `scraper/DOMScraperBridge.kt` | Element extraction pipeline | 1,430 |
| `repository/IBrowserRepository.kt` | Data abstraction interface | ~120 |
| `repository/BrowserRepository.kt` | Tab/favorites/history impl | ~400 |
| `repository/ISettingsRepository.kt` | Settings abstraction | ~80 |
| `repository/SettingsRepository.kt` | Settings implementation | ~250 |
| `data/BrowserDatabase.sq` | SQLDelight schema | 591 |

### AndroidMain Specific

| File | Purpose | Lines |
|------|---------|-------|
| `engine/AndroidWebViewEngine.kt` | WebView abstraction | ~200 |
| `bridge/VoiceOSBridge.kt` | JavaScript interface | ~180 |
| `network/NetworkInterceptor.kt` | HTTP monitoring | ~100 |
| `download/DownloadManager.kt` | Download handling | ~150 |
| `security/EncryptionManager.kt` | SQLCipher setup | ~80 |

---

## 14. Quick Start for Developers

### Setting Up WebAvanue Module

1. **Add dependency** to your `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(project(":Modules:WebAvanue"))
   }
   ```

2. **Initialize WebViewEngine** in your Activity:
   ```kotlin
   val webEngine = AndroidWebViewEngine(webView, context)
   val repository = BrowserRepository(db, webEngine)
   val domScraper = DOMScraperBridge(webEngine, repository)
   ```

3. **Connect Voice Commands**:
   ```kotlin
   val voiceExecutor = BrowserVoiceExecutor(
       engine = webEngine,
       scraper = domScraper,
       repository = repository
   )
   voiceOSCore.setWebCommandExecutor(voiceExecutor)
   ```

4. **Handle Voice Events**:
   ```kotlin
   voiceExecutor.commandResults.collect { result ->
       when (result) {
           is Success -> showFeedbackToast("Command executed")
           is Failure -> showErrorDialog(result.error.message)
       }
   }
   ```

---

## 15. References & Related Documentation

### Related Chapters
- **Chapter 93** (Voice Command Pipeline): Speech recognition pipeline and multi-locale support
- **Chapter 94** (4-Tier Voice Enablement): AVID system and voice accessibility tiers
- **Chapter 95** (VOS Distribution & Handler Dispatch): Command registration and routing
- **Chapter 101** (HTTPAvanue): KMP HTTP server for API interactions
- **Chapter 106** (Foundation Abstractions): Expect/actual patterns in KMP modules

### External Resources
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [SQLDelight GitHub](https://github.com/cashapp/sqldelight)
- [Compose Multiplatform](https://www.jetbrains.com/help/compose/desktop-web-getting-started.html)
- [Android WebView Developers Guide](https://developer.android.com/develop/ui/views/layout/webapps/webview)

### Contributing Guidelines

When extending WebAvanue:
1. **Maintain 95% code sharing** — Keep platform-specific code minimal
2. **Use expect/actual** for platform abstractions, NOT sealed classes or when expressions
3. **Test on device** before committing DOM scraper changes (JavaScript is fragile across browsers)
4. **Update DataStore keys** when adding new settings (backwards compat required)
5. **Always use Result<T>** for repository methods (typed error handling)
6. **Document voice commands** in both Parser and DOMScraperBridge when adding new actions

---

**Status**: Active Development
**Module Version**: 4.0.0-alpha
**Last Updated**: 2026-02-22
**Next Chapter**: 108 (RemoteAvanue: Remote Desktop Control & Accessibility)
