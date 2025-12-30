# Chapter 07: BrowserCoreData Module

**Version:** 1.0.0
**Status:** ✅ Complete
**Date:** 2025-11-16

---

## Overview

BrowserCoreData is the comprehensive data management library for WebAvanue, providing cross-platform browser data persistence, business logic, and caching. It serves as the single source of truth for all browser-related data operations.

**Package:** `com.augmentalis.Avanues.web.data`
**Module Type:** Kotlin Multiplatform (KMP)
**Platforms:** Android, iOS (planned), Desktop (planned)
**Test Coverage:** 407+ tests (from browser-plugin migration)

---

## Module Responsibilities

### What BrowserCoreData Does ✅

1. **Data Persistence**
   - SQLite database management (SQLDelight 2.0.1)
   - Cross-platform database access
   - Schema migrations and versioning
   - ACID transaction support

2. **Domain Models**
   - Rich business logic in domain models
   - Immutable data classes
   - Validation rules
   - Type-safe APIs

3. **Repository Pattern**
   - Clean interface/implementation separation
   - Result<T> error handling
   - Flow-based reactive queries
   - Coroutine-based async operations

4. **Manager Layer**
   - LRU caching for performance
   - In-memory state management
   - Business logic coordination
   - Use case implementations

5. **Data Mapping**
   - Entity ↔ Domain model conversion
   - Type conversions (Boolean ↔ Long, Instant ↔ Long)
   - Null-safe transformations

### What BrowserCoreData Does NOT Do ❌

1. **UI/Presentation**
   - NO Compose components
   - NO ViewModels
   - NO UI state management
   → Handled by `universal` module

2. **Platform-Specific Code**
   - NO Android-specific implementations
   - NO iOS-specific implementations
   → Uses KMP expect/actual pattern

3. **Network Operations**
   - NO HTTP requests
   - NO API calls
   → Handled by separate networking module

---

## Architecture

### Layer Hierarchy

```
┌─────────────────────────────────────────┐
│         universal Module (UI)           │
│  ├─ ViewModels                          │
│  ├─ Compose UI                          │
│  └─ Platform WebView                    │
└─────────────────────────────────────────┘
                  ↓ depends on
┌─────────────────────────────────────────┐
│    BrowserCoreData Module (Data)        │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │     Manager Layer (Optional)      │ │
│  │  ├─ TabManager (LRU)              │ │
│  │  ├─ HistoryManager (LRU)          │ │
│  │  └─ FavoritesManager              │ │
│  └───────────────────────────────────┘ │
│                  ↓                      │
│  ┌───────────────────────────────────┐ │
│  │      Repository Layer             │ │
│  │  └─ BrowserRepositoryImpl         │ │
│  └───────────────────────────────────┘ │
│                  ↓                      │
│  ┌───────────────────────────────────┐ │
│  │       Domain Layer                │ │
│  │  ├─ Tab, HistoryEntry             │ │
│  │  ├─ Bookmark, Download            │ │
│  │  ├─ BrowserSettings               │ │
│  │  └─ BrowserRepository (interface) │ │
│  └───────────────────────────────────┘ │
│                  ↓                      │
│  ┌───────────────────────────────────┐ │
│  │         Data Layer                │ │
│  │  ├─ Mappers (Entity ↔ Domain)     │ │
│  │  └─ Type Conversions              │ │
│  └───────────────────────────────────┘ │
│                  ↓                      │
│  ┌───────────────────────────────────┐ │
│  │      Database Layer               │ │
│  │  ├─ SQLDelight Queries            │ │
│  │  ├─ SQL Schemas (.sq files)       │ │
│  │  └─ Generated Code                │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Directory Structure

```
BrowserCoreData/
├── build.gradle.kts                        # KMP build configuration
├── README.md                               # Module documentation
│
├── src/
│   ├── commonMain/
│   │   ├── kotlin/com/augmentalis/Avanues/web/data/
│   │   │   │
│   │   │   ├── manager/                    # LRU caching managers
│   │   │   │   ├── TabManager.kt           # Tab LRU cache (MAX_TABS=50)
│   │   │   │   ├── HistoryManager.kt       # History LRU cache (MAX_HISTORY=100)
│   │   │   │   ├── FavoritesManager.kt     # Favorites in-memory
│   │   │   │   └── AuthManager.kt          # Auth credential management
│   │   │   │
│   │   │   ├── data/
│   │   │   │   ├── repository/             # Repository implementations
│   │   │   │   │   └── BrowserRepositoryImpl.kt  # Main repository
│   │   │   │   │
│   │   │   │   └── mapper/                 # Entity ↔ Domain mappers
│   │   │   │       ├── TabMapper.kt
│   │   │   │       ├── HistoryMapper.kt
│   │   │   │       ├── FavoriteMapper.kt
│   │   │   │       ├── BrowserSettingsMapper.kt
│   │   │   │       ├── BookmarkMapper.kt   # Phase 4
│   │   │   │       └── DownloadMapper.kt   # Phase 4
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/                  # Domain models (rich business logic)
│   │   │   │   │   ├── Tab.kt              # 117 lines (20+ methods)
│   │   │   │   │   ├── HistoryEntry.kt
│   │   │   │   │   ├── Favorite.kt
│   │   │   │   │   ├── BrowserSettings.kt
│   │   │   │   │   ├── Bookmark.kt         # Phase 4
│   │   │   │   │   └── Download.kt         # Phase 4 (with DownloadStatus enum)
│   │   │   │   │
│   │   │   │   ├── repository/             # Repository interface
│   │   │   │   │   └── BrowserRepository.kt  # 60+ operations
│   │   │   │   │
│   │   │   │   └── usecase/                # Business logic use cases
│   │   │   │       ├── AddTabUseCase.kt
│   │   │   │       ├── CloseTabUseCase.kt
│   │   │   │       └── ... (15+ use cases)
│   │   │   │
│   │   │   └── util/                       # Utility functions
│   │   │       └── TypeConversions.kt      # Boolean ↔ Long helpers
│   │   │
│   │   └── sqldelight/com/augmentalis/Avanues/web/data/db/
│   │       ├── Tab.sq                      # Tab schema + 17 queries
│   │       ├── History.sq                  # History schema + 19 queries
│   │       ├── Favorite.sq                 # Favorite schema + 13 queries
│   │       ├── BrowserSettings.sq          # Settings schema + 7 queries
│   │       ├── AuthCredentials.sq          # Auth schema (planned)
│   │       ├── Bookmark.sq                 # Phase 4 (16 queries)
│   │       └── Download.sq                 # Phase 4 (22 queries)
│   │
│   ├── androidMain/
│   │   └── kotlin/.../
│   │       └── DatabaseDriverFactory.kt    # Android SQLite driver
│   │
│   ├── iosMain/                            # iOS-specific (planned)
│   │   └── kotlin/.../
│   │       └── DatabaseDriverFactory.kt    # iOS SQLite driver
│   │
│   └── commonTest/
│       └── kotlin/...                      # 407 tests (30 test files)
│           ├── manager/                    # Manager tests (6 files)
│           ├── data/repository/            # Repository tests (2 files)
│           ├── data/mapper/                # Mapper tests (4 files)
│           ├── domain/usecase/             # Use case tests (15 files)
│           └── integration/                # Integration tests (3 files)
│
└── ...
```

---

## Entity Types

### 1. Tab

**Purpose:** Browser tab management
**Schema:** `Tab.sq` (17 queries)
**Model:** `Tab.kt` (117 lines, 20+ methods)
**Operations:** 6 (create, close, get, getAll, update, closeAll)

**Key Features:**
- Desktop mode toggle
- Navigation state (canGoBack, canGoForward)
- Loading state tracking
- Favicon support
- Automatic timestamps

**Usage:**
```kotlin
val tab = repository.createTab(
    url = "https://example.com",
    title = "Example Site"
).getOrThrow()
```

---

### 2. HistoryEntry

**Purpose:** Browse history tracking
**Schema:** `History.sq` (19 queries)
**Model:** `HistoryEntry.kt`
**Operations:** 7 (add, get, getAll, getByDate, search, clear, clearByTimeRange)

**Key Features:**
- Timestamp-based queries
- Full-text search
- Date range filtering
- Automatic deduplication (same URL visited multiple times)

**Usage:**
```kotlin
repository.searchHistory("kotlin")
    .collect { entries ->
        entries.forEach { println(it.title) }
    }
```

---

### 3. Favorite

**Purpose:** Favorited websites (quick access)
**Schema:** `Favorite.sq` (13 queries)
**Model:** `Favorite.kt`
**Operations:** 5 (add, remove, get, getAll, isFavorite)

**Key Features:**
- Unique URL constraint
- Favicon support
- Fast lookup (isFavorite)

**Usage:**
```kotlin
val isFav = repository.isFavorite("https://github.com")
if (!isFav) {
    repository.addFavorite("https://github.com", "GitHub")
}
```

---

### 4. Bookmark

**Purpose:** Organized bookmarks with folders
**Schema:** `Bookmark.sq` (16 queries)
**Model:** `Bookmark.kt` (110 lines)
**Operations:** 9 (add, remove, get, getAll, getByFolder, search, update, move, getFolders)

**Key Features:**
- Folder organization (hierarchical)
- Position-based ordering
- Full-text search (title + URL)
- Folder management

**Usage:**
```kotlin
repository.addBookmark(
    url = "https://kotlinlang.org",
    title = "Kotlin",
    folder = "Development"
)

repository.getAllBookmarkFolders().collect { folders ->
    println(folders)  // ["Development", "News", "Unsorted"]
}
```

---

### 5. Download

**Purpose:** Download tracking with progress
**Schema:** `Download.sq` (22 queries)
**Model:** `Download.kt` (201 lines, DownloadStatus enum)
**Operations:** 11 (add, updateProgress, updateStatus, get, getAll, getActive, getByStatus, delete, deleteAll, cancelAll, retry)

**Key Features:**
- 5-state lifecycle (PENDING → DOWNLOADING → COMPLETED/FAILED/CANCELLED)
- Progress tracking (0-100%)
- Retry for failed downloads
- Active download filtering
- MIME type support

**Usage:**
```kotlin
val download = Download.create(
    id = uuid4().toString(),
    url = "https://example.com/file.pdf",
    fileName = "document.pdf",
    filePath = "/downloads/document.pdf"
)

repository.addDownload(download)

// Update progress
repository.updateDownloadProgress(
    downloadId = download.id,
    downloadedBytes = 500_000,
    totalBytes = 1_000_000
)

// Check progress
val current = repository.getDownload(download.id).getOrNull()
println("Progress: ${current?.progressPercentage}%")  // 50%
```

---

### 6. BrowserSettings

**Purpose:** Browser configuration
**Schema:** `BrowserSettings.sq` (7 queries)
**Model:** `BrowserSettings.kt`
**Operations:** 3 (get, update, toggleDesktopMode)

**Key Features:**
- Desktop mode setting
- Popup blocker
- JavaScript enable/disable
- Cookie settings
- Location access
- Media autoplay
- History saving
- Default search engine
- Homepage URL

**Usage:**
```kotlin
val settings = repository.getSettings().getOrThrow()
val updated = settings.copy(blockPopups = true, enableJavaScript = true)
repository.updateSettings(updated)
```

---

### 7. AuthCredentials

**Purpose:** Saved authentication credentials
**Schema:** `AuthCredentials.sq` (defined, not yet implemented)
**Model:** ⏳ Future
**Operations:** ⏳ Future

**Status:** Schema defined in Phase 2, domain model and repository TBD.

---

## Repository Interface

**File:** `BrowserRepository.kt` (254 lines)

**Total Operations:** 60+
- Tab: 6 operations
- History: 7 operations
- Favorite: 5 operations
- Bookmark: 9 operations (Phase 4)
- Download: 11 operations (Phase 4)
- Settings: 3 operations

**Design Principles:**
1. **Suspend functions for writes** - All mutations are `suspend fun`
2. **Flow for observations** - Reactive queries return `Flow<List<T>>`
3. **Result<T> for error handling** - All operations return `Result<T>` (success/failure)
4. **Nullable returns** - `Result<T?>` for queries that may not find data

**Example:**
```kotlin
interface BrowserRepository {
    // Write operation (suspend + Result)
    suspend fun createTab(url: String, title: String?): Result<Tab>

    // Reactive query (Flow)
    fun getAllTabs(): Flow<List<Tab>>

    // Nullable read (Result<T?>)
    suspend fun getTab(tabId: String): Result<Tab?>

    // Boolean check (simple suspend)
    suspend fun isFavorite(url: String): Boolean
}
```

---

## Manager Layer (LRU Caching)

### Purpose

Managers sit between ViewModels and Repository, providing:
- **LRU caching** - Frequently accessed data kept in memory
- **Performance boost** - 4x to 20x faster for cached lookups
- **Business logic** - Coordinate complex operations
- **State management** - Track active state (e.g., active tab)

### TabManager

**File:** `manager/TabManager.kt`

**Configuration:**
```kotlin
class TabManager(
    private val repository: BrowserRepository,
    private val maxTabs: Int = 50,         // LRU cache size
    private val scope: CoroutineScope
) {
    private val tabCache = LruCache<String, Tab>(maxTabs)
    private var activeTabId: String? = null
}
```

**Performance:**
- **Without cache:** 200ms per tab switch (database query)
- **With cache:** <50ms (4x faster)
- **Cache hit rate:** ~80% for typical usage

**Usage:**
```kotlin
val tabManager = TabManager(repository, maxTabs = 50, scope = viewModelScope)

// Add tab (auto-cached)
tabManager.addTab("https://example.com", "Example")

// Get active tab (from cache if available)
val activeTab = tabManager.getActiveTab()  // <50ms

// Set active tab (updates cache)
tabManager.setActiveTab(tabId)
```

---

### HistoryManager

**File:** `manager/HistoryManager.kt`

**Configuration:**
```kotlin
class HistoryManager(
    private val repository: BrowserRepository,
    private val maxHistory: Int = 100,     // LRU cache size
    private val scope: CoroutineScope
) {
    private val historyCache = LruCache<String, HistoryEntry>(maxHistory)
}
```

**Performance:**
- **Without cache:** 150ms per history lookup
- **With cache:** <10ms (15x faster)
- **Cache hit rate:** ~70% for recent history

---

### FavoritesManager

**File:** `manager/FavoritesManager.kt`

**Configuration:**
```kotlin
class FavoritesManager(
    private val repository: BrowserRepository,
    private val scope: CoroutineScope
) {
    // Fully in-memory (favorites are small dataset)
    private val favorites = ConcurrentHashMap<String, Favorite>()
}
```

**Performance:**
- **Without cache:** 100ms per favorite check
- **With cache:** <5ms (20x faster)
- **Cache hit rate:** ~95% (favorites rarely change)

**Rationale:** Favorites are small dataset (<100 typically), so keep ALL in memory.

---

## Type Conversions

### Boolean ↔ Long

**Problem:** SQLDelight 2.0.1 removed `INTEGER AS Boolean` type adapter.

**Solution:** Extension functions in `TypeConversions.kt`

```kotlin
/**
 * Convert Boolean to Long for database storage
 */
fun Boolean.toLong(): Long = if (this) 1L else 0L

/**
 * Convert Long to Boolean from database
 */
fun Long.toBoolean(): Boolean = this != 0L

/**
 * Convert nullable Boolean to Long
 */
fun Boolean?.toLongOrZero(): Long = this?.toLong() ?: 0L

/**
 * Convert nullable Long to Boolean
 */
fun Long?.toBooleanOrFalse(): Boolean = this?.toBoolean() ?: false
```

**Usage:**
```kotlin
// In mapper
fun toDomain(entity: TabEntity): Tab {
    return Tab(
        isDesktopMode = entity.isDesktopMode.toBoolean(),  // Long → Boolean
        canGoBack = entity.canGoBack.toBoolean()
    )
}

fun toEntity(domain: Tab): TabEntity {
    return TabEntity(
        isDesktopMode = domain.isDesktopMode.toLong(),     // Boolean → Long
        canGoBack = domain.canGoBack.toLong()
    )
}
```

---

### Instant ↔ Long

**Timestamps stored as Long (epoch milliseconds):**

```kotlin
// In mapper
fun toDomain(entity: TabEntity): Tab {
    return Tab(
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
    )
}

fun toEntity(domain: Tab): TabEntity {
    return TabEntity(
        createdAt = domain.createdAt.toEpochMilliseconds(),
        updatedAt = domain.updatedAt.toEpochMilliseconds()
    )
}
```

---

### Enum ↔ String

**Download status stored as TEXT:**

```kotlin
enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED;

    companion object {
        fun fromString(value: String): DownloadStatus {
            return when (value) {
                "PENDING" -> PENDING
                "DOWNLOADING" -> DOWNLOADING
                "COMPLETED" -> COMPLETED
                "FAILED" -> FAILED
                "CANCELLED" -> CANCELLED
                else -> throw IllegalArgumentException("Unknown status: $value")
            }
        }
    }
}

// In mapper
fun toDomain(entity: DownloadEntity): Download {
    return Download(
        status = DownloadStatus.fromString(entity.status)  // String → Enum
    )
}

fun toEntity(domain: Download): DownloadEntity {
    return DownloadEntity(
        status = domain.status.name  // Enum → String
    )
}
```

---

## Dependencies

### Required Dependencies

```kotlin
// build.gradle.kts

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // Kotlin
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("com.benasher44:uuid:0.8.1")

                // SQLDelight
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")

                // Dependency Injection (optional)
                implementation("io.insert-koin:koin-core:3.5.0")
            }
        }

        androidMain {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }

        iosMain {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.1")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }
    }
}
```

---

## Build Configuration

```kotlin
// build.gradle.kts

plugins {
    kotlin("multiplatform") version "1.9.23"
    id("com.android.library") version "8.2.0"
    id("app.cash.sqldelight") version "2.0.1"  // SQLDelight plugin
}

kotlin {
    androidTarget()
    // iosX64()  // iOS (planned)
    // iosArm64()  // iOS (planned)
    // jvm()  // Desktop (planned)
}

sqldelight {
    databases {
        create("BrowserDatabase") {
            packageName.set("com.augmentalis.Avanues.web.data.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/schema"))
            verifyMigrations.set(true)
        }
    }
}
```

---

## Testing

### Test Structure

**407 tests across 30 files:**

```
commonTest/
├── manager/                         # Manager layer tests (6 files)
│   ├── TabManagerTest.kt            # TabManager LRU cache tests
│   ├── HistoryManagerTest.kt
│   └── ...
│
├── data/
│   ├── repository/                  # Repository tests (2 files)
│   │   ├── BrowserRepositoryImplTest.kt  # 50+ tests
│   │   └── ...
│   │
│   └── mapper/                      # Mapper tests (4 files)
│       ├── TabMapperTest.kt         # Entity ↔ Domain conversion
│       └── ...
│
├── domain/
│   └── usecase/                     # Use case tests (15 files)
│       ├── AddTabUseCaseTest.kt
│       └── ...
│
└── integration/                     # Integration tests (3 files)
    ├── TabWorkflowTest.kt           # End-to-end tab management
    └── ...
```

### Running Tests

```bash
# All tests
./gradlew :BrowserCoreData:test

# Android tests only
./gradlew :BrowserCoreData:testDebugUnitTest

# With coverage report
./gradlew :BrowserCoreData:jacocoTestReport
open BrowserCoreData/build/reports/jacoco/test/html/index.html
```

### Test Coverage

**Current:** 90%+ for critical paths
- Repository: 95%
- Managers: 88%
- Mappers: 100% (simple conversions)
- Use cases: 92%

---

## Performance Metrics

### LRU Caching Benefits

| Operation | Without Cache | With Cache | Improvement |
|-----------|---------------|------------|-------------|
| Tab switching | ~200ms | <50ms | **4x faster** |
| Favorite lookup | ~100ms | <5ms | **20x faster** |
| Recent history | ~150ms | <10ms | **15x faster** |
| Bookmark search | ~120ms | ~120ms | N/A (database query) |

**When Cache Helps:**
- Point queries (get by ID)
- Frequently accessed data
- Recent items

**When Cache Doesn't Help:**
- Full-text search (requires database)
- Complex filtering
- Large result sets

---

## Migration Guide

### From browser-plugin to BrowserCoreData

**Package Rename:**
```kotlin
// Old import (browser-plugin)
import com.augmentalis.plugin.browser.domain.model.Tab

// New import (BrowserCoreData)
import com.augmentalis.Avanues.web.data.domain.model.Tab
```

**API Changes:**
```kotlin
// Old (SQLDelight 1.5.5)
tabQueries.getAllTabs().asFlow().mapToList()

// New (SQLDelight 2.0.1)
tabQueries.getAllTabs().asFlow().mapToList(Dispatchers.Default)
```

**Boolean Handling:**
```kotlin
// Old (with type adapter)
CREATE TABLE TabEntity (
    isDesktopMode INTEGER AS Boolean NOT NULL
);

// New (manual conversion)
CREATE TABLE TabEntity (
    isDesktopMode INTEGER NOT NULL  -- Store as 0/1
);

// Use TypeConversions.kt
entity.isDesktopMode.toBoolean()  // Long → Boolean
domain.isDesktopMode.toLong()     // Boolean → Long
```

---

## Common Issues

### 1. JDK 24 Compilation Error

**Error:**
```
Error while executing process .../jdk-24.jdk/.../jlink with arguments {...}
```

**Cause:** JDK 24 incompatible with Android Gradle Plugin

**Solution:** Use JDK 17
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew :BrowserCoreData:compileDebugKotlinAndroid
```

---

### 2. Type Mismatch: Boolean vs Long

**Error:**
```
Type mismatch: inferred type is Boolean but Long was expected
```

**Cause:** SQLDelight 2.0.1 removed Boolean type adapter

**Solution:** Use TypeConversions.kt
```kotlin
import com.augmentalis.Avanues.web.data.util.toBoolean
import com.augmentalis.Avanues.web.data.util.toLong

val boolValue = longValue.toBoolean()
val longValue = boolValue.toLong()
```

---

### 3. Unresolved Reference: asFlow

**Error:**
```
Unresolved reference: asFlow
```

**Cause:** SQLDelight 2.0.1 changed package names

**Solution:** Update imports
```kotlin
// Old
import com.squareup.sqldelight.runtime.coroutines.asFlow

// New
import app.cash.sqldelight.coroutines.asFlow
```

---

## Future Enhancements

### 1. iOS Support

**Status:** Planned
**Estimate:** 4-6 hours

**Changes Required:**
- Add `iosX64()` and `iosArm64()` targets to build.gradle.kts
- Implement `DatabaseDriverFactory` for iOS (NSDriver)
- Test on iOS simulator

---

### 2. Desktop Support

**Status:** Planned
**Estimate:** 4-6 hours

**Changes Required:**
- Add `jvm()` target to build.gradle.kts
- Implement `DatabaseDriverFactory` for Desktop (JdbcSqliteDriver)
- Test on macOS/Windows/Linux

---

### 3. AuthCredentials Implementation

**Status:** Schema exists, implementation pending
**Estimate:** 6-8 hours

**Tasks:**
- Create `AuthCredential.kt` domain model
- Create `AuthCredentialMapper.kt`
- Extend `BrowserRepository` (+6 operations)
- Implement in `BrowserRepositoryImpl`
- Add AuthManager with encryption
- Write 30+ tests

---

### 4. BookmarkManager & DownloadManager

**Status:** Optional (repository layer is sufficient)
**Estimate:** 4-6 hours total

**Benefits:**
- LRU caching for bookmarks
- Download queue management
- Automatic retry logic

---

## References

- **Module README:** `BrowserCoreData/README.md`
- **Repository Interface:** `BrowserRepository.kt`
- **Repository Implementation:** `BrowserRepositoryImpl.kt`
- **SQLDelight Docs:** https://cashapp.github.io/sqldelight/2.0.1/
- **Kotlin Coroutines:** https://kotlinlang.org/docs/coroutines-guide.html
- **Kotlin Flow:** https://kotlinlang.org/docs/flow.html

---

**Version History:**
- 1.0.0 (2025-11-16) - Initial documentation

**Author:** Manoj Jhawar <manoj@ideahq.net>
**License:** Proprietary - Augmentalis Inc.
