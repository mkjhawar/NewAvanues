# WebAvanue Enhanced WebView System - Universal Specification

**App:** WebAvanue
**Feature:** Enhanced WebView with Cockpit Integration
**Type:** Universal Specification (All Platforms)
**Version:** 1.0
**Date:** 2025-12-09
**Architecture:** KMP (Kotlin Multiplatform)

---

## Executive Summary

Enhance WebAvanue's WebView component with advanced capabilities ported from the Cockpit module, enabling voice-controlled web interactions, full state persistence, YouTube optimization, and JavaScript injection for hands-free browsing.

**Key Benefits:**
- Voice-first web browsing with natural language commands
- Seamless state restoration (scroll, zoom, media playback)
- Automatic YouTube optimization for video content
- Custom voice commands per website
- KMP architecture for cross-platform reuse

---

## Architecture Overview

### KMP Layer Structure

```
Common/WebAvanue/                          # KMP Library Module
├── src/
│   ├── commonMain/kotlin/
│   │   └── com/avanues/webavanue/
│   │       ├── webview/
│   │       │   ├── state/
│   │       │   │   ├── WindowState.kt           # PORTED FROM COCKPIT
│   │       │   │   └── StateSerializer.kt       # KMP serialization
│   │       │   ├── config/
│   │       │   │   ├── WebViewConfigPreset.kt   # Enum: STANDARD, YOUTUBE, WIDGET
│   │       │   │   ├── UrlPatterns.kt           # YouTube detection regex
│   │       │   │   └── LoginUrls.kt             # Known login URLs
│   │       │   ├── injection/
│   │       │   │   ├── CommandRegistry.kt       # Domain → Command → JS map
│   │       │   │   ├── VoiceCommand.kt          # Command data class
│   │       │   │   └── JavaScriptSnippets.kt    # Common JS utilities
│   │       │   └── voice/
│   │       │       ├── VoiceOSBridge.kt         # Interface (expect/actual)
│   │       │       └── VoiceCommandHandler.kt   # Command routing
│   │       └── utils/
│   │           ├── UrlUtils.kt                  # Domain extraction, validation
│   │           └── DomainMatcher.kt             # URL pattern matching
│   │
│   ├── androidMain/kotlin/
│   │   └── com/avanues/webavanue/
│   │       ├── webview/
│   │       │   ├── config/
│   │       │   │   └── WebViewConfig.kt         # PORTED FROM COCKPIT (Android-specific)
│   │       │   ├── injection/
│   │       │   │   └── JavaScriptInjector.kt    # PORTED FROM COCKPIT
│   │       │   ├── renderer/
│   │       │   │   ├── WebViewRenderer.kt       # PORTED FROM COCKPIT (Compose)
│   │       │   │   └── WebViewJavaScriptInterface.kt
│   │       │   └── voice/
│   │       │       └── VoiceOSBridgeImpl.kt     # Android VoiceOS integration
│   │       └── utils/
│   │           └── AndroidWebViewUtils.kt       # Android-specific utilities
│   │
│   ├── iosMain/kotlin/
│   │   └── com/avanues/webavanue/
│   │       └── webview/
│   │           ├── renderer/
│   │           │   └── WKWebViewRenderer.kt     # iOS WKWebView wrapper
│   │           └── voice/
│   │               └── VoiceOSBridgeImpl.kt     # iOS VoiceOver integration
│   │
│   └── desktopMain/kotlin/
│       └── com/avanues/webavanue/
│           └── webview/
│               ├── renderer/
│               │   └── JavaFXWebViewRenderer.kt # Desktop WebView wrapper
│               └── voice/
│                   └── VoiceOSBridgeImpl.kt     # Desktop voice integration
```

---

## Core Components (KMP Common)

### 1. WindowState.kt (commonMain)

**Purpose:** Platform-agnostic state persistence model

**Ported from:** `Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/core/window/WindowState.kt`

**Key Features:**
- Immutable data class with kotlinx.serialization
- Scroll position (scrollX, scrollY)
- Zoom level (0.5x to 3.0x with voice commands)
- Media playback state (position, isPlaying)
- Last accessed timestamp for LRU sorting

**Voice Commands:**
- "Zoom in" → `zoomIn()`
- "Zoom out" → `zoomOut()`
- "Reset zoom" → `resetZoom()`
- "Resume video" → `play()`
- "Pause" → `pause()`

**Implementation:**
```kotlin
// Location: Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/state/WindowState.kt

@Serializable
data class WindowState(
    val scrollX: Int = 0,
    val scrollY: Int = 0,
    val zoomLevel: Float = 1.0f,
    val mediaPlaybackPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT = WindowState()
        const val MIN_ZOOM = 0.5f
        const val MAX_ZOOM = 3.0f
        const val ZOOM_STEP = 0.25f
    }

    fun withScroll(x: Int, y: Int): WindowState
    fun withZoom(level: Float): WindowState
    fun zoomIn(): WindowState = withZoom(zoomLevel + ZOOM_STEP)
    fun zoomOut(): WindowState = withZoom(zoomLevel - ZOOM_STEP)
    fun resetZoom(): WindowState = withZoom(1.0f)
    fun withMediaPosition(positionMs: Long, playing: Boolean): WindowState
    fun play(): WindowState
    fun pause(): WindowState
    fun toVoiceDescription(): String
}
```

### 2. WebViewConfigPreset.kt (commonMain)

**Purpose:** Platform-agnostic WebView configuration presets

**Key Features:**
- Enum defining configuration types
- Settings data classes for each preset
- YouTube detection logic (KMP)

**Implementation:**
```kotlin
// Location: Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/config/WebViewConfigPreset.kt

enum class WebViewConfigPreset {
    STANDARD,   // General web apps
    YOUTUBE,    // Video streaming optimization
    WIDGET      // Lightweight content
}

data class WebViewSettings(
    val preset: WebViewConfigPreset,
    val enableJavaScript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val enableCaching: Boolean = true,
    val enableZoom: Boolean = true,
    val mediaAutoplay: Boolean = false,
    val mixedContentMode: MixedContentMode = MixedContentMode.ALWAYS_ALLOW,
    val userAgentOverride: String? = null
)

enum class MixedContentMode {
    ALWAYS_ALLOW,
    NEVER_ALLOW,
    COMPATIBILITY_MODE
}

object WebViewConfigFactory {
    fun standard() = WebViewSettings(preset = STANDARD)
    fun youtube() = WebViewSettings(
        preset = YOUTUBE,
        mediaAutoplay = true,
        userAgentOverride = DESKTOP_USER_AGENT
    )
    fun widget() = WebViewSettings(
        preset = WIDGET,
        enableCaching = false,
        enableZoom = false
    )
}
```

### 3. UrlPatterns.kt (commonMain)

**Purpose:** YouTube detection and URL pattern matching (KMP)

**Ported from:** `WebViewConfig.isYouTubeVideo()` logic

**Implementation:**
```kotlin
// Location: Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/config/UrlPatterns.kt

object UrlPatterns {
    private val YOUTUBE_VIDEO_REGEX = Regex(
        "^((?:https?:)?//)?((?:www|m)\\.)?(youtube(-nocookie)?\\.com|youtu.be)(/(?:[\\w\\-]+\\?v=|embed/|v/)?)([\\w\\-]+)(\\S+)?\$"
    )

    private val YOUTUBE_INVALID_PREFIXES = listOf(
        "https://www.youtube.com/youtubei/v1/att/",
        "https://m.youtube.com/static/",
        "https://m.youtube.com/s/",
        "https://m.youtube.com/youtubei/v1/",
        "https://m.youtube.com/generate",
        "https://m.youtube.com/youtubei/v1/log_event",
        "https://m.youtube.com/api/stats/",
        "https://www.youtube.com/pcs/activeview",
        "https://www.youtube.com/youtubei/v1/log_event",
        "https://www.youtube.com/api/stats/",
        "https://www.youtube.com/pagead"
    )

    fun isYouTubeVideo(url: String): Boolean {
        if (!url.contains("youtube.com", ignoreCase = true) &&
            !url.contains("youtu.be", ignoreCase = true)) {
            return false
        }

        if (!YOUTUBE_VIDEO_REGEX.matches(url)) {
            return false
        }

        return YOUTUBE_INVALID_PREFIXES.none { url.startsWith(it) }
    }

    fun getDisplayDomain(url: String): String {
        return try {
            val uri = parseUri(url) // Platform-specific implementation
            val host = uri.host ?: return url
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            url
        }
    }
}

expect fun parseUri(url: String): Uri // Platform-specific URI parsing
```

### 4. CommandRegistry.kt (commonMain)

**Purpose:** Voice command → JavaScript mapping (KMP)

**Key Features:**
- Platform-agnostic command storage
- Domain-based command organization
- JSON import/export for command files

**Implementation:**
```kotlin
// Location: Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/injection/CommandRegistry.kt

@Serializable
data class VoiceCommand(
    val domain: String,
    val command: String,
    val javascript: String,
    val description: String = ""
)

class CommandRegistry {
    private val commands: MutableMap<String, MutableMap<String, VoiceCommand>> = mutableMapOf()

    fun register(voiceCommand: VoiceCommand) {
        val domain = voiceCommand.domain.lowercase()
        val command = voiceCommand.command.uppercase()

        val domainCommands = commands.getOrPut(domain) { mutableMapOf() }
        domainCommands[command] = voiceCommand
    }

    fun getCommand(domain: String, command: String): VoiceCommand? {
        val normalizedDomain = domain.lowercase()
        val normalizedCommand = command.uppercase()
        return commands[normalizedDomain]?.get(normalizedCommand)
    }

    fun getCommandsForDomain(domain: String): List<VoiceCommand> {
        return commands[domain.lowercase()]?.values?.toList() ?: emptyList()
    }

    fun getAllDomains(): List<String> = commands.keys.toList()

    fun clear() = commands.clear()

    fun unregisterDomain(domain: String) {
        commands.remove(domain.lowercase())
    }

    // Import commands from JSON
    fun importFromJson(json: String) {
        // Platform-specific JSON parsing
    }

    // Export commands to JSON
    fun exportToJson(): String {
        // Platform-specific JSON serialization
    }
}
```

### 5. VoiceOSBridge.kt (commonMain - expect/actual)

**Purpose:** Platform-agnostic voice integration interface

**Implementation:**
```kotlin
// Location: Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/voice/VoiceOSBridge.kt

interface VoiceOSBridge {
    // WebView → VoiceOS
    suspend fun requestVoiceInput(): String
    suspend fun announceAction(action: String)
    suspend fun announcePage(title: String, url: String)

    // VoiceOS → WebView
    fun onVoiceCommand(command: String, domain: String): Boolean
    fun onScrollCommand(direction: ScrollDirection): Boolean
    fun onZoomCommand(zoomAction: ZoomAction): Boolean
}

enum class ScrollDirection {
    UP, DOWN, TOP, BOTTOM
}

enum class ZoomAction {
    IN, OUT, RESET, SET_LEVEL
}

expect class VoiceOSBridgeImpl() : VoiceOSBridge
```

---

## Platform-Specific Components (Android)

### 1. WebViewConfig.kt (androidMain)

**Purpose:** Android WebView configuration

**Ported from:** `Common/Cockpit/src/androidMain/kotlin/com/avanues/cockpit/webview/WebViewConfig.kt`

**Location:** `Common/WebAvanue/src/androidMain/kotlin/com/avanues/webavanue/webview/config/WebViewConfig.kt`

**Key Functions:**
- `applyStandardConfig(webView: WebView)`
- `applyYouTubeConfig(webView: WebView)`
- `applyWidgetConfig(webView: WebView)`
- `clearCache(webView: WebView)`
- `clearCookiesForDomain(domain: String)`

**Port Instructions:**
1. Copy entire file from Cockpit
2. Update package name to `com.avanues.webavanue.webview.config`
3. Replace `UrlPatterns.isYouTubeVideo()` call with KMP version
4. No other changes needed

### 2. JavaScriptInjector.kt (androidMain)

**Purpose:** JavaScript injection and execution

**Ported from:** `Common/Cockpit/src/androidMain/kotlin/com/avanues/cockpit/webview/JavaScriptInjector.kt`

**Location:** `Common/WebAvanue/src/androidMain/kotlin/com/avanues/webavanue/webview/injection/JavaScriptInjector.kt`

**Key Functions:**
- `executeJavaScript(webView: WebView, javascript: String, callback: ((String) -> Unit)?)`
- `executeVoiceCommand(webView: WebView, command: String, domain: String): Boolean`
- `scrollPage(webView: WebView, deltaY: Int)`
- `scrollToTop(webView: WebView)`
- `scrollToBottom(webView: WebView)`
- `getPageTitle(webView: WebView, callback: (String) -> Unit)`
- `getScrollPosition(webView: WebView, callback: (Pair<Int, Int>) -> Unit)`
- `clickAtCoordinates(webView: WebView, xRatio: Float, yRatio: Float)`
- `fillTextField(webView: WebView, selector: String, value: String)`

**Port Instructions:**
1. Copy entire file from Cockpit
2. Update package name to `com.avanues.webavanue.webview.injection`
3. Integrate with KMP `CommandRegistry` instead of internal map
4. Update imports for VoiceOsLogger

**Integration with CommandRegistry:**
```kotlin
// Replace internal commandMap with:
private val registry = CommandRegistry()

fun registerCommand(domain: String, command: String, javascript: String) {
    registry.register(VoiceCommand(domain, command, javascript))
}

fun executeVoiceCommand(webView: WebView, command: String, domain: String): Boolean {
    val voiceCommand = registry.getCommand(domain, command) ?: return false
    executeJavaScript(webView, voiceCommand.javascript)
    return true
}
```

### 3. WebViewRenderer.kt (androidMain)

**Purpose:** Compose WebView component with state persistence

**Ported from:** `Common/Cockpit/src/androidMain/kotlin/com/avanues/cockpit/webview/WebViewRenderer.kt`

**Location:** `Common/WebAvanue/src/androidMain/kotlin/com/avanues/webavanue/webview/renderer/WebViewRenderer.kt`

**Key Features:**
- Automatic scroll/zoom restoration from WindowState
- JavaScript injection integration
- YouTube detection and configuration
- Loading states with progress indicator
- Error handling with fallback UI

**Port Instructions:**
1. Copy entire file from Cockpit
2. Update package name to `com.avanues.webavanue.webview.renderer`
3. Replace `AppWindow` parameter with simpler data class:

```kotlin
@Composable
fun WebViewRenderer(
    url: String,
    state: WindowState = WindowState.DEFAULT,
    onStateChange: (WindowState) -> Unit,
    onUrlChange: ((String) -> Unit)? = null,
    onTitleChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Same implementation as Cockpit, but use url parameter directly
}
```

4. Use KMP `UrlPatterns.isYouTubeVideo()` instead of WebViewConfig version
5. Integrate with VoiceOSBridge for voice announcements

---

## Platform-Specific Components (iOS)

### 1. WKWebViewRenderer.kt (iosMain)

**Purpose:** iOS WKWebView wrapper with state persistence

**Location:** `Common/WebAvanue/src/iosMain/kotlin/com/avanues/webavanue/webview/renderer/WKWebViewRenderer.kt`

**Key Features:**
- WKWebView configuration with JavaScript enabled
- State restoration using WKWebView API
- JavaScript message handlers for voice commands
- Cookie management via WKHTTPCookieStore

**Implementation Notes:**
- Use `WKWebViewConfiguration` for preset settings
- Use `WKUserScript` for JavaScript injection
- Use `WKScriptMessageHandler` for JS → Kotlin communication
- State persistence via `WKWebView.scrollView.contentOffset` and zoom scale

### 2. VoiceOSBridgeImpl.kt (iosMain)

**Purpose:** iOS VoiceOver integration

**Location:** `Common/WebAvanue/src/iosMain/kotlin/com/avanues/webavanue/webview/voice/VoiceOSBridgeImpl.kt`

**Implementation:**
- Use `AVSpeechSynthesizer` for voice announcements
- Use `SFSpeechRecognizer` for voice input
- Integrate with iOS VoiceOver API for accessibility

---

## Platform-Specific Components (Desktop)

### 1. JavaFXWebViewRenderer.kt (desktopMain)

**Purpose:** Desktop WebView wrapper (JavaFX or JxBrowser)

**Location:** `Common/WebAvanue/src/desktopMain/kotlin/com/avanues/webavanue/webview/renderer/JavaFXWebViewRenderer.kt`

**Options:**
- **JavaFX WebView:** Built-in, basic features
- **JxBrowser:** Commercial, Chromium-based, full features

**Implementation Notes:**
- Use JavaFX `WebView` and `WebEngine` for basic support
- JavaScript execution via `WebEngine.executeScript()`
- State persistence via `WebEngine.getLoadWorker()` callbacks

---

## Voice Command Implementation

### Built-in Commands (All Platforms)

#### Navigation Commands
| Voice Command | JavaScript Action | Platform |
|---------------|-------------------|----------|
| "Scroll down" | `window.scrollBy(0, 300)` | All |
| "Scroll up" | `window.scrollBy(0, -300)` | All |
| "Go to top" | `window.scrollTo(0, 0)` | All |
| "Go to bottom" | `window.scrollTo(0, document.body.scrollHeight)` | All |
| "Back" | Platform navigation API | All |
| "Forward" | Platform navigation API | All |
| "Refresh" | Platform reload API | All |

#### Zoom Commands
| Voice Command | Action | Platform |
|---------------|--------|----------|
| "Zoom in" | `state.zoomIn()` | All |
| "Zoom out" | `state.zoomOut()` | All |
| "Reset zoom" | `state.resetZoom()` | All |
| "Set zoom to 150%" | `state.withZoom(1.5f)` | All |

#### Media Commands (YouTube)
| Voice Command | JavaScript Action | Platform |
|---------------|-------------------|----------|
| "Play" | `document.querySelector('video').play()` | All |
| "Pause" | `document.querySelector('video').pause()` | All |
| "Seek to 2:30" | `document.querySelector('video').currentTime = 150` | All |
| "Fullscreen" | `document.querySelector('video').requestFullscreen()` | All |

#### Custom Site Commands (Domain-Specific)

**GitHub:**
```kotlin
registry.register(VoiceCommand(
    domain = "github.com",
    command = "SIGN IN",
    javascript = "document.querySelector('a[href=\"/login\"]').click();",
    description = "Clicks the sign-in link"
))
```

**Gmail:**
```kotlin
registry.register(VoiceCommand(
    domain = "mail.google.com",
    command = "COMPOSE",
    javascript = "document.querySelector('[gh=\"cm\"]').click();",
    description = "Opens compose new email dialog"
))
```

---

## State Persistence Flow

### Save State (Android Example)

```kotlin
// In WebViewRenderer.kt
DisposableEffect(webView) {
    val view = webView
    if (view != null) {
        // Track scroll changes
        view.viewTreeObserver.addOnScrollChangedListener {
            val newState = currentState.withScroll(view.scrollX, view.scrollY)
            onStateChange(newState)
        }

        // Track zoom changes (if supported)
        // Implementation depends on WebView API
    }

    onDispose {
        webView?.destroy()
    }
}
```

### Restore State (Android Example)

```kotlin
// In WebViewClient.onPageFinished()
override fun onPageFinished(view: WebView?, url: String?) {
    super.onPageFinished(view, url)

    // Restore scroll position
    view?.post {
        view.scrollTo(state.scrollX, state.scrollY)
    }

    // Restore zoom level
    if (state.zoomLevel != 1.0f) {
        view?.post {
            view.setInitialScale((state.zoomLevel * 100).toInt())
        }
    }
}
```

### Persist to Storage (KMP)

```kotlin
// WindowStatePersistence.kt (commonMain)
class WindowStatePersistence(private val storage: KeyValueStorage) {
    fun save(windowId: String, state: WindowState) {
        val json = Json.encodeToString(WindowState.serializer(), state)
        storage.put("window_state_$windowId", json)
    }

    fun load(windowId: String): WindowState? {
        val json = storage.get("window_state_$windowId") ?: return null
        return Json.decodeFromString(WindowState.serializer(), json)
    }
}

expect class KeyValueStorage {
    fun put(key: String, value: String)
    fun get(key: String): String?
}
```

---

## Integration with Existing WebAvanue Code

### Step 1: Create KMP Module

```bash
# Create module structure
mkdir -p Common/WebAvanue/src/{commonMain,androidMain,iosMain,desktopMain}/kotlin

# Create build.gradle.kts
touch Common/WebAvanue/build.gradle.kts
```

### Step 2: Configure build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget()
    jvm("desktop")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.webkit:webkit:1.8.0")
                implementation("androidx.compose.ui:ui:1.5.4")
                implementation("androidx.compose.material3:material3:1.1.2")
            }
        }

        val iosMain by getting {
            dependencies {
                // iOS-specific dependencies
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation("org.openjfx:javafx-web:17")
            }
        }
    }
}
```

### Step 3: Port Files from Cockpit

```bash
# Copy WindowState.kt (no changes needed)
cp Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/core/window/WindowState.kt \
   Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/state/

# Copy Android WebView files
cp Common/Cockpit/src/androidMain/kotlin/com/avanues/cockpit/webview/WebViewConfig.kt \
   Common/WebAvanue/src/androidMain/kotlin/com/avanues/webavanue/webview/config/

cp Common/Cockpit/src/androidMain/kotlin/com/avanues/cockpit/webview/JavaScriptInjector.kt \
   Common/WebAvanue/src/androidMain/kotlin/com/avanues/webavanue/webview/injection/

cp Common/Cockpit/src/androidMain/kotlin/com/avanues/cockpit/webview/WebViewRenderer.kt \
   Common/WebAvanue/src/androidMain/kotlin/com/avanues/webavanue/webview/renderer/
```

### Step 4: Update Package Names

```bash
# Use find/replace to update package names in all copied files
# From: com.avanues.cockpit
# To:   com.avanues.webavanue
```

### Step 5: Integrate with WebAvanue App

```kotlin
// In WebAvanue app module
dependencies {
    implementation(project(":Common:WebAvanue"))
}

// Usage in Activity/Fragment
@Composable
fun WebAvanueScreen() {
    var currentState by remember { mutableStateOf(WindowState.DEFAULT) }

    WebViewRenderer(
        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        state = currentState,
        onStateChange = { currentState = it },
        onTitleChange = { title -> /* Update toolbar */ }
    )
}
```

---

## Testing Strategy

### Unit Tests (commonMain)

```kotlin
// WindowStateTest.kt
class WindowStateTest {
    @Test
    fun zoomIn_increasesZoomByStep() {
        val state = WindowState.DEFAULT
        val newState = state.zoomIn()
        assertEquals(1.25f, newState.zoomLevel)
    }

    @Test
    fun zoomLevel_coercedToValidRange() {
        val state = WindowState.DEFAULT
        val maxZoom = state.withZoom(5.0f)
        assertEquals(WindowState.MAX_ZOOM, maxZoom.zoomLevel)
    }
}

// CommandRegistryTest.kt
class CommandRegistryTest {
    @Test
    fun register_addsCommandToDomain() {
        val registry = CommandRegistry()
        registry.register(VoiceCommand(
            domain = "github.com",
            command = "SIGN IN",
            javascript = "..."
        ))

        val command = registry.getCommand("github.com", "SIGN IN")
        assertNotNull(command)
    }
}
```

### Integration Tests (androidMain)

```kotlin
// WebViewRendererTest.kt (Android Instrumented)
@Test
fun webViewRenderer_restoresScrollPosition() {
    val state = WindowState(scrollX = 0, scrollY = 500)

    composeTestRule.setContent {
        WebViewRenderer(
            url = "https://example.com",
            state = state,
            onStateChange = {}
        )
    }

    // Wait for page load
    composeTestRule.waitForIdle()

    // Verify scroll position restored
    // (Implementation depends on test framework)
}
```

---

## Migration Path

### Phase 1: Setup (Week 1)
- Create Common/WebAvanue KMP module
- Configure build.gradle.kts
- Set up source sets (commonMain, androidMain, iosMain, desktopMain)

### Phase 2: Port Common Code (Week 2)
- Copy WindowState.kt from Cockpit
- Create WebViewConfigPreset, UrlPatterns, CommandRegistry
- Create VoiceOSBridge interface (expect/actual)

### Phase 3: Port Android Code (Week 3)
- Copy WebViewConfig.kt from Cockpit
- Copy JavaScriptInjector.kt from Cockpit
- Copy WebViewRenderer.kt from Cockpit
- Update package names and imports
- Integrate with KMP common code

### Phase 4: Integrate with WebAvanue App (Week 4)
- Add dependency on Common/WebAvanue
- Replace existing WebView usage with WebViewRenderer
- Test state persistence and voice commands

### Phase 5: iOS Implementation (Week 5-6)
- Implement WKWebViewRenderer
- Implement VoiceOSBridgeImpl for iOS
- Test on iOS devices

### Phase 6: Desktop Implementation (Week 7)
- Implement JavaFXWebViewRenderer
- Test on desktop platforms

---

## Success Metrics

### Functional Requirements
- ✅ YouTube videos auto-detect and apply optimized config
- ✅ Voice commands execute JavaScript on web pages
- ✅ Scroll/zoom state persists across window switches
- ✅ Media playback resumes from last position
- ✅ All built-in utilities work on 10+ test sites

### Performance Requirements
- ✅ State save/restore < 50ms
- ✅ JavaScript injection < 100ms
- ✅ No frame drops during scroll tracking
- ✅ Memory usage < 10MB per WebView instance

### Voice Integration Requirements
- ✅ VoiceOS recognizes 20+ core commands
- ✅ Voice announcements are clear and timely
- ✅ Custom commands load from JSON config files
- ✅ 95%+ accuracy on voice command recognition

---

## Security Considerations

### JavaScript Injection
- Only execute JS from registered command registry
- No arbitrary code execution from user input
- Whitelist domains for custom commands
- User confirmation for destructive commands

### Cookie Management
- Respect privacy settings
- HTTPS enforcement for login pages
- Clear cookies on sign-out
- Isolated storage per WebView instance

### Content Security
- Mixed content warnings for HTTP resources
- Safe Browsing enabled by default
- No automatic download execution
- Sandbox JavaScript execution

---

## Documentation Deliverables

1. **API Reference** - KDoc for all public classes and functions
2. **Integration Guide** - How to integrate WebAvanue WebView into apps
3. **Voice Command Reference** - Complete list of built-in and custom commands
4. **Migration Guide** - How to migrate from standard WebView
5. **Troubleshooting Guide** - Common issues and solutions
6. **Example Projects** - Sample apps demonstrating usage

---

## Dependencies

### KMP Common
- `kotlinx-serialization-json:1.6.0`
- `kotlinx-coroutines-core:1.7.3`

### Android
- `androidx.webkit:webkit:1.8.0`
- `androidx.compose.ui:ui:1.5.4`
- `androidx.compose.material3:material3:1.1.2`
- VoiceOSLogger (internal)

### iOS
- iOS SDK 14.0+
- WKWebKit framework

### Desktop
- `org.openjfx:javafx-web:17`

---

## Future Enhancements

### Post-MVP Features
- Custom JavaScript library injection
- Multi-tab WebView management
- Bookmarks and history with voice
- Reading mode with TTS integration
- Offline mode with service workers
- Progressive Web App (PWA) support
- WebRTC support for video calls
- File upload/download with voice
- Print preview and PDF export
- Developer tools integration

---

## Appendix A: File Checklist

### Files to Create (KMP Common)

- [ ] `Common/WebAvanue/build.gradle.kts`
- [ ] `Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/state/WindowState.kt`
- [ ] `Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/config/WebViewConfigPreset.kt`
- [ ] `Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/config/UrlPatterns.kt`
- [ ] `Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/config/LoginUrls.kt`
- [ ] `Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/injection/CommandRegistry.kt`
- [ ] `Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/injection/VoiceCommand.kt`
- [ ] `Common/WebAvanue/src/commonMain/kotlin/com/avanues/webavanue/webview/voice/VoiceOSBridge.kt`

### Files to Port from Cockpit (Android)

- [ ] `WebViewConfig.kt` → `Common/WebAvanue/src/androidMain/.../config/WebViewConfig.kt`
- [ ] `JavaScriptInjector.kt` → `Common/WebAvanue/src/androidMain/.../injection/JavaScriptInjector.kt`
- [ ] `WebViewRenderer.kt` → `Common/WebAvanue/src/androidMain/.../renderer/WebViewRenderer.kt`

### Files to Create (iOS)

- [ ] `Common/WebAvanue/src/iosMain/kotlin/com/avanues/webavanue/webview/renderer/WKWebViewRenderer.kt`
- [ ] `Common/WebAvanue/src/iosMain/kotlin/com/avanues/webavanue/webview/voice/VoiceOSBridgeImpl.kt`

### Files to Create (Desktop)

- [ ] `Common/WebAvanue/src/desktopMain/kotlin/com/avanues/webavanue/webview/renderer/JavaFXWebViewRenderer.kt`

---

## Appendix B: Voice Command Examples

### Complete Voice Command List

**Navigation (20 commands)**
- "Go to [URL]"
- "Search for [query]"
- "Back" / "Go back"
- "Forward" / "Go forward"
- "Refresh" / "Reload"
- "Stop loading"
- "Scroll down" / "Scroll up"
- "Go to top" / "Go to bottom"
- "Page down" / "Page up"
- "Next page" / "Previous page"

**Zoom (5 commands)**
- "Zoom in" / "Zoom out"
- "Reset zoom" / "Default zoom"
- "Set zoom to [percent]"

**Media (10 commands)**
- "Play" / "Pause"
- "Stop"
- "Mute" / "Unmute"
- "Volume up" / "Volume down"
- "Seek to [time]"
- "Skip ahead [seconds]"
- "Skip back [seconds]"
- "Fullscreen" / "Exit fullscreen"

**Interaction (15 commands)**
- "Click [element]"
- "Tap [element]"
- "Fill [field] with [value]"
- "Submit form"
- "Select [option]"
- "Check [checkbox]"
- "Uncheck [checkbox]"
- "Open link [text]"

**Custom Site Commands (50+ examples)**
- GitHub: "Sign in", "Create repo", "Star this", "Fork this", "Clone URL"
- Gmail: "Compose", "Send", "Delete", "Archive", "Mark as read"
- YouTube: "Play", "Pause", "Next video", "Previous video", "Subscribe"
- Twitter: "Tweet", "Like", "Retweet", "Reply", "Follow"
- Amazon: "Add to cart", "Buy now", "Remove from cart"

---

**End of Specification**
