# ADR-005: WebView Lifecycle Management

**Status:** Accepted
**Date:** 2025-12-12
**Deciders:** WebAvanue Architecture Team

## Context

WebAvanue manages multiple browser tabs, each requiring a WebView instance. WebView creation is expensive on Android (100-300ms per instance), causing:

1. Slow tab creation (user waits 100-300ms for new tab)
2. Memory overhead (each WebView uses ~30-50MB)
3. Janky tab switching (WebView lifecycle transitions)
4. Poor UX with many tabs (memory pressure)

We needed a strategy to:
- Minimize tab creation latency
- Reduce memory footprint
- Enable smooth tab switching
- Handle background tab state efficiently
- Prevent memory leaks

Initial implementation attempted WebView pooling (pre-create WebViews), but this introduced complexity and violated SRP (Single Responsibility Principle).

## Decision

We will use **WebViewLifecycle** pattern with lazy initialization and proper state management.

Key principles:
- **Lazy Creation**: Create WebView only when tab becomes visible
- **State Preservation**: Save/restore WebView state on tab switch
- **Resource Cleanup**: Pause/resume WebView based on visibility
- **Memory Management**: Destroy background WebViews under memory pressure
- **SRP Compliance**: WebViewLifecycle handles only WebView lifecycle, not pooling

Pattern:
```kotlin
class WebViewLifecycle(
    private val context: Context,
    private val tab: Tab
) {
    private var webView: WebView? = null

    fun createIfNeeded(): WebView {
        return webView ?: createWebView().also { webView = it }
    }

    fun pause() {
        webView?.onPause()
    }

    fun resume() {
        webView?.onResume()
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }
}
```

## Rationale

### Why Lifecycle Management Over Pooling

1. **Simplicity**: Lifecycle pattern is simpler than pool management
2. **Memory Efficient**: Only active tabs have WebViews
3. **SRP Compliance**: Lifecycle handles one concern (WebView state)
4. **Testability**: Easy to mock and test lifecycle transitions
5. **Android Best Practices**: Follows Android lifecycle guidelines

### Technical Benefits

- **Lazy Initialization**: WebView created only when tab is visible (saves 100-300ms x inactive tabs)
- **Automatic Cleanup**: Lifecycle ensures proper pause/resume/destroy
- **State Management**: WebView state saved when tab backgrounded
- **Memory Pressure Handling**: Can destroy background WebViews to free memory
- **Thread Safety**: All WebView operations on Main thread

### Performance Benefits

- **Startup Time**: Don't pre-create WebViews, faster app launch
- **Memory Usage**: ~30-50MB saved per inactive tab
- **Tab Switching**: Smooth transitions with pause/resume
- **Scalability**: Can handle 100+ tabs (only active one has WebView)

## Consequences

### Positive

- ✅ **Fast App Startup**: No pre-created WebViews
- ✅ **Low Memory**: Only visible tab has WebView
- ✅ **Smooth Switching**: Proper pause/resume transitions
- ✅ **SRP Compliant**: Lifecycle handles only lifecycle
- ✅ **Testable**: Clear lifecycle methods to test
- ✅ **Android Compatible**: Follows platform guidelines
- ✅ **Leak-Free**: Proper cleanup prevents leaks

### Negative

- ⚠️ **Tab Creation Delay**: 100-300ms to create WebView when tab becomes visible (mitigated: acceptable UX, alternative is memory bloat)
- ⚠️ **State Loss Risk**: Must properly save/restore state (mitigated: WebView.saveState API)

### Mitigation Strategies

1. **Tab Creation Delay**: Show loading indicator during WebView creation
2. **State Loss**: Use WebView.saveState/restoreState for navigation history
3. **Memory Pressure**: Monitor memory and proactively destroy background WebViews

## Lifecycle States

### State 1: Created (No WebView)

Tab exists in database but WebView not yet created.

```kotlin
// Tab created
repository.createTab(Tab.create(url = "https://example.com"))

// WebView = null (not visible yet)
```

**Memory**: ~1KB (just Tab data model)

### State 2: Active (WebView Created)

Tab is visible, WebView created and active.

```kotlin
// User switches to tab
val lifecycle = WebViewLifecycle(context, tab)
val webView = lifecycle.createIfNeeded()  // Creates WebView
webView.loadUrl(tab.url)
```

**Memory**: ~30-50MB (WebView + rendered page)

### State 3: Paused (WebView Backgrounded)

Tab backgrounded but WebView retained for fast resume.

```kotlin
// User switches to another tab
lifecycle.pause()  // webView.onPause()
```

**Memory**: ~30-50MB (WebView retained)
**State**: JavaScript paused, timers stopped

### State 4: Destroyed (WebView Released)

WebView destroyed to free memory (low memory or tab closed).

```kotlin
// Low memory or tab closed
lifecycle.destroy()  // webView.destroy(), webView = null
```

**Memory**: ~1KB (just Tab data model)
**State**: Can recreate WebView later if tab reopened

## Implementation

### WebViewLifecycle Class

```kotlin
class WebViewLifecycle(
    private val context: Context,
    private val tab: Tab,
    private val onStateChange: (WebViewState) -> Unit
) {
    private var webView: WebView? = null
    private var state: WebViewState = WebViewState.CREATED

    /**
     * Create WebView if not already created
     */
    fun createIfNeeded(): WebView {
        return webView ?: run {
            val newWebView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    // ... other settings
                }

                webViewClient = createWebViewClient()
                webChromeClient = createWebChromeClient()

                // Restore state if exists
                tab.sessionData?.let { restoreState(Bundle.fromString(it)) }
            }

            webView = newWebView
            state = WebViewState.ACTIVE
            onStateChange(state)

            newWebView
        }
    }

    /**
     * Pause WebView (background tab)
     */
    fun pause() {
        webView?.onPause()
        state = WebViewState.PAUSED
        onStateChange(state)
    }

    /**
     * Resume WebView (foreground tab)
     */
    fun resume() {
        webView?.onResume()
        state = WebViewState.ACTIVE
        onStateChange(state)
    }

    /**
     * Save WebView state before destroying
     */
    fun saveState(): String? {
        val bundle = Bundle()
        webView?.saveState(bundle)
        return bundle.toString()
    }

    /**
     * Destroy WebView and free resources
     */
    fun destroy() {
        webView?.let {
            it.stopLoading()
            it.destroy()
        }
        webView = null
        state = WebViewState.DESTROYED
        onStateChange(state)
    }

    fun getWebView(): WebView? = webView
}

enum class WebViewState {
    CREATED,    // Tab exists, no WebView
    ACTIVE,     // WebView created and active
    PAUSED,     // WebView paused (background)
    DESTROYED   // WebView destroyed
}
```

### Usage in BrowserScreen

```kotlin
@Composable
fun BrowserScreen(
    activeTab: Tab,
    viewModel: TabViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = remember(activeTab.id) {
        WebViewLifecycle(
            context = context,
            tab = activeTab
        ) { state ->
            // Log state changes
            Napier.d("Tab ${activeTab.id} state: $state")
        }
    }

    DisposableEffect(lifecycle) {
        onDispose {
            lifecycle.pause()
            // Save state to database
            val sessionData = lifecycle.saveState()
            viewModel.updateTab(activeTab.copy(sessionData = sessionData))
        }
    }

    AndroidView(
        factory = { lifecycle.createIfNeeded() },
        update = { webView ->
            if (webView.url != activeTab.url) {
                webView.loadUrl(activeTab.url)
            }
        }
    )
}
```

### Memory Pressure Handling

```kotlin
class WebViewMemoryManager(
    private val maxMemoryMb: Int = 200
) {
    private val lifecycles = mutableMapOf<String, WebViewLifecycle>()

    fun registerLifecycle(tabId: String, lifecycle: WebViewLifecycle) {
        lifecycles[tabId] = lifecycle
    }

    fun handleMemoryPressure() {
        val currentMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024)

        if (currentMemory > maxMemoryMb) {
            // Destroy background WebViews
            lifecycles.values
                .filter { it.getWebView() != null }
                .sortedBy { it.lastAccessedAt }  // Destroy least recently used
                .take(3)
                .forEach { it.destroy() }

            Napier.w("Memory pressure: Destroyed background WebViews")
        }
    }
}
```

## Comparison: Pooling vs Lifecycle

### Pooling Approach (Rejected)

```kotlin
class WebViewPool {
    private val pool = mutableListOf<WebView>()
    private val maxSize = 5

    // ❌ Violates SRP: Manages both pool AND WebView creation
    fun acquire(): WebView {
        return pool.removeFirstOrNull() ?: createWebView()
    }

    fun release(webView: WebView) {
        if (pool.size < maxSize) {
            pool.add(webView)
        } else {
            webView.destroy()
        }
    }

    // ❌ Memory waste: 5 pre-created WebViews = 150-250MB
    fun prewarm() {
        repeat(maxSize) {
            pool.add(createWebView())
        }
    }
}
```

**Problems**:
- 🚫 SRP violation (pool management + WebView creation)
- 🚫 Memory waste (pre-created WebViews)
- 🚫 Complexity (pool size management)
- 🚫 State confusion (pool WebViews vs active WebViews)

### Lifecycle Approach (Accepted)

```kotlin
class WebViewLifecycle {
    private var webView: WebView? = null

    // ✅ SRP: Only manages WebView lifecycle
    fun createIfNeeded(): WebView {
        return webView ?: createWebView().also { webView = it }
    }

    fun pause() { webView?.onPause() }
    fun resume() { webView?.onResume() }
    fun destroy() { webView?.destroy(); webView = null }
}
```

**Benefits**:
- ✅ SRP compliant (single responsibility)
- ✅ Memory efficient (lazy creation)
- ✅ Simple (no pool management)
- ✅ Clear state (webView null or non-null)

## Alternatives Considered

### Alternative 1: WebView Pooling (Pre-creation)

- **Pros:**
  - Instant tab creation (no 100-300ms delay)
  - Warm WebViews ready to use

- **Cons:**
  - Memory waste (150-250MB for 5 pre-created WebViews)
  - SRP violation (pool + lifecycle management)
  - Pool size tuning complexity
  - Startup delay (pre-warming on launch)

- **Why Rejected:** Memory waste and SRP violation. Pre-creating WebViews trades memory for speed, but most users don't need 5 tabs simultaneously. Lifecycle approach is more memory-efficient.

### Alternative 2: Single Shared WebView

- **Pros:**
  - Minimal memory (only 1 WebView)
  - No lifecycle management

- **Cons:**
  - Loses navigation history on tab switch
  - Slow tab switching (must reload page)
  - No concurrent tab loading
  - Poor UX (tabs don't preserve state)

- **Why Rejected:** Destroys core browser UX. Users expect tabs to preserve state when switching.

### Alternative 3: Keep All WebViews Alive

- **Pros:**
  - Instant tab switching
  - Full state preservation

- **Cons:**
  - Massive memory usage (30-50MB x tab count)
  - App killed by system with many tabs
  - Poor performance on low-end devices

- **Why Rejected:** Unsustainable memory usage. With 20 tabs = 600-1000MB, app would be killed by Android.

### Alternative 4: WebView as Service (Background Processes)

- **Pros:**
  - WebViews in separate processes
  - Process isolation

- **Cons:**
  - Complex IPC (Inter-Process Communication)
  - Increased app complexity
  - Debugging difficulty
  - Android 10+ restrictions on background processes

- **Why Rejected:** Over-engineered for the problem. Lifecycle approach provides sufficient isolation without IPC overhead.

## Best Practices

### 1. Always Pause Background WebViews

```kotlin
// ✅ Good: Pause when tab backgrounded
DisposableEffect(activeTabId) {
    onDispose {
        lifecycle.pause()  // Stops JavaScript, timers
    }
}
```

### 2. Save State Before Destroying

```kotlin
// ✅ Good: Preserve navigation history
fun switchTab(newTabId: String) {
    val sessionData = currentLifecycle.saveState()
    repository.updateTab(currentTab.copy(sessionData = sessionData))
    currentLifecycle.destroy()
}
```

### 3. Handle Memory Pressure

```kotlin
// ✅ Good: Destroy WebViews on low memory
override fun onTrimMemory(level: Int) {
    when (level) {
        TRIM_MEMORY_MODERATE,
        TRIM_MEMORY_RUNNING_LOW -> {
            backgroundLifecycles.forEach { it.destroy() }
        }
    }
}
```

### 4. One Lifecycle Per Tab

```kotlin
// ✅ Good: Separate lifecycle per tab
val lifecycle = remember(tab.id) {
    WebViewLifecycle(context, tab)
}

// ❌ Bad: Reusing lifecycle across tabs
val lifecycle = remember { WebViewLifecycle(context, currentTab) }
```

## References

- [Android WebView Best Practices](https://developer.android.com/guide/webapps/webview#best-practices)
- [WebView Memory Management](https://developer.android.com/reference/android/webkit/WebView#destroy())
- [Single Responsibility Principle](https://en.wikipedia.org/wiki/Single-responsibility_principle)
- [Original Issue: #SRP-Violation](https://github.com/yourusername/WebAvanue/issues/...)

## Revision History

| Version | Date       | Changes                                                   |
|---------|------------|-----------------------------------------------------------|
| 1.0     | 2025-12-12 | Initial ADR documenting WebViewLifecycle decision         |
