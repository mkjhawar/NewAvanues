# Hilt DI Migration Guide

**Version**: 1.0
**Last Updated**: November 15, 2025
**Target Audience**: AVA Developers
**Purpose**: Step-by-step guide for converting ViewModels to use Hilt dependency injection

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Prerequisites](#2-prerequisites)
3. [Step-by-Step ViewModel Conversion](#3-step-by-step-viewmodel-conversion)
4. [Common Patterns](#4-common-patterns)
5. [Testing Hilt-Injected ViewModels](#5-testing-hilt-injected-viewmodels)
6. [Troubleshooting](#6-troubleshooting)
7. [References](#7-references)

---

## 1. Introduction

### 1.1 Why Migrate to Hilt?

Hilt provides significant advantages over manual dependency injection:

| Benefit | Description |
|---------|-------------|
| **Type Safety** | Compile-time validation of dependencies (catch errors before runtime) |
| **No Nullables** | All dependencies guaranteed non-null (eliminates defensive checks) |
| **Easy Testing** | Simple mock injection using `@TestInstallIn` |
| **Context-Free** | Eliminates Context injection in ViewModels (prevents memory leaks) |
| **Industry Standard** | Google-recommended DI solution for Android |

### 1.2 Migration Status

As of November 15, 2025, **all AVA ViewModels have been migrated to Hilt**:

- ✅ ChatViewModel (Phase 3)
- ✅ SettingsViewModel (Phase 6)
- ✅ TeachAvaViewModel (Phase 7)

**New ViewModels** should follow the patterns in this guide from day one.

### 1.3 What This Guide Covers

- Converting existing ViewModels to use `@HiltViewModel`
- Adding constructor injection with `@Inject`
- Handling Context dependencies (the ActionsManager pattern)
- Testing Hilt-injected ViewModels
- Common pitfalls and troubleshooting

---

## 2. Prerequisites

### 2.1 Dependencies

Ensure your `build.gradle.kts` includes Hilt:

```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.51.1"
    id("com.google.devtools.ksp") version "2.0.0-1.0.23"
}

dependencies {
    // Hilt core
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    // Hilt ViewModels
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Testing (optional)
    testImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspTest("com.google.dagger:hilt-android-compiler:2.51.1")
}
```

### 2.2 Application Setup

Your Application class must be annotated with `@HiltAndroidApp`:

```kotlin
@HiltAndroidApp
class AvaApplication : Application() {
    // ...
}
```

**File**: `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/AvaApplication.kt`

### 2.3 Required Hilt Modules

AVA uses three Hilt modules (all located in `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/`):

1. **DatabaseModule.kt** - Provides `AVADatabase` and all DAOs
2. **RepositoryModule.kt** - Provides repository implementations
3. **AppModule.kt** - Provides singletons (ChatPreferences, IntentClassifier, ModelManager, ActionsManager)

**Verify these exist before converting ViewModels.**

---

## 3. Step-by-Step ViewModel Conversion

### 3.1 Starting Point (Before Hilt)

Let's use a typical ViewModel as our example:

```kotlin
// ❌ BEFORE (Manual DI)
class SettingsViewModel(
    private val context: Context,
    private val userPreferences: UserPreferences? = null
) : ViewModel() {

    private val prefs = userPreferences ?: UserPreferences(context)

    fun updateTheme(theme: String) {
        prefs.setTheme(theme)
    }
}
```

**Problems**:
- ❌ Context injection (memory leak risk)
- ❌ Nullable dependency with fallback logic
- ❌ Hard to test (requires Android context)

### 3.2 Step 1: Add Hilt Annotations

```kotlin
// ✅ STEP 1: Add @HiltViewModel and @Inject constructor
@HiltViewModel  // <-- Add this annotation
class SettingsViewModel @Inject constructor(  // <-- Add @Inject
    private val context: Context,
    private val userPreferences: UserPreferences? = null
) : ViewModel() {

    private val prefs = userPreferences ?: UserPreferences(context)

    fun updateTheme(theme: String) {
        prefs.setTheme(theme)
    }
}
```

**Import required**:
```kotlin
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
```

### 3.3 Step 2: Remove Nullable Dependencies

```kotlin
// ✅ STEP 2: Make dependencies non-nullable
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val context: Context,
    private val userPreferences: UserPreferences  // <-- Remove "? = null"
) : ViewModel() {

    // ❌ Remove fallback logic (no longer needed)
    // private val prefs = userPreferences ?: UserPreferences(context)

    fun updateTheme(theme: String) {
        userPreferences.setTheme(theme)  // <-- Use directly
    }
}
```

**Why?**: Hilt guarantees non-null injection, eliminating defensive programming.

### 3.4 Step 3: Remove Context Injection

**Option A: If Context is NOT needed**

```kotlin
// ✅ STEP 3A: Remove Context completely
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences  // <-- No Context!
) : ViewModel() {

    fun updateTheme(theme: String) {
        userPreferences.setTheme(theme)
    }
}
```

**Option B: If Context IS needed (use ActionsManager pattern)**

For cases where you MUST access Context (e.g., for `ActionsManager`), inject `ActionsManager` instead:

```kotlin
// ✅ STEP 3B: Replace Context with ActionsManager
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val trainExampleRepository: TrainExampleRepository,
    private val chatPreferences: ChatPreferences,
    private val intentClassifier: IntentClassifier,
    private val modelManager: ModelManager,
    private val actionsManager: ActionsManager,  // <-- Instead of Context
    private val responseGenerator: ResponseGenerator
) : ViewModel() {

    // Use actionsManager for Context-dependent operations
    private fun executeAction(intent: String) {
        actionsManager.executeIntent(intent)  // Internally uses Context
    }
}
```

**Why?**: ActionsManager wraps Context-dependent operations, keeping ViewModels testable.

### 3.5 Step 4: Update All Usages

Search for any code that used the old nullable pattern:

```kotlin
// ❌ BEFORE (Defensive null checks)
conversationRepository?.getAllConversations()?.collect { /* ... */ }

// ✅ AFTER (Direct usage)
conversationRepository.getAllConversations().collect { /* ... */ }
```

**Tip**: Use your IDE's "Find Usages" feature to locate all `?` operators related to injected dependencies.

### 3.6 Step 5: Update Composables

**Before** (Manual ViewModel creation):
```kotlin
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val viewModel = remember {
        SettingsViewModel(context, UserPreferences(context))
    }

    // UI code
}
```

**After** (Hilt ViewModel):
```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()  // <-- Use hiltViewModel()
) {
    // UI code
}
```

**Import required**:
```kotlin
import androidx.hilt.navigation.compose.hiltViewModel
```

### 3.7 Complete Example: SettingsViewModel

**File**: `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/settings/SettingsViewModel.kt`

```kotlin
package com.augmentalis.ava.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.core.data.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SettingsScreen, managing user preferences.
 *
 * Dependencies are injected via Hilt:
 * @param userPreferences User preferences manager
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    init {
        viewModelScope.launch {
            _theme.value = userPreferences.getTheme()
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            userPreferences.setTheme(theme)
            _theme.value = theme
        }
    }
}
```

---

## 4. Common Patterns

### 4.1 Repository Injection

**Pattern**: Inject repositories directly (no Context)

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val trainExampleRepository: TrainExampleRepository
) : ViewModel()
```

**Provided by**: `RepositoryModule.kt`

### 4.2 ChatPreferences Injection

**Pattern**: Inject `ChatPreferences` singleton

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatPreferences: ChatPreferences
) : ViewModel()
```

**Provided by**: `AppModule.provideChatPreferences()`

### 4.3 NLU Component Injection

**Pattern**: Inject `IntentClassifier` and `ModelManager`

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val modelManager: ModelManager
) : ViewModel()
```

**Provided by**: `AppModule.provideIntentClassifier()` and `AppModule.provideModelManager()`

### 4.4 ActionsManager Pattern (Context Replacement)

**When to use**: When ViewModel needs to execute actions that require Context

**Pattern**:
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val actionsManager: ActionsManager  // Replaces Context
) : ViewModel() {

    fun handleIntent(intent: String) {
        // ActionsManager internally uses Context
        actionsManager.executeIntent(intent)
    }
}
```

**Provided by**: `AppModule.provideActionsManager()`

**Why?**: Keeps ViewModels Context-free while still allowing Context-dependent operations.

### 4.5 Multiple Dependencies Example

**Real-world example** (ChatViewModel with 8 dependencies):

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val trainExampleRepository: TrainExampleRepository,
    private val chatPreferences: ChatPreferences,
    private val intentClassifier: IntentClassifier,
    private val modelManager: ModelManager,
    private val actionsManager: ActionsManager,
    private val responseGenerator: ResponseGenerator
) : ViewModel()
```

**All injected automatically by Hilt** - no manual wiring required!

---

## 5. Testing Hilt-Injected ViewModels

### 5.1 Unit Testing with Mocks

**Pattern**: Use `@TestInstallIn` to replace production modules with test modules

```kotlin
@HiltAndroidTest
class ChatViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var messageRepository: MessageRepository

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        hiltRule.inject()
        viewModel = ChatViewModel(
            conversationRepository,
            messageRepository,
            // ... other dependencies
        )
    }

    @Test
    fun `test sendMessage creates conversation`() {
        // Test implementation
    }
}
```

### 5.2 Mocking Dependencies

**Option 1**: Create a test module with `@TestInstallIn`

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideConversationRepository(): ConversationRepository {
        return mockk<ConversationRepository>()
    }
}
```

**Option 2**: Use manual injection in tests (simpler for small tests)

```kotlin
@Test
fun `test updateTheme saves preference`() {
    val mockPrefs = mockk<UserPreferences>()
    every { mockPrefs.setTheme(any()) } just Runs

    val viewModel = SettingsViewModel(mockPrefs)
    viewModel.updateTheme("dark")

    verify { mockPrefs.setTheme("dark") }
}
```

### 5.3 Testing ActionsManager Pattern

**Pattern**: Mock `ActionsManager` to verify intent execution

```kotlin
@Test
fun `test executeAction calls ActionsManager`() {
    val mockActionsManager = mockk<ActionsManager>()
    every { mockActionsManager.executeIntent(any()) } just Runs

    val viewModel = ChatViewModel(
        conversationRepository,
        messageRepository,
        trainExampleRepository,
        chatPreferences,
        intentClassifier,
        modelManager,
        mockActionsManager,
        responseGenerator
    )

    viewModel.handleIntent("open_settings")

    verify { mockActionsManager.executeIntent("open_settings") }
}
```

---

## 6. Troubleshooting

### 6.1 "Cannot find symbol: HiltViewModel"

**Error**:
```
error: cannot find symbol @HiltViewModel
```

**Solution**: Add missing dependencies to `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
}
```

### 6.2 "Dagger does not support injection into private fields"

**Error**:
```
error: Dagger does not support injection into private fields
```

**Solution**: Change field visibility from `private` to `internal` or remove visibility modifier:
```kotlin
// ❌ WRONG
@HiltViewModel
class MyViewModel @Inject constructor(
    @Inject private val repository: Repository  // <-- Remove @Inject here
) : ViewModel()

// ✅ CORRECT
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: Repository  // <-- Only @Inject on constructor
) : ViewModel()
```

### 6.3 "No injector factory bound for Class<YourViewModel>"

**Error**:
```
java.lang.RuntimeException: Cannot create an instance of class com.example.MyViewModel
```

**Solution**: Ensure your Activity/Fragment is annotated with `@AndroidEntryPoint`:
```kotlin
@AndroidEntryPoint  // <-- Add this
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyScreen()  // Uses hiltViewModel() inside
        }
    }
}
```

### 6.4 "Dependency X is not provided"

**Error**:
```
error: [Dagger/MissingBinding] com.example.MyRepository cannot be provided
```

**Solution**: Add a `@Provides` method in the appropriate module:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMyRepository(dao: MyDao): MyRepository {
        return MyRepositoryImpl(dao)
    }
}
```

### 6.5 "Circular dependency detected"

**Error**:
```
error: [Dagger/DependencyCycle] Found a dependency cycle
```

**Solution**: Refactor to break the cycle. Common approaches:
- Use `Provider<T>` for lazy injection
- Split modules into smaller, more focused modules
- Re-evaluate dependency relationships (may indicate design issue)

```kotlin
// ❌ CIRCULAR DEPENDENCY
class A @Inject constructor(val b: B)
class B @Inject constructor(val a: A)

// ✅ BREAK CYCLE WITH PROVIDER
class A @Inject constructor(val bProvider: Provider<B>)
class B @Inject constructor(val a: A)
```

### 6.6 "ActionsManager not found" when building

**Error**:
```
error: cannot find symbol ActionsManager
```

**Solution**: Ensure `ActionsManager` is in the correct module path:
- **Expected**: `Universal/AVA/Features/Actions/src/main/java/com/augmentalis/ava/features/actions/ActionsManager.kt`
- **Check**: Verify the Actions module is included in `settings.gradle`

---

## 7. References

### 7.1 Official Documentation

- [Hilt Developer Guide](https://developer.android.com/training/dependency-injection/hilt-android)
- [Hilt Testing Guide](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Jetpack Compose + Hilt](https://developer.android.com/jetpack/compose/libraries#hilt)

### 7.2 AVA-Specific Documentation

- **Developer Manual Chapter 32**: `docs/Developer-Manual-Chapter32-Hilt-DI.md` - Comprehensive Hilt architecture guide
- **Hilt DI Specification**: `.ideacode/specs/SPEC-hilt-di-implementation.md` - Full migration spec (9 phases)
- **Migration Report**: `docs/HILT-DI-MIGRATION-2025-11-13.md` - Detailed migration notes
- **Project Status**: `docs/PROJECT-PHASES-STATUS.md` - Current phase progress

### 7.3 Example Implementations

All ViewModels in AVA have been migrated to Hilt. Use these as reference:

1. **ChatViewModel** (8 dependencies, ActionsManager pattern)
   - File: `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/ui/ChatViewModel.kt`
   - Shows: Multiple repositories, NLU components, ActionsManager

2. **SettingsViewModel** (1 dependency, simple)
   - File: `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/settings/SettingsViewModel.kt`
   - Shows: Single dependency injection, preferences

3. **TeachAvaViewModel** (2 dependencies)
   - File: `Universal/AVA/Features/Teach/src/main/java/com/augmentalis/ava/features/teach/TeachAvaViewModel.kt`
   - Shows: Repository + preferences injection

### 7.4 Hilt Module Locations

All Hilt modules are in: `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/`

1. **DatabaseModule.kt** - Database + DAOs (6 DAOs total)
2. **RepositoryModule.kt** - Repository implementations (6 repositories)
3. **AppModule.kt** - Singletons (ChatPreferences, IntentClassifier, ModelManager, ActionsManager)

---

**Questions?** See [Troubleshooting](#6-troubleshooting) or consult the [Developer Manual Chapter 32](Developer-Manual-Chapter32-Hilt-DI.md).

**Last Updated**: November 15, 2025
**Version**: 1.0
**Maintained By**: AVA AI Team
