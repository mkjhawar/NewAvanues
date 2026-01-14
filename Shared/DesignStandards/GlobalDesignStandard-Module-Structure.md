# Global Design Standard: Module Structure

**Version:** 1.0.0
**Created:** 2025-11-10
**Last Updated:** 2025-11-10
**Status:** Living Document
**Scope:** All Avanues Ecosystem Modules

---

## Purpose

This standard defines the **canonical structure for all modules** in the Avanues ecosystem. Every module must follow this structure to ensure consistency, maintainability, and ease of navigation.

---

## Core Principles

1. **Kotlin Multiplatform (KMP)**: All modules support multiple platforms
2. **Clean Architecture**: Separation of data, domain, and presentation layers
3. **Platform-Specific Implementations**: Use expect/actual pattern
4. **Self-Documenting**: Clear naming and organization
5. **Testable**: Each layer has corresponding tests

---

## Standard Module Structure

```
module-name/
├── src/
│   ├── commonMain/              # Shared cross-platform code
│   │   └── kotlin/
│   │       └── com/augmentalis/[module]/
│   │           ├── domain/           # Business logic (platform-agnostic)
│   │           │   ├── model/       # Domain models (data classes)
│   │           │   ├── repository/  # Repository interfaces
│   │           │   └── usecase/     # Use cases (business operations)
│   │           ├── data/            # Data layer interfaces
│   │           │   ├── repository/  # Repository implementations (expect)
│   │           │   └── mapper/      # Entity ↔ Domain mapping
│   │           ├── presentation/    # UI layer (if Compose Multiplatform)
│   │           │   ├── viewmodel/  # Shared ViewModels
│   │           │   └── state/      # UI state models
│   │           └── core/            # Core types and utilities
│   │               ├── Types.kt    # Common types/enums
│   │               └── Utils.kt    # Helper functions
│   │
│   ├── androidMain/             # Android-specific implementations
│   │   └── kotlin/
│   │       └── com/augmentalis/[module]/
│   │           ├── data/
│   │           │   ├── local/      # Room database, SharedPrefs
│   │           │   ├── remote/     # Android HTTP client
│   │           │   └── repository/ # Android repository (actual)
│   │           ├── platform/       # Android-specific utilities
│   │           │   └── Context.kt  # Platform dependencies
│   │           └── ui/             # Android Compose UI (if needed)
│   │               └── components/ # Android-only components
│   │
│   ├── iosMain/                 # iOS-specific implementations
│   │   └── kotlin/
│   │       └── com/augmentalis/[module]/
│   │           ├── data/
│   │           │   ├── local/      # Core Data, UserDefaults
│   │           │   └── repository/ # iOS repository (actual)
│   │           ├── platform/       # iOS-specific utilities
│   │           └── ui/             # SwiftUI bridge (if needed)
│   │
│   ├── jvmMain/                 # Desktop (macOS, Windows, Linux)
│   │   └── kotlin/
│   │       └── com/augmentalis/[module]/
│   │           ├── data/
│   │           │   └── repository/ # Desktop repository
│   │           └── platform/
│   │
│   ├── commonTest/              # Shared tests
│   │   └── kotlin/
│   │       └── com/augmentalis/[module]/
│   │           ├── domain/        # Domain logic tests
│   │           └── data/          # Data layer tests (with mocks)
│   │
│   ├── androidUnitTest/         # Android unit tests
│   │   └── kotlin/
│   │       └── com/augmentalis/[module]/
│   │           └── data/          # Android-specific data tests
│   │
│   └── androidInstrumentedTest/ # Android instrumentation tests
│       └── kotlin/
│           └── com/augmentalis/[module]/
│               └── ui/            # UI tests with Compose
│
├── docs/                        # Module documentation
│   ├── README.md               # Module overview
│   ├── ARCHITECTURE.md         # Architecture decisions
│   ├── API.md                  # Public API documentation
│   └── MIGRATION.md            # Migration guides
│
├── build.gradle.kts            # Gradle build configuration
└── GlobalDesignStandard.md     # Module-specific design decisions
```

---

## Layer Responsibilities

### Domain Layer (commonMain/domain/)

**Purpose**: Platform-agnostic business logic

**Contains:**
- **Models**: Pure data classes with no platform dependencies
- **Repository Interfaces**: Define data access contracts
- **Use Cases**: Business operations (e.g., CreateBookmark, DeleteTab)

**Rules:**
- ✅ NO platform-specific code (Android SDK, iOS, etc.)
- ✅ NO external frameworks (Room, Realm, etc.)
- ✅ Pure Kotlin, fully testable
- ✅ Must compile to all targets

**Example:**
```kotlin
// commonMain/domain/model/Bookmark.kt
package com.augmentalis.browser.domain.model

data class Bookmark(
    val id: String,
    val url: String,
    val title: String,
    val folder: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// commonMain/domain/repository/BookmarkRepository.kt
interface BookmarkRepository {
    suspend fun getAllBookmarks(): List<Bookmark>
    suspend fun addBookmark(bookmark: Bookmark): Result<Unit>
    suspend fun deleteBookmark(id: String): Result<Unit>
}

// commonMain/domain/usecase/AddBookmark.kt
class AddBookmarkUseCase(
    private val repository: BookmarkRepository
) {
    suspend operator fun invoke(url: String, title: String): Result<Bookmark> {
        val bookmark = Bookmark(
            id = generateId(),
            url = url,
            title = title
        )
        return repository.addBookmark(bookmark).map { bookmark }
    }
}
```

---

### Data Layer (platformMain/data/)

**Purpose**: Platform-specific data implementations

**Contains:**
- **Local Data Sources**: Room (Android), Core Data (iOS), SQLite (Desktop)
- **Remote Data Sources**: HTTP clients, API services
- **Repository Implementations**: Actual implementations using expect/actual
- **Mappers**: Convert between database entities and domain models

**Rules:**
- ✅ Can use platform-specific frameworks
- ✅ Must implement domain repository interfaces
- ✅ Should handle errors and return Result<T>
- ✅ Must map entities to domain models

**Example:**
```kotlin
// commonMain/data/repository/BookmarkRepository.kt (expect)
expect class BookmarkRepositoryImpl() : BookmarkRepository

// androidMain/data/local/BookmarkEntity.kt
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val folder: String?,
    val createdAt: Long
)

// androidMain/data/local/BookmarkDao.kt
@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAll(): List<BookmarkEntity>

    @Insert
    suspend fun insert(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)
}

// androidMain/data/repository/BookmarkRepositoryImpl.kt (actual)
actual class BookmarkRepositoryImpl(
    private val dao: BookmarkDao
) : BookmarkRepository {
    override suspend fun getAllBookmarks(): List<Bookmark> {
        return withContext(Dispatchers.IO) {
            dao.getAll().map { it.toDomain() }
        }
    }

    override suspend fun addBookmark(bookmark: Bookmark): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dao.insert(bookmark.toEntity())
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

// androidMain/data/mapper/BookmarkMapper.kt
fun BookmarkEntity.toDomain() = Bookmark(
    id = id,
    url = url,
    title = title,
    folder = folder,
    createdAt = createdAt
)

fun Bookmark.toEntity() = BookmarkEntity(
    id = id,
    url = url,
    title = title,
    folder = folder,
    createdAt = createdAt
)
```

---

### Presentation Layer (commonMain/presentation/)

**Purpose**: Shared UI logic (ViewModels, State)

**Contains:**
- **ViewModels**: Shared business logic for UI
- **UI State**: Immutable data classes representing UI
- **UI Events**: User actions and side effects

**Rules:**
- ✅ Platform-agnostic (uses Kotlin only)
- ✅ Should NOT contain Compose code (that's in UI layer)
- ✅ Uses coroutines for async operations
- ✅ Exposes StateFlow/SharedFlow for reactive updates

**Example:**
```kotlin
// commonMain/presentation/state/BrowserUiState.kt
data class BrowserUiState(
    val tabs: List<Tab> = emptyList(),
    val activeTab: Tab? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// commonMain/presentation/viewmodel/BrowserViewModel.kt
class BrowserViewModel(
    private val addBookmarkUseCase: AddBookmarkUseCase,
    private val getAllBookmarksUseCase: GetAllBookmarksUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BrowserEvent>()
    val events: SharedFlow<BrowserEvent> = _events.asSharedFlow()

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = addBookmarkUseCase(url, title)

            result.onSuccess { bookmark ->
                _uiState.update {
                    it.copy(
                        bookmarks = it.bookmarks + bookmark,
                        isLoading = false
                    )
                }
                _events.emit(BrowserEvent.BookmarkAdded(bookmark))
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message, isLoading = false)
                }
            }
        }
    }

    fun loadBookmarks() {
        viewModelScope.launch {
            val bookmarks = getAllBookmarksUseCase()
            _uiState.update { it.copy(bookmarks = bookmarks) }
        }
    }
}

sealed class BrowserEvent {
    data class BookmarkAdded(val bookmark: Bookmark) : BrowserEvent()
    data class NavigationError(val error: String) : BrowserEvent()
}
```

---

### UI Layer (platformMain/ui/)

**Purpose**: Platform-specific UI components

**Android**: Jetpack Compose
**iOS**: SwiftUI (via Kotlin/Native interop)
**Desktop**: Compose Desktop

**Rules:**
- ✅ Use platform-specific Compose/SwiftUI
- ✅ Observe ViewModels via StateFlow
- ✅ Handle platform-specific navigation
- ✅ Can use platform UI libraries (Material3, SF Symbols)

**Example (Android):**
```kotlin
// androidMain/ui/BrowserScreen.kt
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { BrowserTopBar() },
        bottomBar = { BrowserBottomBar() }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.error != null -> ErrorView(uiState.error!!)
            else -> BrowserContent(
                tabs = uiState.tabs,
                activeTab = uiState.activeTab,
                modifier = Modifier.padding(padding)
            )
        }
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowserEvent.BookmarkAdded -> {
                    // Show snackbar
                }
                is BrowserEvent.NavigationError -> {
                    // Show error dialog
                }
            }
        }
    }
}
```

---

## Platform-Specific Implementations (expect/actual)

### When to Use expect/actual

Use expect/actual when:
- Need platform-specific APIs (Context, UIViewController)
- Accessing native features (camera, location, biometrics)
- Platform-specific storage (Room, Core Data)
- Performance-critical operations (image processing)

### expect/actual Pattern

```kotlin
// commonMain/platform/FileIO.kt
expect class FileIO {
    suspend fun readFile(path: String): ByteArray
    suspend fun writeFile(path: String, data: ByteArray): Result<Unit>
}

// androidMain/platform/FileIO.kt
actual class FileIO {
    actual suspend fun readFile(path: String): ByteArray {
        return withContext(Dispatchers.IO) {
            File(path).readBytes()
        }
    }

    actual suspend fun writeFile(path: String, data: ByteArray): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                File(path).writeBytes(data)
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
    }
}

// iosMain/platform/FileIO.kt
actual class FileIO {
    actual suspend fun readFile(path: String): ByteArray {
        return withContext(Dispatchers.Default) {
            // Use NSFileManager
            // ...
        }
    }

    actual suspend fun writeFile(path: String, data: ByteArray): Result<Unit> {
        // iOS implementation
    }
}
```

---

## Module Naming Conventions

### Package Names
- **Root**: `com.augmentalis.[product].[module]`
- **Examples**:
  - `com.augmentalis.avanue.browser`
  - `com.augmentalis.voiceos.recognition`
  - `com.augmentalis.avamagic.components.ipc`

### File Names
- **Domain Models**: `[Entity].kt` (e.g., `Bookmark.kt`, `Tab.kt`)
- **Repository Interfaces**: `[Entity]Repository.kt` (e.g., `BookmarkRepository.kt`)
- **Repository Implementations**: `[Entity]RepositoryImpl.kt`
- **Use Cases**: `[Action][Entity]UseCase.kt` (e.g., `AddBookmarkUseCase.kt`)
- **ViewModels**: `[Feature]ViewModel.kt` (e.g., `BrowserViewModel.kt`)
- **UI Screens**: `[Feature]Screen.kt` (e.g., `BrowserScreen.kt`)
- **UI Components**: `[Component].kt` (e.g., `TabCard.kt`, `BookmarkList.kt`)

---

## Build Configuration (build.gradle.kts)

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.[product]"
version = "1.0.0"

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Desktop targets (optional)
    jvm()

    // Source sets
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                // Shared dependencies
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.room:room-runtime:2.6.0")
                implementation("androidx.room:room-ktx:2.6.0")
                // Android-specific dependencies
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }

        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "com.augmentalis.[product].[module]"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

---

## Documentation Requirements

Every module MUST have:

### 1. README.md
```markdown
# [Module Name]

**Purpose:** Brief description of what this module does

**Dependencies:**
- Module A (for X)
- Module B (for Y)

**Platforms:**
- ✅ Android
- ✅ iOS
- ⏳ Desktop (planned)

## Usage

Quick start code example

## Architecture

High-level architecture diagram or description

## Testing

How to run tests
```

### 2. ARCHITECTURE.md
```markdown
# Architecture

## Layers

### Domain
- Models
- Repositories
- Use Cases

### Data
- Android: Room + Retrofit
- iOS: Core Data + URLSession

### Presentation
- Shared ViewModels
- Platform-specific UI

## Data Flow

[Diagram or description of data flow]

## Design Decisions

### ADR-001: Using Room for Android
**Decision:** Use Room instead of SQLDelight
**Reason:** Better Android ecosystem integration, FTS5 support
**Alternatives Considered:** SQLDelight, ObjectBox
```

### 3. GlobalDesignStandard.md
Module-specific design decisions (e.g., IPCConnector has one for IPC patterns)

---

## Testing Standards

### Unit Tests (commonTest/)
- Test domain models
- Test use cases with mocked repositories
- Test mappers
- **Coverage Target:** 80%+

### Android Tests (androidUnitTest/)
- Test Android-specific repository implementations
- Test Room DAOs
- **Coverage Target:** 70%+

### Instrumented Tests (androidInstrumentedTest/)
- Test Compose UI components
- Test database migrations
- **Coverage Target:** Basic smoke tests

---

## Version History

- **v1.0.0** (2025-11-10): Initial Module Structure standard

---

**Created by Manoj Jhawar, manoj@ideahq.net**
