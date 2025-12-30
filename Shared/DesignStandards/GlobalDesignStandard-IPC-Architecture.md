# Global Design Standard: IPC Architecture

**Version:** 1.0.0
**Created:** 2025-11-10
**Last Updated:** 2025-11-10
**Status:** Living Document
**Scope:** All Avanues Ecosystem Modules

---

## Purpose

This standard defines **how all modules in the Avanues ecosystem communicate** with each other. It establishes patterns, best practices, and decision trees to ensure consistent, maintainable, and performant inter-process communication (IPC).

---

## Core Principles

1. **Simplicity First**: Use the simplest IPC mechanism that meets requirements
2. **Testability**: All IPC must be mockable and testable
3. **Type Safety**: Prefer type-safe Kotlin over Parcelable/Bundle when possible
4. **Error Handling**: All IPC must handle errors gracefully
5. **Thread Safety**: All IPC operations must be thread-safe
6. **Documentation**: All IPC interfaces must be fully documented

---

## IPC Decision Tree

Use this decision tree to choose the right IPC mechanism:

```
START: Need to communicate between modules?
│
├─ Same Process?
│  ├─ YES → Use **In-Process** patterns
│  │  ├─ Simple callback? → Direct function callbacks
│  │  ├─ Reactive data? → StateFlow / SharedFlow
│  │  ├─ Shared state? → SharedViewModel
│  │  └─ UI composition? → CompositionLocal
│  │
│  └─ NO → Need cross-process?
│     ├─ Internal service (VoiceOS <-> Avanues)? → Use **AIDL**
│     ├─ External plugin/3rd party? → Use **ContentProvider**
│     ├─ Event notification only? → Use **Broadcast Receiver**
│     └─ Data sharing (read-only)? → Use **ContentProvider**
```

---

## IPC Mechanisms

### 1. In-Process Communication (Same App)

#### 1.1 Direct Callbacks

**When to use:**
- Simple one-off function calls
- No state management needed
- Immediate response required

**Pattern:**
```kotlin
interface VoiceCommandHandler {
    fun processCommand(command: String): Boolean
    fun setOnResult(callback: (Boolean) -> Unit)
}

class BrowserVoiceHandler(
    private val viewModel: BrowserViewModel
) : VoiceCommandHandler {
    private var onResult: ((Boolean) -> Unit)? = null

    override fun processCommand(command: String): Boolean {
        val result = when {
            command.startsWith("open") -> {
                viewModel.openUrl(command.substringAfter("open"))
                true
            }
            else -> false
        }
        onResult?.invoke(result)
        return result
    }

    override fun setOnResult(callback: (Boolean) -> Unit) {
        onResult = callback
    }
}
```

**Pros:**
- Simple, direct
- No overhead
- Easy to debug

**Cons:**
- Tight coupling
- Hard to change callback signature
- No backpressure

---

#### 1.2 StateFlow / SharedFlow (Reactive)

**When to use:**
- Need to observe state changes
- Multiple subscribers
- Want backpressure handling
- Compose-based UI

**Pattern:**
```kotlin
class BrowserViewModel : ViewModel() {
    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    private val _events = MutableSharedFlow<BrowserEvent>()
    val events: SharedFlow<BrowserEvent> = _events.asSharedFlow()

    fun navigateToUrl(url: String) {
        _currentUrl.value = url
        viewModelScope.launch {
            _events.emit(BrowserEvent.NavigationStarted(url))
        }
    }
}

// Consumer
class CommandBar {
    init {
        viewModelScope.launch {
            browserViewModel.currentUrl.collect { url ->
                updateUrlBar(url)
            }
        }
    }
}
```

**Pros:**
- Reactive, declarative
- Multiple subscribers
- Built-in backpressure
- Compose-friendly

**Cons:**
- More complex than callbacks
- Need to manage lifecycle

---

#### 1.3 Shared ViewModel

**When to use:**
- Multiple UI components share state
- Bidirectional communication
- Complex state management

**Pattern:**
```kotlin
// Shared between Browser and CommandBar
class BrowserCommandBarBridge : ViewModel() {
    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    fun navigateToUrl(url: String) {
        _state.update { it.copy(currentUrl = url, isLoading = true) }
    }

    fun onPageLoaded(title: String) {
        _state.update { it.copy(pageTitle = title, isLoading = false) }
    }
}

// In Activity/Fragment
val bridge: BrowserCommandBarBridge by viewModels()

// Browser uses bridge
BrowserScreen(bridge = bridge)

// CommandBar uses same bridge
CommandBarScreen(bridge = bridge)
```

**Pros:**
- Single source of truth
- Automatic lifecycle management
- Survives configuration changes

**Cons:**
- Need ViewModel dependency
- Harder to test without ViewModelScope

---

#### 1.4 CompositionLocal (Compose Dependency Injection)

**When to use:**
- Providing theme/config to entire tree
- Dependency injection in Compose
- Avoid prop drilling

**Pattern:**
```kotlin
// Define CompositionLocal
data class BrowserTheme(
    val primaryColor: Color,
    val accentColor: Color
)

val LocalBrowserTheme = compositionLocalOf { BrowserTheme.default }

// Provide at root
@Composable
fun BrowserApp() {
    CompositionLocalProvider(LocalBrowserTheme provides customTheme) {
        BrowserScreen()  // Can access theme deep in tree
    }
}

// Consume anywhere in tree
@Composable
fun TabComponent() {
    val theme = LocalBrowserTheme.current
    Surface(color = theme.primaryColor) { ... }
}
```

**Pros:**
- Standard Compose pattern
- Avoids prop drilling
- Easy to test (provide mock)

**Cons:**
- Only works in Compose
- Can hide dependencies

---

### 2. Cross-Process Communication (AIDL)

**When to use:**
- Internal VoiceOS services (recognition, synthesis)
- Background services that must survive app death
- Process isolation required for stability

**Pattern:**
```kotlin
// 1. Define AIDL interface
// IVoiceRecognitionService.aidl
package com.augmentalis.voiceos;

import com.augmentalis.voiceos.IRecognitionCallback;

interface IVoiceRecognitionService {
    boolean startRecognition(String language, int mode);
    boolean stopRecognition();
    boolean isRecognizing();
    void registerCallback(IRecognitionCallback callback);
    void unregisterCallback(IRecognitionCallback callback);
    List<String> getAvailableEngines();
}

// 2. Implement Service
class VoiceRecognitionService : Service() {
    private val binder = object : IVoiceRecognitionService.Stub() {
        override fun startRecognition(language: String, mode: Int): Boolean {
            return recognitionEngine.start(language, mode)
        }
        // ... other methods
    }

    override fun onBind(intent: Intent): IBinder = binder
}

// 3. Client binding
class VoiceClient(private val context: Context) {
    private var service: IVoiceRecognitionService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IVoiceRecognitionService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    fun connect() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.augmentalis.voiceos",
                "com.augmentalis.voiceos.VoiceRecognitionService"
            )
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    suspend fun startRecognition(language: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val result = service?.startRecognition(language, 0) ?: false
                Result.success(result)
            } catch (e: RemoteException) {
                Result.failure(e)
            }
        }
    }
}
```

**AIDL Best Practices:**
1. **Use parcelable data classes for complex types**
2. **Always handle RemoteException**
3. **Use oneway for fire-and-forget operations**
4. **Register/unregister callbacks properly to avoid leaks**
5. **Document thread safety requirements**
6. **Use DeathRecipient to detect service crashes**

---

### 3. ContentProvider (External Plugins / Data Sharing)

**When to use:**
- External 3rd-party plugins need data
- Sharing structured data across apps
- Read-only data access
- CRUD operations on shared data

**Pattern:**
```kotlin
// 1. Define contract
object BrowserContract {
    const val AUTHORITY = "com.augmentalis.avanue.browser"

    object Bookmarks : BaseColumns {
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/bookmarks")
        const val TABLE_NAME = "bookmarks"
        const val COLUMN_URL = "url"
        const val COLUMN_TITLE = "title"
    }
}

// 2. Implement ContentProvider
class BrowserContentProvider : ContentProvider() {
    private lateinit var database: BrowserDatabase

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            BOOKMARKS -> {
                database.bookmarkDao().queryCursor(selection, selectionArgs)
            }
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // ... implementation
    }
}

// 3. Client access
class BookmarkReader(private val context: Context) {
    fun getAllBookmarks(): List<Bookmark> {
        val cursor = context.contentResolver.query(
            BrowserContract.Bookmarks.CONTENT_URI,
            null, null, null, null
        )

        return cursor?.use {
            val bookmarks = mutableListOf<Bookmark>()
            while (it.moveToNext()) {
                bookmarks.add(Bookmark(
                    url = it.getString(it.getColumnIndexOrThrow("url")),
                    title = it.getString(it.getColumnIndexOrThrow("title"))
                ))
            }
            bookmarks
        } ?: emptyList()
    }
}
```

**ContentProvider Best Practices:**
1. **Use batch operations for efficiency**
2. **Implement proper permissions**
3. **Use URI matching for different data types**
4. **Support ContentObserver for reactive updates**
5. **Return proper MIME types**

---

### 4. Broadcast Receivers (Event Notifications)

**When to use:**
- System-wide event notifications
- One-to-many communication
- No return value needed
- Decoupled components

**Pattern:**
```kotlin
// 1. Define broadcast actions
object BrowserBroadcasts {
    const val ACTION_PAGE_LOADED = "com.augmentalis.avanue.PAGE_LOADED"
    const val ACTION_DOWNLOAD_COMPLETE = "com.augmentalis.avanue.DOWNLOAD_COMPLETE"

    const val EXTRA_URL = "url"
    const val EXTRA_TITLE = "title"
}

// 2. Send broadcast
class BrowserViewModel {
    fun onPageLoaded(url: String, title: String) {
        val intent = Intent(BrowserBroadcasts.ACTION_PAGE_LOADED).apply {
            putExtra(BrowserBroadcasts.EXTRA_URL, url)
            putExtra(BrowserBroadcasts.EXTRA_TITLE, title)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}

// 3. Receive broadcast
class PageLoadedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BrowserBroadcasts.ACTION_PAGE_LOADED -> {
                val url = intent.getStringExtra(BrowserBroadcasts.EXTRA_URL)
                val title = intent.getStringExtra(BrowserBroadcasts.EXTRA_TITLE)
                onPageLoaded(url, title)
            }
        }
    }
}

// 4. Register receiver
val receiver = PageLoadedReceiver()
val filter = IntentFilter(BrowserBroadcasts.ACTION_PAGE_LOADED)
LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
```

**Broadcast Best Practices:**
1. **Use LocalBroadcastManager for in-app broadcasts**
2. **Unregister receivers to avoid leaks**
3. **Use explicit intents when possible**
4. **Don't send sensitive data via broadcasts**
5. **Consider WorkManager for background tasks**

---

## Module Communication Patterns

### Pattern 1: VoiceOS → App Modules

**Use case:** Voice commands to control browser, notes, AI

**Recommended:** AIDL for service binding + callbacks

```kotlin
// VoiceOS exposes IVoiceCommandService via AIDL
// Apps bind to service and register command handlers

interface IVoiceCommandService {
    void registerCommandHandler(String module, ICommandHandler handler);
    void unregisterCommandHandler(String module);
}

interface ICommandHandler {
    boolean handleCommand(String command);
}
```

---

### Pattern 2: App Modules → VoiceOS

**Use case:** Request voice recognition, speak text

**Recommended:** AIDL for service binding

```kotlin
// Apps bind to IVoiceRecognitionService
// Call methods directly

val result = voiceService.startRecognition("en-US", CONTINUOUS)
voiceService.speak("Hello world", priority = NORMAL)
```

---

### Pattern 3: Module → Module (Same App)

**Use case:** Browser → Theme, Browser → Logger

**Recommended:** Dependency Injection + Interfaces

```kotlin
class BrowserViewModel(
    private val theme: ThemeProvider,
    private val logger: Logger
) : ViewModel() {
    fun navigateToUrl(url: String) {
        logger.d("Browser", "Navigate: $url")
        // ... use theme.colors
    }
}
```

---

### Pattern 4: Plugin → App

**Use case:** 3rd-party plugins access app data

**Recommended:** ContentProvider

```kotlin
// Plugin queries app's ContentProvider
val cursor = contentResolver.query(
    Uri.parse("content://com.augmentalis.avanue.browser/bookmarks"),
    null, null, null, null
)
```

---

## Error Handling

### AIDL Error Handling
```kotlin
try {
    val result = service?.startRecognition(language, mode)
    if (result == true) {
        Result.success(Unit)
    } else {
        Result.failure(Exception("Recognition failed"))
    }
} catch (e: RemoteException) {
    // Service crashed or disconnected
    Result.failure(IPCError.ServiceUnavailable("Service died"))
} catch (e: SecurityException) {
    // Permission denied
    Result.failure(IPCError.PermissionDenied("Missing permission"))
}
```

### StateFlow Error Handling
```kotlin
sealed class BrowserState {
    data class Success(val data: Data) : BrowserState()
    data class Error(val exception: Throwable) : BrowserState()
    object Loading : BrowserState()
}

private val _state = MutableStateFlow<BrowserState>(BrowserState.Loading)
val state: StateFlow<BrowserState> = _state.asStateFlow()
```

---

## Thread Safety

### AIDL Threading
- AIDL methods are called on binder thread pool
- Do NOT perform long operations on binder thread
- Use Dispatchers.IO for database/network calls
- Post results back to main thread if needed

### StateFlow Threading
- Updates must be on proper dispatcher
- Use `flowOn(Dispatchers.IO)` for expensive operations
- Collect on Main for UI updates

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val data = loadData()  // Heavy operation
    _state.update { BrowserState.Success(data) }
}
```

---

## Testing

### Mocking AIDL
```kotlin
@Test
fun testVoiceRecognition() = runTest {
    val mockService = mock<IVoiceRecognitionService>()
    whenever(mockService.startRecognition(any(), any())).thenReturn(true)

    val client = VoiceClient(mockService)
    val result = client.startRecognition("en-US")

    assertTrue(result.isSuccess)
}
```

### Mocking StateFlow
```kotlin
@Test
fun testBrowserNavigation() = runTest {
    val viewModel = BrowserViewModel()

    viewModel.navigateToUrl("https://example.com")

    assertEquals("https://example.com", viewModel.currentUrl.value)
}
```

---

## Performance Guidelines

### IPC Performance Comparison

| Mechanism | Latency | Throughput | Use Case |
|-----------|---------|------------|----------|
| Direct Call | 0.001ms | Unlimited | In-process only |
| StateFlow | 0.01ms | High | Reactive updates |
| Intent | 1-5ms | Low | Events |
| AIDL | 0.1-1ms | Medium | Cross-process services |
| ContentProvider | 1-10ms | Medium | Data sharing |
| Broadcast | 5-20ms | Low | Notifications |

### Optimization Tips
1. **Batch operations** for ContentProvider queries
2. **Use oneway** for AIDL fire-and-forget
3. **Limit StateFlow emissions** (use `distinctUntilChanged`)
4. **Cache AIDL service references** (don't rebind frequently)
5. **Use parcelize** for complex data types

---

## Security

### AIDL Security
```xml
<!-- Define signature permission -->
<permission
    android:name="com.augmentalis.voiceos.VOICE_SERVICE"
    android:protectionLevel="signature" />

<service
    android:name=".VoiceRecognitionService"
    android:permission="com.augmentalis.voiceos.VOICE_SERVICE"
    android:exported="true" />
```

### ContentProvider Security
```xml
<provider
    android:name=".BrowserContentProvider"
    android:authorities="com.augmentalis.avanue.browser"
    android:exported="true"
    android:readPermission="com.augmentalis.avanue.READ_BOOKMARKS"
    android:writePermission="com.augmentalis.avanue.WRITE_BOOKMARKS" />
```

---

## Documentation Requirements

Every IPC interface must document:
1. **Purpose**: What does this interface do?
2. **Thread Safety**: Which thread are methods called on?
3. **Return Values**: What do return values mean?
4. **Exceptions**: What exceptions can be thrown?
5. **Example Usage**: Code example showing how to use

**Example:**
```kotlin
/**
 * Voice Recognition Service Interface
 *
 * Provides speech recognition functionality with multiple engine support.
 *
 * Thread Safety: All methods are called on binder thread pool.
 * Main thread is NOT required.
 *
 * @since 1.0.0
 */
interface IVoiceRecognitionService {
    /**
     * Start voice recognition
     *
     * @param language Language code (e.g., "en-US", "fr-FR")
     * @param mode Recognition mode (0=continuous, 1=single_shot)
     * @return true if started successfully, false if already running
     * @throws RemoteException if service dies
     * @throws SecurityException if missing permission
     *
     * Example:
     * ```kotlin
     * val result = service.startRecognition("en-US", CONTINUOUS)
     * if (result) { // Recognition started }
     * ```
     */
    boolean startRecognition(String language, int mode);
}
```

---

## Version History

- **v1.0.0** (2025-11-10): Initial IPC Architecture standard

---

## References

- NewAvanue IPC Documentation: `/Volumes/M-Drive/Coding/NewAvanue/docs/project-info/IPC-METHODS.md`
- VoiceOS AIDL Examples: `/Volumes/M-Drive/Coding/VoiceOS/app/src/main/aidl/`
- Avanues IPC Implementation: `modules/MagicIdea/Components/IPCConnector/`

---

**Created by Manoj Jhawar, manoj@ideahq.net**
