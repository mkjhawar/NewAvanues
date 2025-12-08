# LocalizationManager Developer Manual

**Module:** LocalizationManager
**Location:** `/modules/managers/LocalizationManager`
**Namespace:** `com.augmentalis.localizationmanager`
**Last Updated:** 2025-12-01
**Version:** 2.0.0 (SQLDelight Migration)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Database Layer](#database-layer)
5. [Preferences API](#preferences-api)
6. [UI Components](#ui-components)
7. [Testing](#testing)
8. [API Reference](#api-reference)

---

## Overview

LocalizationManager handles user preferences and localization settings for VOS4:

- **User Preferences**: Persistent key-value storage
- **Message Debounce**: Configurable delay for message display
- **Statistics Display**: Auto-show configuration
- **Animation Settings**: Language animation toggles
- **Detail Levels**: User preference for information density

### Key Features

- SQLDelight database (KMP-ready)
- Flow-based reactive preferences
- Compose UI integration
- Type-safe preference keys
- Default value handling

---

## Architecture

### Component Architecture

```
LocalizationManager
├── data/                    # Data layer
│   ├── UserPreference.kt   # Domain model
│   ├── LocalizationDatabase.kt # Database singleton
│   └── sqldelight/         # SQLDelight adapters
│       ├── PreferencesDaoAdapter.kt # DAO implementation
│       └── TypeAliases.kt  # Backward compatibility
├── repository/             # Repository layer
│   └── PreferencesRepository.kt # Business logic
└── ui/                     # UI layer
    ├── LocalizationManagerActivity.kt
    ├── LocalizationManagerContent.kt
    ├── LocalizationViewModel.kt
    └── TestableComposables.kt
```

### Design Principles

1. **Direct Implementation**: No interfaces unless strategically valuable
2. **Reactive**: Flow-based preferences for Compose integration
3. **Type-Safe**: Strongly-typed preference keys and values
4. **KMP-Ready**: SQLDelight for cross-platform compatibility

---

## Database Layer

### SQLDelight Migration (v2.0.0)

LocalizationManager uses **SQLDelight** via the shared `VoiceOSDatabase`:

#### Schema Location

`libraries/core/database/src/commonMain/sqldelight/com/augmentalis/database/UserPreference.sq`

#### Key Queries

```sql
-- Insert or update preference
INSERT OR REPLACE INTO user_preference(key, value, type, updatedAt) VALUES (?, ?, ?, ?);

-- Get value by key
getValue:
SELECT value FROM user_preference WHERE key = ?;

-- Get by pattern (for localization prefs)
getByKeyPattern:
SELECT * FROM user_preference WHERE key LIKE ?;

-- LocalizationManager-specific queries
getDebounceDuration:
SELECT value FROM user_preference WHERE key = 'message_debounce_duration';

getStatisticsAutoShow:
SELECT value FROM user_preference WHERE key = 'statistics_auto_show';

getLanguageAnimationEnabled:
SELECT value FROM user_preference WHERE key = 'language_animation_enabled';
```

### Adapter Classes

**PreferencesDaoAdapter** wraps SQLDelight queries:

```kotlin
class PreferencesDaoAdapter(
    private val queries: UserPreferenceQueries
) {
    companion object {
        private const val TYPE_LOCALIZATION = "LOCALIZATION"

        fun create(context: Context): PreferencesDaoAdapter {
            val driver = DatabaseDriverFactory(context.applicationContext).createDriver()
            val db = VoiceOSDatabase(driver)
            return PreferencesDaoAdapter(db.userPreferenceQueries)
        }
    }

    suspend fun savePreference(preference: UserPreference) = withContext(Dispatchers.IO) {
        queries.insert(
            key = preference.key,
            value_ = preference.value,
            type = TYPE_LOCALIZATION,
            updatedAt = preference.lastModified
        )
    }

    suspend fun getPreference(key: String): String? = withContext(Dispatchers.IO) {
        queries.getValue(key).executeAsOneOrNull()
    }
}
```

### Database Initialization

**LocalizationDatabase.kt** provides singleton access:

```kotlin
object LocalizationDatabase {
    @Volatile
    private var preferencesDao: PreferencesDaoAdapter? = null

    fun getPreferencesDao(context: Context): PreferencesDaoAdapter {
        return preferencesDao ?: synchronized(this) {
            preferencesDao ?: PreferencesDaoAdapter.create(context).also {
                preferencesDao = it
            }
        }
    }
}
```

---

## Preferences API

### Preference Keys

Defined in `UserPreference.kt`:

```kotlin
object PreferenceKeys {
    const val MESSAGE_DEBOUNCE_DURATION = "message_debounce_duration"
    const val STATISTICS_AUTO_SHOW = "statistics_auto_show"
    const val LANGUAGE_ANIMATION_ENABLED = "language_animation_enabled"
    const val PREFERRED_DETAIL_LEVEL = "preferred_detail_level"
}
```

### Default Values

```kotlin
object PreferenceDefaults {
    const val MESSAGE_DEBOUNCE_DURATION = 2000L // 2 seconds
    const val STATISTICS_AUTO_SHOW = false
    const val LANGUAGE_ANIMATION_ENABLED = true
    const val PREFERRED_DETAIL_LEVEL = "STANDARD"
}
```

### Debounce Duration Options

```kotlin
enum class DebounceDuration(val displayName: String, val milliseconds: Long) {
    INSTANT("Instant (No delay)", 0L),
    FAST("Fast (1 second)", 1000L),
    NORMAL("Normal (2 seconds)", 2000L),
    SLOW("Slow (3 seconds)", 3000L),
    VERY_SLOW("Very Slow (5 seconds)", 5000L)
}
```

### Detail Level Options

```kotlin
enum class DetailLevel(val displayName: String) {
    MINIMAL("Minimal"),
    STANDARD("Standard"),
    COMPREHENSIVE("Comprehensive")
}
```

---

## PreferencesRepository

Repository provides typed access to preferences:

### Usage

```kotlin
val repository = PreferencesRepository(
    LocalizationDatabase.getPreferencesDao(context)
)

// Read preferences (Flow-based)
repository.getDebounceDuration().collect { duration ->
    // Use duration: Long
}

repository.getStatisticsAutoShow().collect { autoShow ->
    // Use autoShow: Boolean
}

// Write preferences
repository.saveDebounceDuration(3000L)
repository.saveStatisticsAutoShow(true)
repository.saveLanguageAnimationEnabled(false)
repository.savePreferredDetailLevel(DetailLevel.COMPREHENSIVE)
```

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getDebounceDuration()` | `Flow<Long>` | Message debounce in ms |
| `saveDebounceDuration(duration)` | `Unit` | Save debounce duration |
| `getStatisticsAutoShow()` | `Flow<Boolean>` | Auto-show statistics |
| `saveStatisticsAutoShow(autoShow)` | `Unit` | Save auto-show pref |
| `getLanguageAnimationEnabled()` | `Flow<Boolean>` | Animation enabled |
| `saveLanguageAnimationEnabled(enabled)` | `Unit` | Save animation pref |
| `getPreferredDetailLevel()` | `Flow<DetailLevel>` | Detail level enum |
| `savePreferredDetailLevel(level)` | `Unit` | Save detail level |
| `getAllPreferences()` | `Flow<List<UserPreference>>` | All prefs |
| `clearAllPreferences()` | `Unit` | Reset to defaults |

---

## UI Components

### LocalizationManagerActivity

Main entry point for the settings screen:

```kotlin
class LocalizationManagerActivity : ComponentActivity() {
    private val preferencesRepository by lazy {
        PreferencesRepository(LocalizationDatabase.getPreferencesDao(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalizationManagerTheme {
                val viewModel: LocalizationViewModel = viewModel(
                    factory = LocalizationViewModelFactory(this, preferencesRepository)
                )
                LocalizationManagerContent(viewModel = viewModel)
            }
        }
    }
}
```

### LocalizationViewModel

Manages UI state and preference operations:

```kotlin
class LocalizationViewModel(
    private val repository: PreferencesRepository
) : ViewModel() {
    val debounceDuration = repository.getDebounceDuration()
        .stateIn(viewModelScope, SharingStarted.Lazily, 2000L)

    fun updateDebounceDuration(duration: Long) {
        viewModelScope.launch {
            repository.saveDebounceDuration(duration)
        }
    }
}
```

### TestableComposables

For testing UI components:

```kotlin
@Composable
fun LocalizationManagerScreen() {
    val context = LocalContext.current
    val preferencesRepository = PreferencesRepository(
        LocalizationDatabase.getPreferencesDao(context)
    )
    val viewModel: LocalizationViewModel = viewModel(
        factory = LocalizationViewModelFactory(context, preferencesRepository)
    )
    LocalizationManagerContent(viewModel = viewModel)
}
```

---

## Testing

### Test Structure

```
src/test/java/com/augmentalis/localizationmanager/
├── repository/
│   └── PreferencesRepositoryTest.kt
├── ui/
│   └── LocalizationViewModelTest.kt
└── data/
    └── PreferencesDaoAdapterTest.kt
```

### Running Tests

```bash
# Run all tests
./gradlew :modules:managers:LocalizationManager:test

# Run specific test class
./gradlew :modules:managers:LocalizationManager:test --tests PreferencesRepositoryTest
```

### Example Test

```kotlin
@RunWith(AndroidJUnit4::class)
class PreferencesRepositoryTest {

    private lateinit var repository: PreferencesRepository
    private lateinit var dao: PreferencesDaoAdapter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dao = PreferencesDaoAdapter.create(context)
        repository = PreferencesRepository(dao)
    }

    @Test
    fun testSaveAndRetrieveDebounceDuration() = runTest {
        // Save
        repository.saveDebounceDuration(3000L)

        // Retrieve
        repository.getDebounceDuration().first().let { duration ->
            assertEquals(3000L, duration)
        }
    }
}
```

---

## API Reference

### Data Models

#### UserPreference

```kotlin
data class UserPreference(
    val key: String,
    val value: String,
    val lastModified: Long = System.currentTimeMillis()
)
```

### Database

#### LocalizationDatabase

```kotlin
object LocalizationDatabase {
    fun getPreferencesDao(context: Context): PreferencesDaoAdapter
}
```

#### PreferencesDaoAdapter

```kotlin
class PreferencesDaoAdapter(queries: UserPreferenceQueries) {
    companion object {
        fun create(context: Context): PreferencesDaoAdapter
    }

    suspend fun getPreference(key: String): String?
    suspend fun getPreferenceWithTimestamp(key: String): UserPreference?
    suspend fun savePreference(preference: UserPreference)
    fun getAllPreferences(): Flow<List<UserPreference>>
    fun getPreferencesByPattern(keyPattern: String): Flow<List<UserPreference>>
    suspend fun deletePreference(key: String)
    suspend fun clearAllPreferences()
    fun getDebounceDuration(): Flow<String?>
    fun getStatisticsAutoShow(): Flow<String?>
    fun getLanguageAnimationEnabled(): Flow<String?>
}
```

---

## Related Documentation

- [User Manual](./user-manual.md) - End-user settings reference
- [Changelog](./changelog/CHANGELOG.md) - Version history
- [CommandManager Developer Manual](../CommandManager/developer-manual.md) - Related module

---

**Last Updated:** 2025-12-01
**Maintained By:** VOS4 Development Team
**Version:** 2.0.0 (SQLDelight Migration)
