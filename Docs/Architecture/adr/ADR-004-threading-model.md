# ADR-004: Coroutines and Threading Model

**Status:** Accepted
**Date:** 2025-12-12
**Deciders:** WebAvanue Architecture Team

## Context

WebAvanue performs various asynchronous operations (database queries, network requests, file I/O, WebView operations) that must not block the UI thread. We needed to establish a consistent threading model for:

1. Database operations (SQLDelight queries)
2. Repository methods (data access)
3. ViewModel operations (business logic)
4. UI updates (Compose recomposition)
5. WebView operations (platform-specific)

The threading model must:
- Prevent ANRs (Application Not Responding) on Android
- Ensure UI thread safety
- Provide structured concurrency
- Work across all KMP platforms
- Be testable and debuggable

## Decision

We will use **Kotlin Coroutines** with a **structured concurrency** model based on **CoroutineDispatchers**.

Threading Strategy:
- **Dispatchers.Main**: UI updates, StateFlow emissions
- **Dispatchers.IO**: Database queries, file I/O, network
- **Dispatchers.Default**: CPU-intensive computations
- **SupervisorJob**: Scope isolation to prevent cascading failures

Architecture Pattern:
```kotlin
Repository (Dispatchers.IO)
    ↓
ViewModel (Dispatchers.Main)
    ↓
UI (Dispatchers.Main)
```

## Rationale

### Why Coroutines Over Alternatives

1. **Native Kotlin**: First-class language support, not a library
2. **Structured Concurrency**: Parent-child job hierarchy prevents leaks
3. **Cancellation**: Automatic cleanup when scope is cancelled
4. **Suspending Functions**: Sequential async code (no callback hell)
5. **Flow**: Reactive streams that integrate with Compose
6. **KMP Support**: Works identically on Android, iOS, JVM, Native

### Technical Benefits

- **Context Switching**: Coroutines switch dispatchers efficiently
- **Memory Efficient**: Lightweight (thousands of coroutines = ~KB memory)
- **Composable**: `async`, `launch`, `withContext` combine naturally
- **Testable**: `runTest` provides deterministic test execution
- **Lifecycle-Aware**: Easy to scope to ViewModel/Screen lifecycle

### Safety Benefits

- **No Thread Leaks**: Cancelling scope cancels all child jobs
- **No Race Conditions**: Structured concurrency prevents interleaving
- **Exception Handling**: Centralized error handling with SupervisorJob
- **Deadlock-Free**: No manual lock management

## Consequences

### Positive

- ✅ **No ANRs**: Database ops run on Dispatchers.IO
- ✅ **UI Thread Safety**: StateFlow updates on Dispatchers.Main
- ✅ **Structured Concurrency**: Automatic cleanup prevents leaks
- ✅ **Readable Code**: Sequential async code with suspend functions
- ✅ **Testable**: `runTest` makes async tests deterministic
- ✅ **Cross-Platform**: Same code on Android, iOS, Desktop
- ✅ **Flow Integration**: Seamless Compose State integration

### Negative

- ⚠️ **Learning Curve**: Team must understand coroutines (mitigated: training provided)
- ⚠️ **Debugging**: Async stack traces can be complex (mitigated: IDE support improving)

### Mitigation Strategies

1. **Learning**: Provide coroutine training and best practices guide
2. **Debugging**: Use `CoroutineName` for readable stack traces
3. **Testing**: Use `runTest` and `TestCoroutineScheduler` for deterministic tests

## Threading Rules

### Rule 1: Repository Operations on IO Dispatcher

**All database and file operations MUST use Dispatchers.IO:**

```kotlin
// ✅ CORRECT
override suspend fun createTab(tab: Tab): Result<Tab> = withContext(Dispatchers.IO) {
    try {
        queries.insertTab(tab.toDbModel())
        refreshTabs()
        Result.success(tab)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ❌ WRONG - Blocks main thread
override suspend fun createTab(tab: Tab): Result<Tab> {
    queries.insertTab(tab.toDbModel())  // Runs on caller's dispatcher!
    return Result.success(tab)
}
```

### Rule 2: ViewModel Operations on Main Dispatcher

**ViewModels launch coroutines on Main, delegate to repository:**

```kotlin
class TabViewModel(private val repository: BrowserRepository) {
    // ✅ CORRECT: Scope on Main, repository uses IO
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun createTab(url: String) {
        viewModelScope.launch {  // Runs on Main
            _isLoading.value = true
            repository.createTab(tab)  // Switches to IO internally
                .onSuccess { tab ->
                    _activeTab.value = tab  // Back on Main
                    _isLoading.value = false
                }
        }
    }
}

// ❌ WRONG - UI updates on IO dispatcher
private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
```

### Rule 3: Flow Emissions on Main Dispatcher

**StateFlow updates MUST be on Main for Compose:**

```kotlin
// ✅ CORRECT: Switch to Main for StateFlow update
private suspend fun refreshTabs() {
    try {
        val tabs = queries.selectAllTabs()
            .executeAsList()
            .map { it.toDomainModel() }

        withContext(Dispatchers.Main) {
            _tabs.value = tabs  // UI-safe update
        }
    } catch (e: Exception) {
        Napier.e("Error refreshing tabs", e)
    }
}

// ❌ WRONG - Updates on IO dispatcher
private suspend fun refreshTabs() {
    withContext(Dispatchers.IO) {
        val tabs = queries.selectAllTabs().executeAsList()
        _tabs.value = tabs  // NOT SAFE! May be on IO dispatcher
    }
}
```

### Rule 4: Use SupervisorJob for Error Isolation

**Parent scope uses SupervisorJob to prevent cascading failures:**

```kotlin
// ✅ CORRECT: SupervisorJob prevents one failure from cancelling all
private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

viewModelScope.launch {
    // If this fails, other jobs continue
    repository.loadTabs()
}

viewModelScope.launch {
    // This keeps running even if loadTabs() failed
    repository.loadSettings()
}

// ❌ WRONG: Regular Job causes cascading cancellation
private val viewModelScope = CoroutineScope(Job() + Dispatchers.Main)
```

### Rule 5: Cancel Scopes in onCleared

**Prevent leaks by cancelling scope when ViewModel is destroyed:**

```kotlin
class TabViewModel(private val repository: BrowserRepository) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ✅ CORRECT: Cancel on cleanup
    fun onCleared() {
        viewModelScope.cancel()
    }
}
```

## Implementation Patterns

### Pattern 1: Repository Method

```kotlin
override suspend fun updateTab(tab: Tab): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        queries.updateTab(
            url = tab.url,
            title = tab.title,
            id = tab.id
        )
        refreshTabs()  // Switches to Main internally
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Pattern 2: ViewModel Loading

```kotlin
fun loadTabs() {
    viewModelScope.launch {  // Main dispatcher
        _isLoading.value = true
        _error.value = null

        repository.getAllTabs()  // Switches to IO
            .onSuccess { tabs ->
                _tabs.value = tabs  // Back on Main
                _isLoading.value = false
            }
            .onFailure { e ->
                _error.value = e.message
                _isLoading.value = false
            }
    }
}
```

### Pattern 3: Flow Observation

```kotlin
fun observeTabs() {
    viewModelScope.launch {
        repository.observeTabs()  // Flow<List<Tab>>
            .catch { e ->
                _error.value = "Failed to observe tabs: ${e.message}"
            }
            .collect { tabs ->
                _tabs.value = tabs  // Collect on Main
            }
    }
}
```

### Pattern 4: Parallel Operations

```kotlin
fun loadAllData() {
    viewModelScope.launch {
        val tabsDeferred = async { repository.getAllTabs() }
        val favoritesDeferred = async { repository.getAllFavorites() }
        val settingsDeferred = async { repository.getSettings() }

        // Await all in parallel
        val tabs = tabsDeferred.await().getOrNull()
        val favorites = favoritesDeferred.await().getOrNull()
        val settings = settingsDeferred.await().getOrNull()

        // Update UI
        _tabs.value = tabs ?: emptyList()
        _favorites.value = favorites ?: emptyList()
        _settings.value = settings
    }
}
```

### Pattern 5: Retry with Exponential Backoff

```kotlin
suspend fun retryOperation(maxRetries: Int = 3): Result<Unit> {
    repeat(maxRetries) { attempt ->
        delay(100L * (1 shl attempt))  // 100ms, 200ms, 400ms

        repository.syncData().onSuccess {
            return Result.success(Unit)
        }
    }
    return Result.failure(Exception("Max retries exceeded"))
}
```

## Testing with Coroutines

### Unit Tests with runTest

```kotlin
@Test
fun `createTab should add tab to list`() = runTest {
    val repository = FakeBrowserRepository()
    val viewModel = TabViewModel(repository)

    viewModel.createTab("https://example.com")

    // Assertions run after coroutines complete
    assertEquals(1, viewModel.tabs.value.size)
}
```

### Testing Flow

```kotlin
@Test
fun `tabs flow should emit updates`() = runTest {
    val repository = FakeBrowserRepository()
    val viewModel = TabViewModel(repository)

    val emissions = mutableListOf<List<Tab>>()
    val job = launch {
        viewModel.tabs.collect { emissions.add(it) }
    }

    viewModel.createTab("https://example.com")
    advanceUntilIdle()  // Process all pending coroutines

    assertEquals(2, emissions.size)  // Initial empty + new tab
    job.cancel()
}
```

## Alternatives Considered

### Alternative 1: Callbacks

- **Pros:**
  - Simple to understand
  - No external dependencies

- **Cons:**
  - Callback hell (nested callbacks)
  - Manual thread management
  - No structured concurrency
  - Memory leaks if callbacks not cleared
  - Difficult to test

- **Why Rejected:** Callbacks lead to unreadable code and manual lifecycle management. Coroutines provide structured concurrency with automatic cleanup.

### Alternative 2: RxJava

- **Pros:**
  - Mature library
  - Rich operator set
  - Observable streams

- **Cons:**
  - Large library size
  - Complex operators
  - Steep learning curve
  - Not native Kotlin
  - Poor KMP support

- **Why Rejected:** RxJava is Android-specific and adds ~3MB to app size. Coroutines + Flow provide equivalent functionality natively in Kotlin with better KMP support.

### Alternative 3: Thread Pools (ExecutorService)

- **Pros:**
  - Low-level control
  - Standard Java API

- **Cons:**
  - Manual thread management
  - No structured concurrency
  - Difficult to cancel
  - Thread leaks if not shut down
  - Complex error handling

- **Why Rejected:** Manual thread management is error-prone. Coroutines abstract thread pools with structured concurrency.

### Alternative 4: LiveData (Android)

- **Pros:**
  - Lifecycle-aware
  - Simple API
  - Android Architecture Component

- **Cons:**
  - Android-only (no KMP)
  - Transformation complexity
  - No backpressure
  - Less composable than Flow

- **Why Rejected:** LiveData is Android-specific. Flow is KMP-compatible and more powerful with coroutines integration.

## References

- [Kotlin Coroutines Documentation](https://kotlinlang.org/docs/coroutines-overview.html)
- [Kotlin Flow Guide](https://kotlinlang.org/docs/flow.html)
- [Android Coroutines Best Practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Structured Concurrency](https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency)

## Revision History

| Version | Date       | Changes                           |
|---------|------------|-----------------------------------|
| 1.0     | 2025-12-12 | Initial ADR documenting decision  |
