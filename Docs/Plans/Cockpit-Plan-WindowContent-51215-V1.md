# Implementation Plan: Window Content Integration

**Project:** NewAvanues-Cockpit
**Feature:** Window Content Wiring & WebView Integration
**Specification:** [Cockpit-Spec-WindowContent-51215-V1.md](../specifications/Cockpit-Spec-WindowContent-51215-V1.md)
**Version:** V1
**Date:** 2025-12-15
**Mode:** YOLO (Auto-implement)

---

## Overview

**Platforms:** Android (Kotlin, Compose)
**Swarm Recommended:** No (single platform, sequential phases work well)
**Estimated:** 32 tasks across 5 phases
**Timeline:** 5 days (aggressive MVP timeline with .yolo mode)

**Scope:**
- Replace placeholder content with functional WebViews and native widgets
- Implement JavaScript bridge for web-native communication
- Add template-based dynamic window sizing for Augmentalis
- Build full-featured Calculator widget
- Implement error handling, loading states, and telemetry
- Integrate across all workspace modes (Flat, Spatial, Curved)

---

## Phase Execution Strategy

### Sequential vs Parallel
- **Sequential Execution:** 5 days (8 hours/day = 40 hours)
- **Parallel Not Applicable:** Single platform (Android), no multi-platform parallelization needed
- **Approach:** Incremental implementation with continuous testing

### Dependencies
```
Phase 1 (Foundation)
    ↓
Phase 2 (WebView Integration) ← Depends on Phase 1
    ↓
Phase 3 (Calculator) ← Can run parallel with Phase 2 (independent)
    ↓
Phase 4 (Error Handling) ← Depends on Phases 2 & 3
    ↓
Phase 5 (Integration & Testing) ← Depends on all previous phases
```

---

## Phase 1: Foundation & Architecture

**Duration:** 1 day (8 hours)
**Priority:** P0 (Critical - everything depends on this)
**Goal:** Create base infrastructure for WebView management and JavaScript bridge

### Tasks

#### Task 1.1: Create Content Package Structure
**Estimate:** 30 minutes
**Description:** Setup file structure for new content modules
**Files to Create:**
- `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/content/`
  - `webview/` (WebView management)
  - `bridge/` (JavaScript bridge)
  - `widgets/` (Native widgets)
  - `loading/` (Loading states)
  - `telemetry/` (Error tracking)

**Acceptance:**
- [ ] All directories created
- [ ] Package structure matches spec

#### Task 1.2: Define WindowContent Sealed Class Extensions
**Estimate:** 1 hour
**Description:** Extend existing WindowContent to support new content types
**Files to Modify:**
- `Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/core/window/WindowContent.kt`

**Implementation:**
```kotlin
sealed class WindowContent {
    data class WebContent(
        val url: String,
        val savedState: Bundle? = null,
        val scrollX: Int = 0,
        val scrollY: Int = 0,
        val jsEnabled: Boolean = true,
        val bridgeEnabled: Boolean = false
    ) : WindowContent()

    data class NativeWidget(
        val widgetType: WidgetType,
        val state: Map<String, Any> = emptyMap()
    ) : WindowContent()

    object MockContent : WindowContent()
}

enum class WidgetType {
    CALCULATOR,
    NOTES,
    TIMER,
    STOPWATCH
}
```

**Acceptance:**
- [ ] WindowContent supports WebContent with bridge flag
- [ ] NativeWidget type defined
- [ ] Backwards compatible with existing code

#### Task 1.3: Create CockpitJsBridge Interface
**Estimate:** 2 hours
**Description:** Define JavaScript bridge API for WebView-native communication
**Files to Create:**
- `content/bridge/CockpitJsBridge.kt`
- `content/bridge/CockpitJsBridgeImpl.kt`

**Implementation:**
```kotlin
interface CockpitJsBridge {
    // Window management
    fun requestSize(template: String, width: Float, height: Float, aspectRatio: Float? = null)
    fun minimize()
    fun maximize()
    fun close()

    // Device features
    fun getLocation(callback: String)
    fun voiceSearch(query: String?)
    fun navigate(destination: String)
    fun shareContent(url: String, title: String)

    // Workspace integration
    fun openWindow(url: String, title: String, windowType: String)
    fun sendMessage(targetWindowId: String, message: String)

    // System
    fun log(level: String, message: String)
    fun reportError(error: String, stack: String)
}

@JavascriptInterface
class CockpitJsBridgeImpl(
    private val windowId: String,
    private val viewModel: WorkspaceViewModel,
    private val context: Context
) : CockpitJsBridge {
    // Implementation of all methods with @JavascriptInterface annotation
}
```

**Acceptance:**
- [ ] All 12 bridge methods defined
- [ ] Methods annotated with @JavascriptInterface
- [ ] Callback mechanism for async operations
- [ ] Input validation on all methods

#### Task 1.4: Create WebViewTelemetry System
**Estimate:** 2 hours
**Description:** Setup error logging and crash reporting
**Files to Create:**
- `content/telemetry/WebViewTelemetry.kt`
- `content/telemetry/TelemetryEvent.kt`

**Implementation:**
```kotlin
class WebViewTelemetry(private val analytics: Analytics) {
    fun logPageStart(windowId: String, url: String)
    fun logPageFinish(windowId: String, url: String, duration: Long)
    fun logError(windowId: String, error: ErrorType, url: String, message: String)
    fun logJsError(windowId: String, error: String, stackTrace: String)
    fun logBridgeCall(method: String, args: Map<String, Any>)
}

sealed class TelemetryEvent {
    data class PageLoadStart(val windowId: String, val url: String) : TelemetryEvent()
    data class PageLoadFinish(val windowId: String, val url: String, val duration: Long) : TelemetryEvent()
    data class PageLoadError(val windowId: String, val error: ErrorType, val message: String) : TelemetryEvent()
    data class JsError(val windowId: String, val error: String, val stackTrace: String) : TelemetryEvent()
    data class BridgeCall(val method: String, val args: Map<String, Any>) : TelemetryEvent()
}
```

**Acceptance:**
- [ ] All event types logged
- [ ] Analytics integration (Firebase or custom)
- [ ] PII scrubbing implemented
- [ ] No performance impact (async logging)

#### Task 1.5: Create ManagedWebView Base Class
**Estimate:** 2.5 hours
**Description:** WebView wrapper with lifecycle, security, and telemetry
**Files to Create:**
- `content/webview/ManagedWebView.kt`
- `content/webview/WebViewLifecycleManager.kt`

**Implementation:**
```kotlin
class ManagedWebView(
    context: Context,
    private val windowId: String,
    private val bridge: CockpitJsBridge?,
    private val telemetry: WebViewTelemetry
) : WebView(context) {

    init {
        configureSecureSettings()
        if (bridge != null) injectJsBridge(bridge)
        setupTelemetry()
        setupWebViewClient()
        setupWebChromeClient()
    }

    private fun configureSecureSettings() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
    }

    private fun injectJsBridge(bridge: CockpitJsBridge) {
        addJavascriptInterface(bridge, "cockpit")
    }

    fun pauseContent() {
        onPause()
        pauseTimers()
    }

    fun resumeContent() {
        onResume()
        resumeTimers()
    }

    fun saveWebViewState(): Bundle {
        return Bundle().also { saveState(it) }
    }

    fun restoreWebViewState(state: Bundle) {
        restoreState(state)
    }
}
```

**Acceptance:**
- [ ] Secure WebView configuration
- [ ] JavaScript bridge injection
- [ ] Lifecycle methods (pause/resume/destroy)
- [ ] State persistence support
- [ ] Telemetry integration

---

## Phase 2: WebView Integration

**Duration:** 1.5 days (12 hours)
**Priority:** P0 (Critical)
**Goal:** Wire all web windows with actual content loading

### Tasks

#### Task 2.1: Create WebViewRenderer for Flat Mode
**Estimate:** 2 hours
**Description:** Render live WebViews in Flat workspace mode
**Files to Create:**
- `rendering/WebViewRenderer.kt`

**Implementation:**
```kotlin
@Composable
fun WebViewContent(
    window: AppWindow,
    modifier: Modifier = Modifier,
    viewModel: WorkspaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val webView = remember(window.id) {
        createManagedWebView(context, window, viewModel)
    }

    DisposableEffect(window.id) {
        onDispose {
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        update = { view ->
            (window.content as? WindowContent.WebContent)?.let { content ->
                if (view.url != content.url) {
                    view.loadUrl(content.url)
                }
            }
        },
        modifier = modifier
    )
}
```

**Acceptance:**
- [ ] WebView renders in WindowCard
- [ ] Touch events work
- [ ] WebView fills content area

#### Task 2.2: Implement Augmentalis Window with Advanced Bridge
**Estimate:** 3 hours
**Description:** Special handling for Augmentalis with template-based sizing
**Files to Create:**
- `content/webview/AugmentalisWindow.kt`

**Implementation:**
```kotlin
class AugmentalisWindow(
    context: Context,
    windowId: String,
    viewModel: WorkspaceViewModel,
    telemetry: WebViewTelemetry
) : ManagedWebView(
    context,
    windowId,
    AugmentalisBridge(windowId, viewModel),
    telemetry
) {
    init {
        loadUrl("https://www.augmentalis.com")
    }
}

class AugmentalisBridge(
    private val windowId: String,
    private val viewModel: WorkspaceViewModel
) : CockpitJsBridgeImpl(windowId, viewModel, context) {

    @JavascriptInterface
    override fun requestSize(template: String, width: Float, height: Float, aspectRatio: Float?) {
        val clampedWidth = width.coerceIn(0.4f, 2.0f)
        val clampedHeight = height.coerceIn(0.3f, 1.5f)

        viewModel.updateWindowSize(windowId, clampedWidth, clampedHeight, animationDuration = 300)
    }
}
```

**Acceptance:**
- [ ] Augmentalis loads at www.augmentalis.com
- [ ] JS bridge accessible via window.cockpit
- [ ] requestSize() triggers animated window resize
- [ ] Size clamped to min/max bounds

#### Task 2.3: Wire Google, Weather, Maps WebViews
**Estimate:** 2 hours
**Description:** Standard WebView integration with bridge
**Files to Modify:**
- Update WindowCard.kt to render WebViews based on WindowContent type

**Implementation:**
```kotlin
@Composable
fun WindowCard(
    window: AppWindow,
    // ... existing params
) {
    // ... existing code

    when (val content = window.content) {
        is WindowContent.WebContent -> {
            WebViewContent(
                window = window,
                modifier = Modifier.fillMaxSize()
            )
        }
        is WindowContent.NativeWidget -> {
            NativeWidgetContent(
                widget = content.widgetType,
                state = content.state,
                modifier = Modifier.fillMaxSize()
            )
        }
        is WindowContent.MockContent -> {
            // Existing placeholder
        }
    }
}
```

**Acceptance:**
- [ ] Google loads at google.com
- [ ] Weather loads at weather.com
- [ ] Maps loads at maps.google.com
- [ ] All have JS bridge access

#### Task 2.4: Implement Loading Indicators
**Estimate:** 2 hours
**Description:** Show progress during page load
**Files to Create:**
- `content/loading/LoadingIndicator.kt`
- `content/loading/LoadingState.kt`

**Implementation:**
```kotlin
@Composable
fun WebViewLoadingIndicator(
    progress: Float,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LinearProgressIndicator(
            progress = progress,
            modifier = modifier
                .fillMaxWidth()
                .height(2.dp),
            color = OceanTheme.primary,
            trackColor = OceanTheme.primary.copy(alpha = 0.2f)
        )
    }
}

data class LoadingState(
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val error: ErrorType? = null
)
```

**Acceptance:**
- [ ] Progress bar visible during load
- [ ] Smooth progress 0% → 100%
- [ ] Minimum display time 300ms
- [ ] Dismisses after page finishes

#### Task 2.5: Update Spatial Mode Bitmap Capture
**Estimate:** 1.5 hours
**Description:** Ensure WebView bitmaps capture correctly
**Files to Modify:**
- `rendering/SpatialWebViewManager.kt`

**Changes:**
- Update capture frequency to 30 FPS (currently 500ms → 33ms)
- Ensure bitmap includes top bar with color
- Handle WebView that hasn't loaded yet

**Acceptance:**
- [ ] Bitmaps show actual web content
- [ ] 30 FPS update rate
- [ ] Colored top bar included

#### Task 2.6: Update Curved Mode WebView Integration
**Estimate:** 1.5 hours
**Description:** Ensure WebViews work in ViewPager2 curved mode
**Files to Modify:**
- `curved/WindowViewPagerAdapter.kt`

**Changes:**
- Replace basic URL loading with ManagedWebView
- Ensure bitmap capture includes WebView content
- Handle visibility toggling

**Acceptance:**
- [ ] Center window shows live WebView
- [ ] Side windows show curved bitmap with web content
- [ ] Swipe transitions smooth

---

## Phase 3: Calculator Widget

**Duration:** 1 day (8 hours)
**Priority:** P1 (High - independent of WebView work)
**Goal:** Build full-featured native Calculator

### Tasks

#### Task 3.1: Create Calculator UI Components
**Estimate:** 2 hours
**Description:** Build Compose UI for Calculator
**Files to Create:**
- `content/widgets/calculator/CalculatorWidget.kt`
- `content/widgets/calculator/CalculatorButton.kt`
- `content/widgets/calculator/CalculatorDisplay.kt`

**Implementation:**
```kotlin
@Composable
fun CalculatorWidget(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .background(OceanTheme.backgroundStart)
            .padding(OceanTheme.spacingMedium)
    ) {
        CalculatorDisplay(
            value = state.display,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        CalculatorButtonGrid(
            onButtonClick = viewModel::handleInput,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(visible = state.showHistory) {
            CalculatorHistory(
                history = state.history,
                onHistoryItemClick = viewModel::loadFromHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}
```

**Acceptance:**
- [ ] Display shows current value
- [ ] 4x5 button grid (0-9, +, -, ×, ÷, =, C, ⌫)
- [ ] Ocean Blue themed
- [ ] Glassmorphic button styling

#### Task 3.2: Implement Calculator Logic
**Estimate:** 2 hours
**Description:** Calculation engine with expression evaluation
**Files to Create:**
- `content/widgets/calculator/CalculatorViewModel.kt`
- `content/widgets/calculator/CalculatorEngine.kt`

**Implementation:**
```kotlin
class CalculatorViewModel : ViewModel() {
    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    fun handleInput(button: CalculatorButton) {
        when (button) {
            is CalculatorButton.Number -> appendDigit(button.digit)
            is CalculatorButton.Operator -> applyOperator(button.operator)
            is CalculatorButton.Equals -> calculate()
            is CalculatorButton.Clear -> clear()
            is CalculatorButton.Backspace -> backspace()
        }
    }

    private fun calculate() {
        try {
            val result = CalculatorEngine.evaluate(_state.value.expression)
            addToHistory(_state.value.expression, result)
            _state.update { it.copy(display = result, expression = result) }
        } catch (e: ArithmeticException) {
            _state.update { it.copy(display = "Error", error = e.message) }
        }
    }
}

data class CalculatorState(
    val display: String = "0",
    val expression: String = "",
    val history: List<HistoryItem> = emptyList(),
    val showHistory: Boolean = false,
    val error: String? = null
)
```

**Acceptance:**
- [ ] All basic operations work (+, -, ×, ÷)
- [ ] Decimal support
- [ ] Error handling (division by zero, etc.)
- [ ] Expression evaluation correct

#### Task 3.3: Implement Calculator History with SQLDelight
**Estimate:** 2 hours
**Description:** Persist calculation history to database
**Files to Create:**
- `content/widgets/calculator/CalculatorHistory.sq` (SQLDelight schema)
- `content/widgets/calculator/CalculatorRepository.kt`

**Schema:**
```sql
CREATE TABLE calculator_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    expression TEXT NOT NULL,
    result TEXT NOT NULL,
    timestamp INTEGER NOT NULL
);

CREATE INDEX idx_calculator_timestamp ON calculator_history(timestamp DESC);

selectRecent:
SELECT * FROM calculator_history
ORDER BY timestamp DESC
LIMIT 10;

insert:
INSERT INTO calculator_history (expression, result, timestamp)
VALUES (?, ?, ?);

deleteOld:
DELETE FROM calculator_history
WHERE id NOT IN (
    SELECT id FROM calculator_history
    ORDER BY timestamp DESC
    LIMIT 10
);
```

**Acceptance:**
- [ ] Last 10 calculations stored
- [ ] History persists across app restarts
- [ ] Oldest entries auto-deleted
- [ ] Fast query performance

#### Task 3.4: Add Voice Input Support
**Estimate:** 1.5 hours
**Description:** "Calculate [expression]" voice command
**Files to Modify:**
- Voice command handler (if exists)
- CalculatorViewModel

**Implementation:**
```kotlin
fun handleVoiceCommand(command: String) {
    val calculationPattern = Regex("calculate (.+)")
    val match = calculationPattern.find(command.lowercase())

    if (match != null) {
        val expression = match.groupValues[1]
        val parsed = parseNaturalLanguageExpression(expression)
        _state.update { it.copy(expression = parsed) }
        calculate()
    }
}
```

**Acceptance:**
- [ ] Voice commands trigger calculations
- [ ] Natural language parsing (e.g., "five plus three")
- [ ] Results announced via TTS (optional)

#### Task 3.5: Wire Calculator into Workspace
**Estimate:** 30 minutes
**Description:** Integrate Calculator widget into window rendering
**Files to Modify:**
- `WindowCard.kt` (already modified in 2.3)

**Acceptance:**
- [ ] Calculator shows in window with WIDGET type
- [ ] Calculator persists state when window minimized
- [ ] Calculator works in all workspace modes

---

## Phase 4: Error Handling & Polish

**Duration:** 1 day (8 hours)
**Priority:** P1 (High)
**Goal:** Robust error handling and user-friendly error UI

### Tasks

#### Task 4.1: Create Error Page Composables
**Estimate:** 2 hours
**Description:** User-friendly error UI for WebView failures
**Files to Create:**
- `content/loading/ErrorPage.kt`
- `content/loading/ErrorType.kt`

**Implementation:**
```kotlin
enum class ErrorType {
    NO_INTERNET,
    SSL_ERROR,
    TIMEOUT,
    NOT_FOUND,
    SERVER_ERROR,
    UNKNOWN
}

@Composable
fun WebViewErrorPage(
    error: ErrorType,
    url: String,
    onRetry: () -> Unit,
    onOpenInBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OceanTheme.backgroundStart)
            .padding(OceanTheme.spacingXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = error.icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = OceanTheme.textTertiary
        )

        Spacer(modifier = Modifier.height(OceanTheme.spacingMedium))

        Text(
            text = error.title,
            style = MaterialTheme.typography.headlineSmall,
            color = OceanTheme.textPrimary
        )

        Text(
            text = error.description,
            style = MaterialTheme.typography.bodyMedium,
            color = OceanTheme.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(OceanTheme.spacingLarge))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = OceanTheme.primary
            )
        ) {
            Text("Retry")
        }

        TextButton(onClick = onOpenInBrowser) {
            Text("Open in Browser")
        }
    }
}
```

**Acceptance:**
- [ ] All 6 error types have UI
- [ ] Retry button works
- [ ] Open in browser works
- [ ] Ocean Blue themed

#### Task 4.2: Implement WebViewClient Error Handling
**Estimate:** 2 hours
**Description:** Catch and categorize WebView errors
**Files to Create:**
- `content/webview/WebViewErrorHandler.kt`

**Implementation:**
```kotlin
class CockpitWebViewClient(
    private val windowId: String,
    private val telemetry: WebViewTelemetry,
    private val onError: (ErrorType, String) -> Unit,
    private val onLoadingProgress: (Float) -> Unit
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        telemetry.logPageStart(windowId, url ?: "")
        onLoadingProgress(0f)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        telemetry.logPageFinish(windowId, url ?: "", duration = 0) // TODO: track duration
        onLoadingProgress(1f)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)

        val errorType = categorizeError(error)
        val url = request?.url?.toString() ?: ""

        telemetry.logError(windowId, errorType, url, error?.description?.toString() ?: "")
        onError(errorType, url)
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        telemetry.logError(windowId, ErrorType.SSL_ERROR, view?.url ?: "", error?.toString() ?: "")
        onError(ErrorType.SSL_ERROR, view?.url ?: "")
        handler?.cancel()
    }

    private fun categorizeError(error: WebResourceError?): ErrorType {
        return when (error?.errorCode) {
            ERROR_HOST_LOOKUP -> ErrorType.NO_INTERNET
            ERROR_TIMEOUT -> ErrorType.TIMEOUT
            ERROR_FILE_NOT_FOUND -> ErrorType.NOT_FOUND
            else -> ErrorType.UNKNOWN
        }
    }
}
```

**Acceptance:**
- [ ] All WebView errors caught
- [ ] Errors categorized correctly
- [ ] Telemetry logged
- [ ] Error UI triggered

#### Task 4.3: Implement Error Recovery & Retry
**Estimate:** 1.5 hours
**Description:** Automatic retry logic for transient failures
**Files to Create:**
- `content/webview/WebViewRetryManager.kt`

**Implementation:**
```kotlin
class WebViewRetryManager(
    private val maxAttempts: Int = 3,
    private val backoffMillis: Long = 1000
) {
    private val attemptCounts = mutableMapOf<String, Int>()

    fun shouldRetry(url: String, error: ErrorType): Boolean {
        if (!error.isRetryable) return false

        val attempts = attemptCounts[url] ?: 0
        return attempts < maxAttempts
    }

    suspend fun retryWithBackoff(url: String, attempt: Int, action: suspend () -> Unit) {
        val attempts = attemptCounts.getOrPut(url) { 0 }
        attemptCounts[url] = attempts + 1

        delay(backoffMillis * attempt)
        action()
    }

    fun resetAttempts(url: String) {
        attemptCounts.remove(url)
    }
}

val ErrorType.isRetryable: Boolean
    get() = when (this) {
        ErrorType.TIMEOUT, ErrorType.NO_INTERNET -> true
        else -> false
    }
```

**Acceptance:**
- [ ] Automatic retry for transient errors
- [ ] Max 3 attempts with backoff
- [ ] Success resets attempt count
- [ ] Non-retryable errors show immediately

#### Task 4.4: Integrate Firebase Crashlytics
**Estimate:** 1.5 hours
**Description:** Setup crash and error reporting
**Files to Modify:**
- `build.gradle.kts` (add Firebase dependencies)
- `content/telemetry/WebViewTelemetry.kt`

**Dependencies:**
```kotlin
dependencies {
    implementation("com.google.firebase:firebase-crashlytics:18.6.0")
    implementation("com.google.firebase:firebase-analytics:21.5.0")
}
```

**Implementation:**
```kotlin
class WebViewTelemetry(private val crashlytics: FirebaseCrashlytics) {
    fun logError(windowId: String, error: ErrorType, url: String, message: String) {
        crashlytics.log("WebView Error: $windowId, $error, $url")
        crashlytics.recordException(
            WebViewException(windowId, error, url, message)
        )
    }
}
```

**Acceptance:**
- [ ] Firebase Crashlytics configured
- [ ] All WebView errors reported
- [ ] PII scrubbed from logs
- [ ] Dashboard accessible

#### Task 4.5: Add Loading Shimmer Effect
**Estimate:** 1 hour
**Description:** Shimmer placeholder during initial load
**Files to Create:**
- `content/loading/ShimmerEffect.kt`

**Implementation:**
```kotlin
@Composable
fun WebViewShimmer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanTheme.glassSurface.copy(alpha = shimmerAlpha))
    )
}
```

**Acceptance:**
- [ ] Shimmer shows for first 500ms
- [ ] Smooth animation
- [ ] Transitions to progress bar

---

## Phase 5: Integration & Testing

**Duration:** 1.5 days (12 hours)
**Priority:** P1 (High)
**Goal:** Integrate everything, test across all modes, fix bugs

### Tasks

#### Task 5.1: Integration Testing - Flat Mode
**Estimate:** 2 hours
**Description:** Test all windows in Flat workspace mode
**Test Cases:**
- [ ] Augmentalis loads and bridge accessible
- [ ] Google search works
- [ ] Calculator performs operations
- [ ] Weather shows content
- [ ] Maps loads correctly
- [ ] Window resize works
- [ ] Window minimize/maximize works
- [ ] Error pages display on network failure

#### Task 5.2: Integration Testing - Spatial Mode
**Estimate:** 2 hours
**Description:** Test all windows in Spatial 3D mode
**Test Cases:**
- [ ] All 5 windows visible in Arc layout
- [ ] WebView bitmaps capture correctly
- [ ] Bitmap updates at 30 FPS
- [ ] Calculator widget renders
- [ ] Window scaling works (1.2m x 0.9m)
- [ ] Touch events work on windows
- [ ] Close buttons work

#### Task 5.3: Integration Testing - Curved Mode
**Estimate:** 2 hours
**Description:** Test all windows in Curved ViewPager2 mode
**Test Cases:**
- [ ] Center window shows live WebView
- [ ] Side windows show curved bitmaps
- [ ] Swipe between windows smooth
- [ ] WebView interactivity works on center
- [ ] Bitmap captures web content correctly
- [ ] All 5 windows accessible via swipe

#### Task 5.4: Performance Testing
**Estimate:** 2 hours
**Description:** Verify performance targets met
**Metrics to Verify:**
- [ ] 60 FPS sustained in all modes
- [ ] Memory usage < 500 MB with 5 windows
- [ ] Augmentalis loads in < 2s on 4G
- [ ] No memory leaks (LeakCanary)
- [ ] WebView pause/resume works correctly
- [ ] Battery drain acceptable

#### Task 5.5: Accessibility Testing
**Estimate:** 1.5 hours
**Description:** Verify TalkBack and accessibility
**Test Cases:**
- [ ] WebView content accessible to TalkBack
- [ ] Calculator buttons labeled correctly
- [ ] Touch targets >= 48dp
- [ ] Color contrast meets WCAG AA
- [ ] Voice commands work

#### Task 5.6: Error Scenario Testing
**Estimate:** 1.5 hours
**Description:** Test all error handling paths
**Test Cases:**
- [ ] Airplane mode shows "No Internet" error
- [ ] SSL errors show certificate warning
- [ ] Retry button reloads content
- [ ] Open in browser launches external app
- [ ] Telemetry logs all errors
- [ ] App doesn't crash on WebView errors

#### Task 5.7: Bug Fixes & Polish
**Estimate:** 1 hour
**Description:** Fix any issues found during testing
**Activities:**
- Fix bugs discovered in integration testing
- Polish UI/UX rough edges
- Optimize performance hotspots
- Update documentation

---

## Task Summary

### By Phase
| Phase | Tasks | Hours |
|-------|-------|-------|
| Phase 1: Foundation | 5 | 8 |
| Phase 2: WebView Integration | 6 | 12 |
| Phase 3: Calculator Widget | 5 | 8 |
| Phase 4: Error Handling | 5 | 8 |
| Phase 5: Testing & Integration | 7 | 12 |
| **Total** | **28** | **48** |

### By Priority
| Priority | Tasks | Hours |
|----------|-------|-------|
| P0 (Critical) | 11 | 22 |
| P1 (High) | 17 | 26 |

---

## Time Estimates

### Sequential Execution
- **Total:** 48 hours (6 days @ 8 hours/day)
- **With overhead:** 5 days (aggressive timeline)

### Critical Path
```
Foundation (8h) → WebView Integration (12h) → Error Handling (8h) → Testing (12h) = 40 hours
                      ↓ (parallel)
                Calculator Widget (8h)
```

### Parallelization Opportunities
- **Phase 2 + Phase 3 can overlap**: Calculator is independent of WebView work
- **Savings:** 8 hours (1 day)
- **Adjusted Total:** 40 hours (5 days)

---

## Dependencies

### Internal Dependencies
- ✅ `Common/Cockpit` module (AppWindow, WindowContent, Vector3D)
- ✅ `WorkspaceViewModel` for window management
- ✅ `OceanTheme` for styling
- ✅ Existing rendering pipeline (Flat, Spatial, Curved)

### External Dependencies
- **AndroidX WebKit**: 1.8.0+ (WebView features)
- **Firebase Crashlytics**: 18.6.0+ (error reporting)
- **Firebase Analytics**: 21.5.0+ (telemetry)
- **SQLDelight**: 2.0.0+ (Calculator history)
- **LeakCanary**: 2.12+ (memory leak detection, debug only)

**Action:** Add to `build.gradle.kts`:
```kotlin
dependencies {
    implementation("androidx.webkit:webkit:1.8.0")
    implementation("com.google.firebase:firebase-crashlytics:18.6.0")
    implementation("com.google.firebase:firebase-analytics:21.5.0")
    implementation("app.cash.sqldelight:android-driver:2.0.0")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
```

---

## Risk Assessment

### High Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| WebView crashes app | High | Try-catch all WebView operations, error boundaries |
| Memory leaks from WebViews | High | Proper lifecycle management, LeakCanary testing, dispose on window close |
| Performance degradation with 5 WebViews | High | Pause inactive WebViews, bitmap caching for spatial mode, lazy loading |

### Medium Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| Template sizing breaks layout | Medium | Clamp size ranges (0.4-2.0m), fallback to default size |
| JS bridge security vulnerabilities | Medium | Input validation, rate limiting, security code review |
| Calculator history database errors | Medium | Schema validation, migration tests, fallback to in-memory |

---

## Success Criteria

### Functional Requirements
- [ ] All 5 windows load actual content (not placeholders)
- [ ] Augmentalis loads with working JS bridge
- [ ] Template-based window resizing works
- [ ] Calculator performs all operations correctly
- [ ] Calculator history persists
- [ ] Error pages display for all error types
- [ ] Loading indicators show during page load

### Performance Requirements
- [ ] 60 FPS sustained in all modes
- [ ] Memory usage < 500 MB with 5 windows
- [ ] Augmentalis loads in < 2s on 4G
- [ ] No memory leaks detected

### Quality Requirements
- [ ] Zero WebView-related crashes
- [ ] All errors logged to telemetry
- [ ] TalkBack accessibility works
- [ ] Security audit passed

---

## Post-Implementation Checklist

### Code Quality
- [ ] All code follows Kotlin style guide
- [ ] SOLID principles applied
- [ ] No compiler warnings
- [ ] TODO comments resolved or tracked

### Documentation
- [ ] API documentation for CockpitJsBridge
- [ ] Architecture diagram updated
- [ ] README updated with new features
- [ ] Troubleshooting guide created

### Testing
- [ ] Unit tests for Calculator logic
- [ ] Integration tests for WebView lifecycle
- [ ] UI tests for all workspace modes
- [ ] Manual testing completed

### Deployment
- [ ] Build APK and test on physical device
- [ ] Performance profiling completed
- [ ] Memory profiling completed
- [ ] Crashlytics dashboard verified

---

## Next Steps After Implementation

1. **User Testing**: Deploy to alpha testers
2. **Performance Monitoring**: Watch Crashlytics for errors
3. **Iteration**: Fix bugs based on telemetry data
4. **Feature Expansion**: Add more native widgets (Notes, Timer)
5. **Augmentalis Integration**: Coordinate with web team for template API

---

**Plan Status:** Ready for Implementation
**Mode:** YOLO (Auto-implement after plan approval)
**Next Command:** `/i.implement` (will execute automatically in .yolo mode)
