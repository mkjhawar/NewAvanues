# WebAvanue - Voice-Controlled Web Browser

**Version:** 1.0 | **Platform:** Kotlin Multiplatform | **Last Updated:** 2026-01-11

---

## Executive Summary

WebAvanue is a **Kotlin Multiplatform (KMP) voice-controlled web browser** that integrates with VoiceOS for hands-free web navigation.

### Key Capabilities

| Capability | Description |
|------------|-------------|
| **Voice Navigation** | Navigate web pages using voice commands |
| **DOM Scraping** | Extract interactive elements with VUIDs |
| **Cross-Platform** | Android, iOS (Phase 2), Desktop (Phase 2) |
| **Tab Management** | Voice-controlled tab switching |
| **Reader Mode** | Distraction-free reading |
| **Accessibility** | Full accessibility support |

### Statistics

| Metric | Value |
|--------|-------|
| Kotlin Files | 268 |
| Lines of Code | 33,435+ |
| Sub-modules | 3 (universal, coredata, config) |
| Test Cases | 407+ |

---

## Architecture Overview

```
WebAvanue (KMP Browser)
├── universal/               # 95% shared code (33,435 LOC)
│   ├── src/commonMain/     # Shared across Android/iOS/Desktop
│   ├── src/androidMain/    # Android implementations
│   ├── src/iosMain/        # iOS (Phase 2)
│   └── src/desktopMain/    # Desktop (Phase 2)
├── coredata/               # Data persistence layer (4,074 LOC)
│   ├── domain/             # Models & repositories
│   ├── data/               # Repository implementations
│   ├── manager/            # LRU caching managers
│   └── sqldelight/         # Database schemas
└── .ideacode/registries/   # Architecture registries
```

### Core Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    WebAvanue Browser                         │
├─────────────────────────────────────────────────────────────┤
│  User speaks: "Click login button"                           │
│                    │                                         │
│                    ▼                                         │
│         ┌─────────────────────┐                             │
│         │  VoiceOS Integration│                             │
│         │  VoiceOSWebCallback │                             │
│         └──────────┬──────────┘                             │
│                    │                                         │
│                    ▼                                         │
│         ┌─────────────────────┐                             │
│         │  DOMScraperBridge   │                             │
│         │  JavaScript Bridge  │                             │
│         │  (428 lines)        │                             │
│         └──────────┬──────────┘                             │
│                    │                                         │
│     ┌──────────────┼──────────────┐                         │
│     │              │              │                         │
│     ▼              ▼              ▼                         │
│  ┌──────┐    ┌──────────┐   ┌──────────┐                   │
│  │Extract│    │ Generate │   │ Execute  │                   │
│  │Elements│   │  VUIDs   │   │ Commands │                   │
│  └──────┘    └──────────┘   └──────────┘                   │
│                    │                                         │
│                    ▼                                         │
│         ┌─────────────────────┐                             │
│         │VoiceCommandGenerator│                             │
│         │  Fuzzy Matching     │                             │
│         │  (311 lines)        │                             │
│         └──────────┬──────────┘                             │
│                    │                                         │
│                    ▼                                         │
│              Click Element                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Module Structure

```
Modules/WebAvanue/
├── app/                     # Main application module
├── universal/               # KMP shared code
│   └── src/
│       ├── commonMain/kotlin/com/augmentalis/webavanue/
│       │   ├── engine/      # Browser engine abstraction
│       │   ├── voiceos/     # VoiceOS integration
│       │   │   ├── DOMScraperBridge.kt
│       │   │   ├── VoiceCommandGenerator.kt
│       │   │   └── DOMElement.kt
│       │   ├── tabs/        # Tab management
│       │   ├── history/     # Browsing history
│       │   ├── bookmarks/   # Bookmark management
│       │   └── reader/      # Reader mode
│       └── androidMain/     # Android WebView
│
├── coredata/                # Data persistence
│   └── src/commonMain/kotlin/com/augmentalis/webavanue/
│       ├── domain/          # Models
│       │   ├── Tab.kt
│       │   ├── History.kt
│       │   ├── Favorite.kt
│       │   ├── Bookmark.kt
│       │   └── Download.kt
│       ├── data/            # Repositories
│       └── manager/         # LRU managers
│
└── BrowserCoreData/         # Legacy module (deprecated)
```

---

## Class Inventory

### VoiceOS Integration

| Class | Lines | Purpose |
|-------|-------|---------|
| `DOMScraperBridge` | 428 | JavaScript bridge for DOM scraping |
| `VoiceCommandGenerator` | 311 | Voice command matching |
| `DOMElement` | 135 | Scraped element model |
| `VoiceOSWebCallback` | - | Integration callback interface |

### Browser Core

| Class | Purpose |
|-------|---------|
| `WebViewEngine` | Platform abstraction (expect/actual) |
| `TabManager` | Tab management with LRU caching |
| `HistoryManager` | Browsing history with search |
| `BookmarkManager` | Bookmark organization |
| `DownloadManager` | Download progress tracking |

### Data Models

| Model | Purpose |
|-------|---------|
| `Tab` | Browser tab entity |
| `History` | History entry |
| `Favorite` | Quick access favorite |
| `Bookmark` | Organized bookmark |
| `Download` | Download record |
| `Settings` | User preferences |
| `AuthCredentials` | Saved credentials |

---

## VoiceOS Integration

### DOMScraperBridge

The JavaScript bridge that extracts interactive elements from web pages:

```kotlin
class DOMScraperBridge {
    // JavaScript injected into WebView
    private val scraperJS = """
        (function() {
            const elements = [];
            const interactive = document.querySelectorAll(
                'a, button, input, select, textarea, [role="button"], [onclick]'
            );

            interactive.forEach((el, index) => {
                const rect = el.getBoundingClientRect();
                elements.push({
                    vuid: 'web_' + index,
                    tagName: el.tagName,
                    text: el.textContent?.trim(),
                    bounds: { x: rect.x, y: rect.y, width: rect.width, height: rect.height },
                    selector: generateSelector(el),
                    ariaLabel: el.getAttribute('aria-label'),
                    role: el.getAttribute('role')
                });
            });

            return JSON.stringify(elements);
        })();
    """.trimIndent()

    suspend fun scrapeDOM(): List<DOMElement>
    fun executeClick(vuid: String)
    fun executeInput(vuid: String, text: String)
    fun executeScroll(direction: String)
}
```

### VoiceCommandGenerator

Fuzzy matching for voice commands:

```kotlin
class VoiceCommandGenerator {
    // Word-based fuzzy matching (minimum 2 words)
    fun matchCommand(
        utterance: String,
        elements: List<DOMElement>
    ): MatchResult {
        // Generates disambiguation options when multiple matches
        // Returns confidence scores and match rankings
    }

    data class MatchResult(
        val element: DOMElement?,
        val confidence: Float,
        val alternatives: List<DOMElement>
    )
}
```

### VoiceOSWebCallback

```kotlin
interface VoiceOSWebCallback {
    fun onDOMScraped(elements: List<DOMElement>)
    fun onPageLoadStarted(url: String)
    fun onPageLoadFinished(url: String)
    suspend fun executeCommand(command: VoiceCommand): Boolean
}
```

---

## BrowserCoreData Features

### Entity Types (7)

```kotlin
data class Tab(
    val id: String,
    val url: String,
    val title: String,
    val favicon: ByteArray?,
    val isActive: Boolean,
    val createdAt: Long,
    val lastAccessedAt: Long
)

data class History(
    val id: String,
    val url: String,
    val title: String,
    val visitedAt: Long,
    val visitCount: Int
)

data class Bookmark(
    val id: String,
    val url: String,
    val title: String,
    val folderId: String?,
    val position: Int,
    val createdAt: Long
)
```

### LRU Caching

| Manager | Performance |
|---------|-------------|
| `TabManager` | 4x faster retrieval |
| `HistoryManager` | 20x faster search |

### Database Schema (SQLDelight)

```sql
CREATE TABLE tabs (
    id TEXT NOT NULL PRIMARY KEY,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    favicon BLOB,
    is_active INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    last_accessed_at INTEGER NOT NULL
);

CREATE TABLE history (
    id TEXT NOT NULL PRIMARY KEY,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    visited_at INTEGER NOT NULL,
    visit_count INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE bookmarks (
    id TEXT NOT NULL PRIMARY KEY,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    folder_id TEXT,
    position INTEGER NOT NULL,
    created_at INTEGER NOT NULL
);
```

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin Multiplatform |
| UI | Compose Multiplatform |
| Database | SQLDelight 2.0.1 |
| Encryption | SQLCipher 4.5.4 |
| Navigation | Voyager |
| Logging | Napier |
| Async | Kotlin Coroutines |
| Serialization | Kotlinx Serialization |

---

## Security Implementation

### Database Encryption

- Algorithm: AES-256-CBC with PBKDF2-HMAC-SHA512
- Key Storage: Android Keystore (hardware-backed)
- Auto-migration from plaintext to encrypted
- CWE-311 compliance (Missing Encryption fix)

```kotlin
// Encrypted database initialization
val driver = AndroidSqliteDriver(
    schema = WebAvanueDatabase.Schema,
    context = context,
    name = "webavanue.db",
    factory = SupportSQLiteOpenHelper.Factory {
        EncryptedSupportSQLiteOpenHelperFactory(
            passphrase = keyManager.getDatabaseKey()
        )
    }
)
```

---

## Usage Examples

### Basic Browser Setup

```kotlin
val browser = WebAvanueEngine(context)

// Load URL
browser.loadUrl("https://example.com")

// Enable VoiceOS integration
browser.setVoiceOSCallback(object : VoiceOSWebCallback {
    override fun onDOMScraped(elements: List<DOMElement>) {
        // Register elements with VoiceOS
        voiceOS.updateDynamicCommands(elements.map { it.toQuantizedCommand() })
    }

    override suspend fun executeCommand(command: VoiceCommand): Boolean {
        return browser.executeElement(command.targetVuid)
    }
})
```

### Voice Command Execution

```kotlin
// User says: "Click login button"
val utterance = "click login button"

// Match against scraped elements
val result = voiceCommandGenerator.matchCommand(utterance, scrapedElements)

if (result.confidence > 0.7f) {
    domScraperBridge.executeClick(result.element!!.vuid)
} else if (result.alternatives.isNotEmpty()) {
    // Show disambiguation UI
    showAlternatives(result.alternatives)
}
```

### Tab Management

```kotlin
val tabManager = TabManager(database)

// Create tab
val tab = tabManager.createTab("https://example.com")

// Switch tabs
tabManager.setActiveTab(tabId)

// Get all tabs
val tabs = tabManager.getAllTabs()

// Close tab
tabManager.closeTab(tabId)
```

---

## Browser Features

### Core Features

- Tab management with LRU caching
- Browsing history with full-text search
- Favorites and bookmarks with folder organization
- Download management with progress tracking
- Reader mode for distraction-free reading
- Screenshot capture
- Private browsing mode

### Voice Features

- Voice navigation ("go back", "scroll down")
- Element interaction ("click [element]", "type [text]")
- Tab control ("new tab", "close tab", "switch to tab 2")
- Page actions ("refresh", "stop", "go home")

### Accessibility

- Full screen reader support
- Voice control via VoiceOS
- High contrast mode
- Adjustable text size

---

## Platform Support

| Platform | Status | Implementation |
|----------|--------|----------------|
| Android | Production | WebView |
| iOS | Phase 2 | WKWebView |
| Desktop | Phase 2 | JCEF |

---

## Performance

### Caching Performance

| Operation | Without Cache | With Cache | Improvement |
|-----------|---------------|------------|-------------|
| Tab retrieval | 20ms | 5ms | 4x |
| History search | 200ms | 10ms | 20x |
| Bookmark load | 15ms | 3ms | 5x |

### Memory Usage

| Component | Size |
|-----------|------|
| Tab (avg) | ~50KB |
| History (1000 entries) | ~2MB |
| Bookmarks (500) | ~500KB |
| DOM cache | ~1MB |

---

## Related Documentation

- [VoiceOSCoreNG](../VoiceOSCoreNG/README.md)
- [VoiceOS App](../VoiceOS/README.md)
- [Common Libraries](../Common/README.md)

---

**Author:** WebAvanue Team | **Last Updated:** 2026-01-11
