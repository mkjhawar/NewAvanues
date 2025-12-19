# Developer Manual - Chapter 32: Hilt Dependency Injection

**Version**: 1.1
**Last Updated**: November 13, 2025 (Code Evaluation Update)
**Authors**: AVA AI Team
**Status**: Phase 3 Complete - ChatViewModel Migrated
**Progress**: 33% (3/9 phases done)

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Architecture Overview](#2-architecture-overview)
3. [Module Structure](#3-module-structure)
4. [ViewModel Injection](#4-viewmodel-injection)
5. [Testing with Hilt](#5-testing-with-hilt)
6. [Best Practices](#6-best-practices)
7. [Common Patterns](#7-common-patterns)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Introduction

### 1.1 Migration Status

> **IMPORTANT UPDATE (2025-11-13):**
> ChatViewModel has been successfully migrated to Hilt! This chapter documents both
> the completed migration and remaining work.
>
> **Completed**: Phases 1-3 (Hilt modules + ChatViewModel conversion + tests)
> **Next**: Phase 4 (MainActivity integration)
> **See**: `.ideacode/specs/SPEC-hilt-di-implementation.md` for full status

### 1.2 Why Hilt?

Hilt is a dependency injection library for Android that reduces boilerplate and provides compile-time validation of dependencies. For AVA AI, Hilt provides:

- **Type Safety**: Compile-time dependency validation
- **Lifecycle Awareness**: Automatic component scoping
- **Testing Support**: Easy mock injection for tests
- **Standard Architecture**: Industry-standard DI pattern

### 1.3 Migration from Manual DI

**Before** (Manual DI):
```kotlin
class ChatViewModel(
    private val context: Context,
    private val repository: ConversationRepository? = null
) : ViewModel()
```

**After** (Hilt):
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ConversationRepository
) : ViewModel()
```

### 1.3 When to Use Hilt

| Use Case | Recommendation |
|----------|---------------|
| ViewModels | ✅ Always use `@HiltViewModel` |
| Repositories | ✅ Provide via modules |
| Singletons (DB, Prefs) | ✅ `@Singleton` scope |
| Android Components | ✅ Field injection (`@AndroidEntryPoint`) |
| Pure Kotlin classes | ⚠️ Consider manual if simple |

---

## 2. Architecture Overview

### 2.1 Hilt Component Hierarchy

```
Application (@HiltAndroidApp)
    │
    ├── AppModule (@InstallIn(SingletonComponent))
    │   ├── ChatPreferences
    │   ├── IntentClassifier
    │   ├── ModelManager
    │   └── UserPreferences
    │
    ├── DatabaseModule (@InstallIn(SingletonComponent))
    │   ├── AVADatabase
    │   └── DAOs (6 total)
    │
    └── RepositoryModule (@InstallIn(SingletonComponent))
        └── Repositories (6 total)
```

### 2.2 Component Scopes

| Scope | Lifetime | Use For |
|-------|----------|---------|
| `SingletonComponent` | Application | Database, Preferences, Services |
| `ViewModelComponent` | ViewModel | ViewModel-specific dependencies |
| `ActivityComponent` | Activity | Activity-scoped objects |
| `FragmentComponent` | Fragment | Fragment-scoped objects |

### 2.3 Dependency Graph

```mermaid
graph TD
    A[AvaApplication @HiltAndroidApp] --> B[SingletonComponent]
    B --> C[AppModule]
    B --> D[DatabaseModule]
    B --> E[RepositoryModule]
    
    C --> F[ChatPreferences]
    C --> G[IntentClassifier]
    C --> H[ModelManager]
    C --> I[UserPreferences]
    
    D --> J[AVADatabase]
    J --> K[DAOs]
    
    E --> L[Repositories]
    K --> L
    
    M[@HiltViewModel ChatViewModel] --> F
    M --> G
    M --> H
    M --> L
```

---

## 3. Module Structure

### 3.1 AppModule

**Purpose**: Application-level singletons (Preferences, Services)

**File**: `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/AppModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideChatPreferences(
        @ApplicationContext context: Context
    ): ChatPreferences {
        return ChatPreferences.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideIntentClassifier(
        @ApplicationContext context: Context
    ): IntentClassifier {
        return IntentClassifier.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideModelManager(
        @ApplicationContext context: Context
    ): ModelManager {
        return ModelManager(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences {
        return UserPreferences(context)
    }
}
```

**Key Points**:
- Use `@ApplicationContext` to get Application context (prevents memory leaks)
- `@Singleton` ensures single instance
- Return concrete implementations

### 3.2 DatabaseModule

**Purpose**: Database and DAO provisioning

**File**: `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/DatabaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AVADatabase {
        return DatabaseProvider.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideConversationDao(database: AVADatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AVADatabase): MessageDao {
        return database.messageDao()
    }

    // ... other DAOs
}
```

**Key Points**:
- Database is singleton (Room best practice)
- DAOs injected from database
- Uses existing `DatabaseProvider` for consistency

### 3.3 RepositoryModule

**Purpose**: Repository implementations

**File**: `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/RepositoryModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideConversationRepository(
        conversationDao: ConversationDao
    ): ConversationRepository {
        return ConversationRepositoryImpl(conversationDao)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        messageDao: MessageDao,
        conversationDao: ConversationDao
    ): MessageRepository {
        return MessageRepositoryImpl(messageDao, conversationDao)
    }

    // ... other repositories
}
```

**Key Points**:
- Return interface types (enables mocking)
- Dependencies automatically injected
- `@Singleton` for data consistency

---

## 4. ViewModel Injection

### 4.1 Basic Pattern

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    // ViewModel implementation
}
```

### 4.2 With Context

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MyRepository
) : ViewModel() {
    // Use context safely (no memory leaks)
}
```

### 4.3 Usage in Composables

```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // UI implementation
}
```

### 4.4 Usage in Activities/Fragments

```kotlin
@AndroidEntryPoint
class MyActivity : AppCompatActivity() {
    
    private val viewModel: MyViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use viewModel
    }
}
```

### 4.5 Multiple ViewModels

```kotlin
@Composable
fun ComplexScreen(
    chatViewModel: ChatViewModel = hiltViewModel(),
    teachViewModel: TeachAvaViewModel = hiltViewModel()
) {
    // Use multiple ViewModels
}
```

---

## 5. Testing with Hilt

### 5.1 Setup Test Module

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {
    
    @Provides
    @Singleton
    fun provideTestRepository(): MyRepository {
        return FakeRepository()
    }
}
```

### 5.2 Test Class Setup

```kotlin
@HiltAndroidTest
class ChatViewModelTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var repository: ConversationRepository
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun testChatViewModel() = runTest {
        val viewModel = ChatViewModel(
            context = ApplicationProvider.getApplicationContext(),
            conversationRepository = repository,
            // ... other dependencies
        )
        
        // Test implementation
    }
}
```

### 5.3 MockK Integration

```kotlin
@HiltAndroidTest
class ChatViewModelTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @MockK
    lateinit var mockRepository: ConversationRepository
    
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        hiltRule.inject()
    }
    
    @Test
    fun testWithMock() {
        every { mockRepository.getConversation(any()) } returns flowOf(mockConversation)
        // Test with mock
    }
}
```

---

## 6. Best Practices

### 6.1 DO's

✅ **Use `@ApplicationContext` for Context**
```kotlin
@Inject constructor(
    @ApplicationContext private val context: Context
)
```

✅ **Return Interface Types**
```kotlin
@Provides
fun provideRepository(): ConversationRepository {
    return ConversationRepositoryImpl() // Implementation
}
```

✅ **Use Singletons for Stateful Objects**
```kotlin
@Provides
@Singleton
fun provideDatabase(): AVADatabase { ... }
```

✅ **Constructor Injection in ViewModels**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel()
```

### 6.2 DON'Ts

❌ **Don't Use Activity Context in Singletons**
```kotlin
// BAD
@Provides
@Singleton
fun provideService(activity: Activity): MyService { ... }
```

❌ **Don't Mix Manual and Hilt Injection**
```kotlin
// BAD
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repo: MyRepository
) : ViewModel() {
    private val manual = ManualDependency() // ❌
}
```

❌ **Don't Create Multiple Singleton Instances**
```kotlin
// BAD
@Provides
fun provideDatabase(): AVADatabase { // Missing @Singleton
    return Room.databaseBuilder(...).build()
}
```

❌ **Don't Use Field Injection in ViewModels**
```kotlin
// BAD
@HiltViewModel
class MyViewModel : ViewModel() {
    @Inject lateinit var repository: MyRepository // ❌
}
```

---

## 7. Common Patterns

### 7.1 Optional Dependencies

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository,
    @Named("optional") private val optionalService: MyService?
) : ViewModel()

// In module
@Provides
@Named("optional")
fun provideOptionalService(): MyService? {
    return if (BuildConfig.FEATURE_ENABLED) MyServiceImpl() else null
}
```

### 7.2 Multiple Implementations

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalRepository

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteRepository

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    @LocalRepository
    fun provideLocalRepo(): DataRepository = LocalRepositoryImpl()
    
    @Provides
    @Singleton
    @RemoteRepository
    fun provideRemoteRepo(): DataRepository = RemoteRepositoryImpl()
}

// Usage
@HiltViewModel
class MyViewModel @Inject constructor(
    @LocalRepository private val localRepo: DataRepository,
    @RemoteRepository private val remoteRepo: DataRepository
) : ViewModel()
```

### 7.3 Conditional Provisioning

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ConditionalModule {
    
    @Provides
    @Singleton
    fun provideAnalytics(
        @ApplicationContext context: Context
    ): Analytics {
        return if (BuildConfig.DEBUG) {
            DebugAnalytics()
        } else {
            ProductionAnalytics(context)
        }
    }
}
```

### 7.4 Assisted Injection

For ViewModels with runtime parameters:

```kotlin
@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted private val itemId: String,
    private val repository: ItemRepository
) : ViewModel() {
    
    @AssistedFactory
    interface Factory {
        fun create(itemId: String): DetailViewModel
    }
}

// Usage
@Composable
fun DetailScreen(
    itemId: String,
    factory: DetailViewModel.Factory
) {
    val viewModel = viewModel<DetailViewModel>(
        factory = viewModelFactory {
            factory.create(itemId)
        }
    )
}
```

---

## 8. Troubleshooting

### 8.1 Common Errors

#### Error: "Cannot find symbol @HiltViewModel"

**Cause**: Missing Hilt dependency or kapt plugin

**Solution**:
```kotlin
plugins {
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

dependencies {
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

#### Error: "Dagger does not support injection into private fields"

**Cause**: Field injection with private modifier

**Solution**: Use constructor injection instead
```kotlin
// BAD
@HiltViewModel
class MyViewModel : ViewModel() {
    @Inject private lateinit var repository: MyRepository
}

// GOOD
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel()
```

#### Error: "Missing binding for X"

**Cause**: Dependency not provided in any module

**Solution**: Add provider to appropriate module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object MyModule {
    @Provides
    fun provideMissingDependency(): X = XImpl()
}
```

### 8.2 Build Issues

#### Slow kapt Build

**Solution**: Enable incremental kapt
```kotlin
// gradle.properties
kapt.incremental.apt=true
kapt.use.worker.api=true
```

#### Duplicate Class Errors

**Cause**: Hilt generated code conflicts

**Solution**: Clean and rebuild
```bash
./gradlew clean
./gradlew :app:kaptDebugKotlin
```

### 8.3 Runtime Issues

#### MainActivity Crash: "does not implement GeneratedComponent"

**Error**:
```
java.lang.IllegalStateException: Given component holder class
com.augmentalis.ava.MainActivity does not implement interface
dagger.hilt.internal.GeneratedComponent or interface
dagger.hilt.internal.GeneratedComponentManager
```

**Cause**: MainActivity missing `@AndroidEntryPoint` annotation

**When it happens**: When Activity tries to create Hilt ViewModels via `hiltViewModel()` in Compose, but the Activity itself is not annotated with `@AndroidEntryPoint`.

**Solution**: Add `@AndroidEntryPoint` annotation to MainActivity
```kotlin
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Now hiltViewModel() will work correctly
            val chatViewModel: ChatViewModel = hiltViewModel()
        }
    }
}
```

**Why this is required**:
- Hilt requires `@AndroidEntryPoint` on ANY Activity/Fragment that uses Hilt ViewModels
- This annotation triggers code generation for Hilt's dependency graph
- Without it, `hiltViewModel()` fails because there's no component to retrieve the ViewModel from

**Related files**:
- `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/MainActivity.kt:26`

**See also**: Section 4.4 "Usage in Activities/Fragments"

---

#### Service EntryPoint: "Injection of @HiltViewModel class is prohibited"

**Error**:
```
error: [dagger.hilt.android.processor.internal.viewmodel.ViewModelValidationPlugin]
Injection of an @HiltViewModel class is prohibited since it does not create a
ViewModel instance correctly.
Access the ViewModel via the Android APIs (e.g. ViewModelProvider) instead.
Injected ViewModel: com.augmentalis.ava.features.chat.ui.ChatViewModel
```

**Cause**: Attempting to directly inject `@HiltViewModel` via `@EntryPoint`

**Why it fails**:
- Hilt ViewModels **must** be created via `ViewModelProvider`
- Services cannot use `ViewModelProvider` (no `ViewModelStoreOwner`)
- Direct injection bypasses ViewModel lifecycle management

**❌ WRONG Approach**:
```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatViewModelEntryPoint {
    fun chatViewModel(): ChatViewModel  // ❌ Prohibited by Hilt
}
```

**✅ CORRECT Approach**: Inject dependencies, construct manually
```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatViewModelDependenciesEntryPoint {
    fun conversationRepository(): ConversationRepository
    fun messageRepository(): MessageRepository
    fun trainExampleRepository(): TrainExampleRepository
    fun chatPreferences(): ChatPreferences
    fun intentClassifier(): IntentClassifier
    fun modelManager(): ModelManager
    fun actionsManager(): ActionsManager
    fun responseGenerator(): ResponseGenerator
}

// In Service:
private fun initializeChatViewModel() {
    val entryPoint = EntryPointAccessors.fromApplication(
        applicationContext,
        ChatViewModelDependenciesEntryPoint::class.java
    )

    chatViewModel = ChatViewModel(
        conversationRepository = entryPoint.conversationRepository(),
        messageRepository = entryPoint.messageRepository(),
        trainExampleRepository = entryPoint.trainExampleRepository(),
        chatPreferences = entryPoint.chatPreferences(),
        intentClassifier = entryPoint.intentClassifier(),
        modelManager = entryPoint.modelManager(),
        actionsManager = entryPoint.actionsManager(),
        responseGenerator = entryPoint.responseGenerator()
    )
}
```

**Benefits**:
- ✅ All dependencies still managed by Hilt
- ✅ No hardcoded construction
- ✅ Respects Hilt ViewModel rules
- ✅ Type-safe dependency injection

**Related files**:
- `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/overlay/AvaChatOverlayService.kt`

**Where this happened**: Phase 6 (Nov 14, 2025)
**Time to fix**: 15 minutes
**Impact**: Build failure
**Prevention**: Never directly inject `@HiltViewModel` - always inject dependencies

**Rule of Thumb**:
- **Activities/Fragments**: Use `hiltViewModel()` or `by viewModels()`
- **Services**: Use `@EntryPoint` for **dependencies**, construct ViewModel manually
- **Never**: Directly inject `@HiltViewModel` classes

**See also**: Section 7.X "EntryPoint Pattern for Services"

---

#### NullPointerException in ViewModel

**Cause**: Dependency not properly injected

**Check**:
1. Module has `@Provides` method
2. Component scope matches injection site
3. `@HiltViewModel` annotation present
4. Activity/Fragment has `@AndroidEntryPoint`
5. Services use `@EntryPoint` for dependencies (not ViewModels directly)

### 8.4 Testing Issues

#### Test Dependencies Not Injected

**Solution**: Add `@HiltAndroidTest` and inject rule
```kotlin
@HiltAndroidTest
class MyTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
}
```

---

## 9. Performance Considerations

### 9.1 Initialization Time

| Component | Init Time | Optimization |
|-----------|-----------|--------------|
| Database | ~50ms | Lazy init via Hilt |
| Preferences | ~5ms | Cached singleton |
| Repositories | <1ms | Lazy init |
| ViewModels | <1ms | On-demand creation |

### 9.2 Memory Footprint

| Component | Memory | Lifecycle |
|-----------|--------|-----------|
| Hilt Container | ~100KB | Application |
| Singletons | ~50KB | Application |
| ViewModels | Variable | Activity/Fragment |
| Total Overhead | ~150KB | Negligible |

### 9.3 APK Size

- Hilt library: ~150KB
- Generated code: ~50KB
- **Total**: ~200KB (0.5% of typical APK)

---

## 10. Migration Guide

### 10.1 From Manual DI

**Step 1**: Add Hilt dependencies
```kotlin
plugins {
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

dependencies {
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

**Step 2**: Annotate Application
```kotlin
@HiltAndroidApp
class AvaApplication : Application()
```

**Step 3**: Create modules
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideX(): X = XImpl()
}
```

**Step 4**: Migrate ViewModels
```kotlin
// Before
class MyViewModel(private val repo: Repository)

// After
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repo: Repository
) : ViewModel()
```

**Step 5**: Update usage
```kotlin
// Before
val viewModel = MyViewModel(repository)

// After
val viewModel: MyViewModel by viewModels()
```

### 10.2 From Koin

| Koin | Hilt |
|------|------|
| `single { }` | `@Provides @Singleton` |
| `factory { }` | `@Provides` (no scope) |
| `viewModel { }` | `@HiltViewModel` |
| `get()` | Constructor param |
| `inject()` | `by viewModels()` |

---

## 11. Examples

### 11.1 Complete ViewModel Example

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val trainExampleRepository: TrainExampleRepository,
    private val chatPreferences: ChatPreferences,
    private val intentClassifier: IntentClassifier,
    private val modelManager: ModelManager
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            // Implementation using injected dependencies
            val classification = intentClassifier.classify(text)
            // ...
        }
    }
}
```

### 11.2 Complete Module Example

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FeatureModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AVADatabase {
        return Room.databaseBuilder(
            context,
            AVADatabase::class.java,
            "ava_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideRepository(
        database: AVADatabase
    ): MyRepository {
        return MyRepositoryImpl(database.myDao())
    }
    
    @Provides
    @Singleton
    fun providePreferences(
        @ApplicationContext context: Context
    ): MyPreferences {
        return MyPreferences(context)
    }
}
```

### 11.3 Complete Test Example

```kotlin
@HiltAndroidTest
class ChatViewModelTest {
    
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    var coroutineRule = MainCoroutineRule()
    
    @Inject
    lateinit var database: AVADatabase
    
    private lateinit var viewModel: ChatViewModel
    private lateinit var repository: ConversationRepository
    
    @Before
    fun setup() {
        hiltRule.inject()
        repository = ConversationRepositoryImpl(database.conversationDao())
        viewModel = ChatViewModel(
            context = ApplicationProvider.getApplicationContext(),
            conversationRepository = repository,
            // ... other dependencies
        )
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testSendMessage() = runTest {
        // Given
        val message = "Hello AVA"
        
        // When
        viewModel.sendMessage(message)
        
        // Then
        val messages = viewModel.messages.first()
        assertEquals(1, messages.size)
        assertEquals(message, messages[0].content)
    }
}
```

---

## 12. References

- [Hilt Documentation](https://dagger.dev/hilt/)
- [Android Hilt Guide](https://developer.android.com/training/dependency-injection/hilt-android)
- [Hilt Testing](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Migration Report](HILT-DI-MIGRATION-2025-11-13.md)

---

## 13. SOLID Coordinators (Chapter 72)

### 13.1 Coordinator Injection Pattern

As part of the SOLID refactoring (Chapter 72), ChatViewModel now uses specialized coordinators:

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    // ... existing dependencies ...

    // P0: SOLID Coordinators
    private val nluCoordinator: NLUCoordinator,
    private val responseCoordinator: ResponseCoordinator,
    private val ragCoordinator: RAGCoordinator,
    private val actionCoordinator: ActionCoordinator,

    // P1: WakeWordEventBus
    private val wakeWordEventBus: WakeWordEventBus,

    @ApplicationContext private val context: Context
) : ViewModel()
```

### 13.2 Coordinator Registration

All coordinators use `@Singleton` and `@Inject constructor`:

```kotlin
@Singleton
class NLUCoordinator @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val modelManager: ModelManager,
    private val trainExampleRepository: TrainExampleRepository,
    private val chatPreferences: ChatPreferences
)

@Singleton
class ResponseCoordinator @Inject constructor(
    private val responseGenerator: ResponseGenerator,
    private val learningManager: IntentLearningManager,
    private val nluSelfLearner: NLUSelfLearner,
    private val chatPreferences: ChatPreferences
)

@Singleton
class RAGCoordinator @Inject constructor(
    private val ragRepository: RAGRepository?,
    private val chatPreferences: ChatPreferences
)

@Singleton
class ActionCoordinator @Inject constructor(
    private val actionsManager: ActionsManager
)

@Singleton
class WakeWordEventBus @Inject constructor()
```

### 13.3 Service EntryPoint for Coordinators

Services access coordinators via `@EntryPoint`:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatViewModelDependenciesEntryPoint {
    // ... existing dependencies ...

    // P0: SOLID Coordinators
    fun nluCoordinator(): NLUCoordinator
    fun responseCoordinator(): ResponseCoordinator
    fun ragCoordinator(): RAGCoordinator
    fun actionCoordinator(): ActionCoordinator

    // P1: WakeWordEventBus
    fun wakeWordEventBus(): WakeWordEventBus
}
```

### 13.4 Coordinator Files

| Coordinator | File | Purpose |
|-------------|------|---------|
| NLUCoordinator | `coordinator/NLUCoordinator.kt` | NLU state, classification, intent caching |
| ResponseCoordinator | `coordinator/ResponseCoordinator.kt` | LLM/template responses, self-learning |
| RAGCoordinator | `coordinator/RAGCoordinator.kt` | RAG context retrieval, source citations |
| ActionCoordinator | `coordinator/ActionCoordinator.kt` | Action execution, routing |
| WakeWordEventBus | `event/WakeWordEventBus.kt` | Wake word event communication |

**See:** [Chapter 72: SOLID Architecture](Developer-Manual-Chapter72-SOLID-Architecture.md) for complete documentation.

---

**Chapter Complete** - Last Updated: December 5, 2025
