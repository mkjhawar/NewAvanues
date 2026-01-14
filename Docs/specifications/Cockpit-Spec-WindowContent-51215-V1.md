# Cockpit MVP - Window Content Integration Specification

**Project:** NewAvanues-Cockpit
**Feature:** Window Content Wiring & WebView Integration
**Version:** V1
**Date:** 2025-12-15
**Status:** Draft
**Platform:** Android (Kotlin, Compose)

---

## Executive Summary

This specification defines the integration of actual content into Cockpit MVP windows, replacing placeholder implementations with fully functional WebViews and native widgets. The system supports five window types (Augmentalis, Google, Calculator, Weather, Maps) across three workspace modes (Flat, Spatial, Curved). All windows receive advanced JavaScript bridge capabilities for native feature access, with Augmentalis receiving special template-based dynamic sizing support.

**Key Capabilities:**
- Advanced WebView integration with JavaScript bridges for all web windows
- Full-featured native Calculator implementation
- Template-based dynamic window sizing for WebAvanue
- Error telemetry and crash reporting
- Unified content loading across all workspace modes

---

## Problem Statement

### Current State
- Windows show placeholder content or basic URL loading
- No error handling or loading states
- WebAvanue has no native integration
- Calculator is mock placeholder
- No JavaScript bridge for web-native communication
- Window sizing is static and cannot adapt to content

### Desired State
- All windows load actual functional content with proper error handling
- WebAvanue integrates deeply via JavaScript bridge with template-based sizing
- Calculator provides full native implementation with history and persistence
- Robust error handling with telemetry for debugging
- Dynamic window sizing based on content requirements
- Consistent behavior across Flat, Spatial, and Curved workspace modes

### Business Value
- **User Experience**: Real functionality instead of demos/placeholders
- **Augmentalis Integration**: Native-quality experience in WebView
- **Reliability**: Error handling prevents app crashes from content failures
- **Debuggability**: Telemetry enables quick issue resolution
- **Flexibility**: Dynamic sizing supports varied use cases

---

## Functional Requirements

### FR-1: WebView Content Loading

**FR-1.1: Augmentalis Advanced Integration**
- **Priority**: P0 (Critical)
- **Description**: Load Augmentalis website with JavaScript bridge and template-based sizing
- **Implementation**:
  - Create `AugmentalisWindow` class extending `WebViewWindow`
  - Inject `CockpitJsBridge` interface into WebView
  - Support templates: `dashboard`, `detail`, `editor`, `settings`
  - Each template defines preferred window dimensions (width, height, aspect ratio)
  - Bridge API: `window.cockpit.requestSize(template, dimensions)`
  - Native responds by updating `AppWindow.widthMeters` and `heightMeters`
- **Acceptance Criteria**:
  - [ ] Augmentalis loads at https://www.augmentalis.com
  - [ ] JS bridge accessible via `window.cockpit`
  - [ ] Template changes trigger window resize animation (300ms spring)
  - [ ] Minimum size: 0.4m x 0.3m, Maximum: 2.0m x 1.5m
  - [ ] Resize respects workspace layout constraints

**FR-1.2: Google WebView**
- **Priority**: P1 (High)
- **Description**: Load Google with JS bridge for search integration
- **Implementation**:
  - Standard WebView with `window.cockpit` bridge
  - Support voice search via bridge: `window.cockpit.voiceSearch()`
  - Enable JavaScript, cookies, local storage, DOM storage
- **Acceptance Criteria**:
  - [ ] Google loads at https://google.com
  - [ ] Search queries work correctly
  - [ ] Voice search triggered from bridge launches Android voice input
  - [ ] Cookies persist across sessions

**FR-1.3: Weather WebView**
- **Priority**: P1 (High)
- **Description**: Load Weather with location bridge access
- **Implementation**:
  - WebView with location permission bridge
  - Bridge API: `window.cockpit.getLocation()` returns `{lat, lon, accuracy}`
  - Auto-inject location for weather service
- **Acceptance Criteria**:
  - [ ] Weather loads at https://weather.com
  - [ ] Location permission requested on first access
  - [ ] Location data injected automatically
  - [ ] Weather updates when location changes

**FR-1.4: Maps WebView**
- **Priority**: P1 (High)
- **Description**: Load Google Maps with location and navigation bridges
- **Implementation**:
  - WebView with location + navigation bridges
  - Bridge API: `window.cockpit.navigate(destination)` triggers native navigation
  - Support voice commands: "Navigate to [place]"
- **Acceptance Criteria**:
  - [ ] Maps loads at https://maps.google.com
  - [ ] Location tracking enabled
  - [ ] Navigation requests trigger Android navigation intent
  - [ ] Voice commands route through bridge

### FR-2: Native Widget Implementation

**FR-2.1: Calculator Widget**
- **Priority**: P1 (High)
- **Description**: Full-featured native Calculator in Compose
- **Implementation**:
  - Create `CalculatorWidget` composable
  - Features: Basic arithmetic (+, -, ×, ÷), decimal support, clear/backspace
  - History: Last 10 calculations stored in `CalculatorHistory` database table
  - Layout: Standard calculator button grid (4x5)
  - Theme: Ocean Blue colors, glassmorphic buttons
- **Acceptance Criteria**:
  - [ ] All basic operations work correctly
  - [ ] History persists across app restarts
  - [ ] History accessible via swipe-up gesture
  - [ ] Keyboard input supported
  - [ ] Voice input: "Calculate [expression]" works

**FR-2.2: Future Native Widgets**
- **Priority**: P3 (Future)
- **Description**: Placeholder for Notes, Timer, etc.
- **Implementation**: Define `NativeWidget` interface for future widgets
- **Acceptance Criteria**:
  - [ ] Architecture supports adding new native widgets
  - [ ] Widget registry allows dynamic widget creation

### FR-3: JavaScript Bridge Architecture

**FR-3.1: CockpitJsBridge Interface**
- **Priority**: P0 (Critical)
- **Description**: Unified JS bridge for all WebViews
- **API Surface**:
```kotlin
interface CockpitJsBridge {
    // Window management
    fun requestSize(template: String, width: Float, height: Float, aspectRatio: Float?)
    fun minimize()
    fun maximize()
    fun close()

    // Device features
    fun getLocation(): Location
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
```
- **Implementation**:
  - Create `CockpitJsBridgeImpl` class
  - Inject into WebView via `addJavascriptInterface(bridge, "cockpit")`
  - All methods annotated with `@JavascriptInterface`
  - Results returned via callbacks: `window.cockpitCallback(requestId, result)`
- **Acceptance Criteria**:
  - [ ] All API methods accessible from JS
  - [ ] Async operations return via callbacks
  - [ ] Errors propagate to JS with proper error objects
  - [ ] Bridge injected before page load (avoid race conditions)

**FR-3.2: WebView Configuration**
- **Priority**: P0 (Critical)
- **Description**: Secure, performant WebView settings
- **Settings**:
  - `javaScriptEnabled = true`
  - `domStorageEnabled = true`
  - `databaseEnabled = true`
  - `allowFileAccess = false` (security)
  - `allowContentAccess = false` (security)
  - `allowFileAccessFromFileURLs = false` (security)
  - `allowUniversalAccessFromFileURLs = false` (security)
  - `mixedContentMode = MIXED_CONTENT_NEVER_ALLOW` (security)
  - `cacheMode = LOAD_DEFAULT` (standard caching)
  - `setSupportZoom(true)` with `builtInZoomControls = true`
- **Acceptance Criteria**:
  - [ ] All security settings enforced
  - [ ] WebView passes security audit
  - [ ] Caching works offline
  - [ ] No console warnings about insecure content

### FR-4: Loading States & Error Handling

**FR-4.1: Loading Indicators**
- **Priority**: P1 (High)
- **Description**: Show progress during content loading
- **Implementation**:
  - Overlay `LinearProgressIndicator` at top of window (Ocean Blue)
  - WebViewClient callbacks: `onPageStarted`, `onPageFinished`, `onProgressChanged`
  - Show loading shimmer for first 500ms, then progress bar
  - Minimum display time: 300ms (avoid flicker)
- **Acceptance Criteria**:
  - [ ] Progress bar appears on URL load
  - [ ] Progress updates smoothly 0% → 100%
  - [ ] Loading overlay dismisses after page finishes
  - [ ] Shimmer effect for initial loads

**FR-4.2: Error Pages**
- **Priority**: P1 (High)
- **Description**: User-friendly error UI when content fails to load
- **Error Types**:
  - `NO_INTERNET`: "No Internet Connection"
  - `SSL_ERROR`: "Security Certificate Problem"
  - `TIMEOUT`: "Page took too long to load"
  - `NOT_FOUND`: "Page not found (404)"
  - `SERVER_ERROR`: "Server error (5xx)"
  - `UNKNOWN`: "Failed to load page"
- **UI Components**:
  - Icon (error-specific)
  - Title (error type)
  - Description (user-friendly explanation)
  - Primary action: "Retry" button
  - Secondary action: "Open in browser" button
- **Implementation**:
  - Create `ErrorPageComposable`
  - WebViewClient `onReceivedError` callback triggers error UI
  - Error state stored in `WindowState.error: ErrorType?`
  - Retry clears error and reloads URL
- **Acceptance Criteria**:
  - [ ] All error types display correct UI
  - [ ] Retry button reloads content
  - [ ] Open in browser launches external browser
  - [ ] Error telemetry sent for all errors

**FR-4.3: Error Telemetry**
- **Priority**: P2 (Medium)
- **Description**: Log errors for debugging and monitoring
- **Implementation**:
  - Create `WebViewTelemetry` class
  - Log events: `page_load_start`, `page_load_finish`, `page_load_error`, `js_error`, `bridge_call`
  - Fields: `windowId`, `url`, `errorType`, `errorMessage`, `stackTrace`, `timestamp`, `userAgent`
  - Send to Firebase Crashlytics or custom analytics
- **Acceptance Criteria**:
  - [ ] All WebView errors logged
  - [ ] Logs include full context (URL, window, user)
  - [ ] Telemetry doesn't impact performance
  - [ ] PII scrubbed from logs

### FR-5: Content Lifecycle Management

**FR-5.1: WebView Lifecycle**
- **Priority**: P1 (High)
- **Description**: Proper WebView pause/resume/destroy
- **Implementation**:
  - `onPause()`: Pause WebView, stop media, freeze JS timers
  - `onResume()`: Resume WebView, restart timers
  - `onDestroy()`: Clear cache (optional), destroy WebView
  - Handle memory leaks: null out WebView parent, clear callbacks
- **Acceptance Criteria**:
  - [ ] WebViews pause when window hidden/minimized
  - [ ] WebViews resume when window visible
  - [ ] No memory leaks on window close
  - [ ] Audio stops when window minimized

**FR-5.2: Content State Persistence**
- **Priority**: P2 (Medium)
- **Description**: Save/restore WebView scroll position and state
- **Implementation**:
  - Save: `webView.saveState(Bundle)` on window minimize/app background
  - Restore: `webView.restoreState(Bundle)` on window restore
  - Store in `WindowContent.WebContent.savedState: Bundle?`
  - Persist to database for cross-session restoration
- **Acceptance Criteria**:
  - [ ] Scroll position preserved when switching windows
  - [ ] Form data preserved (non-sensitive only)
  - [ ] Back/forward history preserved
  - [ ] State survives app restart (optional)

### FR-6: Workspace Mode Integration

**FR-6.1: Flat Mode WebView**
- **Priority**: P1 (High)
- **Description**: WebView in traditional 2D grid layout
- **Implementation**:
  - Use existing `WindowCard` with embedded WebView
  - WebView inside `Box` with `Modifier.fillMaxSize()`
  - Standard Android View interop via `AndroidView`
- **Acceptance Criteria**:
  - [ ] WebView fills window content area
  - [ ] Touch events work correctly
  - [ ] Window resize updates WebView dimensions

**FR-6.2: Spatial Mode WebView**
- **Priority**: P1 (High)
- **Description**: WebView bitmaps for 3D spatial projection
- **Implementation**:
  - Keep existing `SpatialWebViewManager`
  - Update bitmap capture to 30 FPS for smoother updates
  - Cache bitmaps, recapture on window focus or 5-second interval
- **Acceptance Criteria**:
  - [ ] Bitmaps render correctly in spatial projection
  - [ ] Content updates visible within 5 seconds
  - [ ] No performance degradation with 5 windows

**FR-6.3: Curved Mode WebView**
- **Priority**: P1 (High)
- **Description**: Live WebView center, curved bitmaps for sides
- **Implementation**:
  - Keep existing `WindowViewPagerAdapter` structure
  - Center page: Live WebView (interactive)
  - Side pages: Bitmap snapshot with curve transformation
  - Recapture bitmap when page scrolls to side
- **Acceptance Criteria**:
  - [ ] Center window fully interactive
  - [ ] Side windows show curved preview
  - [ ] Smooth transition when swiping between windows

---

## Non-Functional Requirements

### NFR-1: Performance

**NFR-1.1: Load Time**
- Augmentalis initial load: < 2 seconds on 4G
- Google/Weather/Maps initial load: < 3 seconds on 4G
- Calculator widget instantiation: < 100ms

**NFR-1.2: Memory**
- Maximum memory per WebView: 150 MB
- Total WebView memory (5 windows): < 500 MB
- Calculator widget: < 5 MB

**NFR-1.3: Frame Rate**
- Flat mode: 60 FPS sustained
- Spatial mode: 60 FPS sustained (with bitmap updates at 30 FPS)
- Curved mode: 60 FPS sustained

**NFR-1.4: Battery**
- WebView background activity: Minimal drain (< 1% per hour)
- Paused WebViews: Zero CPU usage

### NFR-2: Security

**NFR-2.1: WebView Security**
- No file:// access
- No universal file access
- HTTPS-only content (except localhost for dev)
- Certificate pinning for www.augmentalis.com domain
- CSP headers respected

**NFR-2.2: Bridge Security**
- All bridge methods validate input
- No eval() or arbitrary code execution
- Rate limiting on bridge calls (100/second per window)
- Permission checks before sensitive operations

### NFR-3: Reliability

**NFR-3.1: Error Recovery**
- Automatic retry on transient failures (max 3 attempts)
- Graceful degradation when content unavailable
- No app crashes from WebView errors

**NFR-3.2: Crash Reporting**
- 100% of WebView crashes logged
- Stack traces captured for JS errors
- User consent for telemetry (opt-in during onboarding)

### NFR-4: Accessibility

**NFR-4.1: WebView Accessibility**
- WebView content accessible to TalkBack
- Touch targets meet minimum size (48dp)
- Color contrast ratios WCAG AA compliant

**NFR-4.2: Calculator Accessibility**
- All buttons labeled for screen readers
- Voice commands: "Calculator [operation]"
- Keyboard navigation supported

---

## Platform-Specific Details

### Android Implementation

**Technology Stack:**
- **Language**: Kotlin 1.9+
- **UI**: Jetpack Compose + AndroidView interop
- **WebView**: `android.webkit.WebView`
- **Database**: SQLDelight (for Calculator history)
- **Networking**: OkHttp (for error handling)
- **Telemetry**: Firebase Crashlytics + Custom Analytics
- **Threading**: Kotlin Coroutines

**Key Classes:**

```kotlin
// Window content abstraction
sealed class WindowContent {
    data class WebContent(
        val url: String,
        val savedState: Bundle? = null,
        val scrollX: Int = 0,
        val scrollY: Int = 0
    ) : WindowContent()

    data class NativeWidget(
        val widgetType: WidgetType
    ) : WindowContent()
}

// WebView wrapper with lifecycle
class ManagedWebView(
    context: Context,
    private val bridge: CockpitJsBridge,
    private val telemetry: WebViewTelemetry
) : WebView(context) {
    init {
        configureSecureSettings()
        injectJsBridge()
        setupTelemetry()
    }
}

// JavaScript bridge implementation
class CockpitJsBridgeImpl(
    private val windowId: String,
    private val viewModel: WorkspaceViewModel
) : CockpitJsBridge {
    @JavascriptInterface
    override fun requestSize(template: String, width: Float, height: Float, aspectRatio: Float?) {
        viewModel.updateWindowSize(windowId, width, height)
    }
    // ... other methods
}

// Calculator widget
@Composable
fun CalculatorWidget(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier) {
        Display(value = state.display)
        ButtonGrid(onButtonClick = viewModel::handleInput)
        History(items = state.history)
    }
}

// Error telemetry
class WebViewTelemetry(private val analytics: Analytics) {
    fun logError(windowId: String, error: ErrorType, url: String, message: String) {
        analytics.logEvent("webview_error", mapOf(
            "window_id" to windowId,
            "error_type" to error.name,
            "url" to url.sanitize(),
            "message" to message
        ))
    }
}
```

**File Structure:**
```
android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/
├── content/
│   ├── webview/
│   │   ├── ManagedWebView.kt
│   │   ├── WebViewLifecycleManager.kt
│   │   ├── WebViewErrorHandler.kt
│   │   └── WebViewTelemetry.kt
│   ├── bridge/
│   │   ├── CockpitJsBridge.kt
│   │   ├── CockpitJsBridgeImpl.kt
│   │   └── BridgeCallbacks.kt
│   ├── widgets/
│   │   ├── CalculatorWidget.kt
│   │   ├── CalculatorViewModel.kt
│   │   └── CalculatorHistory.kt
│   └── loading/
│       ├── LoadingIndicator.kt
│       ├── ErrorPage.kt
│       └── LoadingState.kt
└── rendering/
    ├── WebViewRenderer.kt (Flat mode)
    ├── SpatialWebViewManager.kt (existing, update)
    └── WindowViewPagerAdapter.kt (existing, update)
```

### Database Schema

**Calculator History:**
```sql
CREATE TABLE calculator_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    expression TEXT NOT NULL,
    result TEXT NOT NULL,
    timestamp INTEGER NOT NULL
);

CREATE INDEX idx_calculator_timestamp ON calculator_history(timestamp DESC);
```

---

## Acceptance Criteria

### Overall System
- [ ] All 5 windows load actual content (not placeholders)
- [ ] JavaScript bridge accessible in all WebViews
- [ ] Calculator widget fully functional
- [ ] Error handling prevents app crashes
- [ ] Loading states visible during content load
- [ ] Telemetry captures all errors

### Augmentalis Specific
- [ ] Template-based sizing works (`dashboard`, `detail`, `editor`, `settings`)
- [ ] Window resize animation smooth (300ms spring)
- [ ] JS bridge calls succeed with < 50ms latency
- [ ] Deep links from Augmentalis website open correctly

### Per-Window Tests
- [ ] **Augmentalis**: Loads at www.augmentalis.com, bridge works, resizing works
- [ ] **Google**: Loads, search works, voice search works
- [ ] **Calculator**: All operations correct, history persists
- [ ] **Weather**: Loads, location injection works
- [ ] **Maps**: Loads, location tracking works, navigation triggers

### Performance Benchmarks
- [ ] 60 FPS sustained in all modes with 5 windows
- [ ] Memory usage < 500 MB total
- [ ] Augmentalis loads in < 2s on 4G
- [ ] No memory leaks on window close (verified with LeakCanary)

### Error Handling Tests
- [ ] Airplane mode shows "No Internet" error
- [ ] Retry button reloads content
- [ ] SSL errors show certificate warning
- [ ] Telemetry logs all error types

---

## Out of Scope

### Explicitly Excluded
- **Desktop/iOS implementations**: Android-only for MVP
- **Offline-first WebView caching**: Standard browser caching only
- **WebRTC support**: No video calls/streaming in WebViews
- **Custom WebView rendering**: Using standard Android WebView, not custom engines
- **Plugin architecture**: Native widgets hardcoded for MVP, extensibility later
- **Cross-window messaging**: JS bridge supports it but not implemented in UI
- **WebView devtools integration**: No Chrome DevTools remote debugging for MVP
- **Multiple Augmentalis instances**: Only one Augmentalis window supported

### Future Enhancements (Post-MVP)
- WebView content preloading for faster perceived performance
- Smart bitmap caching strategies for spatial mode
- WebView pooling for memory efficiency
- Custom error recovery strategies per site
- A/B testing different WebView configurations
- Progressive Web App (PWA) support

---

## Dependencies

### Internal Dependencies
- `Common/Cockpit` module for `AppWindow`, `WindowContent` types
- `WorkspaceViewModel` for window management
- `OceanTheme` for consistent styling
- Existing rendering pipeline (Flat, Spatial, Curved)

### External Dependencies
- **AndroidX WebKit**: 1.8.0+ (WebView features)
- **Firebase Crashlytics**: 18.4.0+ (error reporting)
- **SQLDelight**: 2.0.0+ (Calculator history)
- **Coil**: 2.5.0+ (image loading for error pages)
- **LeakCanary**: 2.12+ (memory leak detection, debug only)

### Platform Requirements
- **Android API Level**: 26+ (Android 8.0 Oreo)
- **WebView Version**: Chrome 90+ (auto-updates via Play Store)
- **Permissions**:
  - `INTERNET` (required)
  - `ACCESS_FINE_LOCATION` (optional, for Weather/Maps)
  - `RECORD_AUDIO` (optional, for voice search)

---

## Implementation Plan Summary

### Phase 1: Foundation (Week 1)
1. Create `ManagedWebView` wrapper class
2. Implement `CockpitJsBridge` interface
3. Setup WebView telemetry
4. Create error page composables

### Phase 2: WebView Integration (Week 2)
1. Wire Augmentalis with advanced bridge
2. Implement template-based sizing
3. Wire Google, Weather, Maps with standard bridges
4. Add loading indicators

### Phase 3: Calculator Widget (Week 3)
1. Build Calculator UI in Compose
2. Implement calculation logic
3. Add history persistence
4. Wire into workspace

### Phase 4: Mode Integration (Week 4)
1. Update Flat mode renderer
2. Update Spatial mode bitmap capture
3. Update Curved mode lifecycle
4. Test cross-mode consistency

### Phase 5: Polish & Testing (Week 5)
1. Error handling edge cases
2. Performance optimization
3. Accessibility audit
4. Security review
5. End-to-end testing

---

## Testing Strategy

### Unit Tests
- JavaScript bridge method validation
- Calculator arithmetic operations
- Error type classification
- Loading state transitions

### Integration Tests
- WebView → Bridge → ViewModel flow
- Template sizing → Window resize flow
- Error → Telemetry → Recovery flow
- Lifecycle pause/resume/destroy

### UI Tests (Espresso)
- Load each window type
- Trigger JS bridge calls from WebView
- Simulate network errors
- Verify error pages display
- Test Calculator button interactions

### Manual Tests
- Load Augmentalis in all workspace modes
- Test template switching in Augmentalis
- Verify side window curved previews
- Test error scenarios (airplane mode, bad SSL)
- Memory profiling with 5 windows

---

## Risk Analysis

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| WebView crashes app | High | Medium | Catch all WebView exceptions, error boundaries |
| Memory leaks from WebViews | High | Medium | Proper lifecycle management, LeakCanary testing |
| JS bridge security vulnerabilities | Critical | Low | Input validation, rate limiting, security review |
| Template sizing breaks layout | Medium | Medium | Clamp size ranges, fallback to default |
| Performance degradation with 5 WebViews | High | High | Lazy loading, pause inactive WebViews, bitmap caching |
| Augmentalis template API changes | Medium | Low | Versioned API, backwards compatibility |

---

## Metrics & Success Criteria

### Key Performance Indicators (KPIs)
- **WebView Load Success Rate**: > 95%
- **Average Load Time**: < 3 seconds
- **Error Recovery Rate**: > 90% (users retry after error)
- **JS Bridge Call Success Rate**: > 99%
- **Memory Leak Rate**: 0% (verified via LeakCanary)
- **Crash-Free Sessions**: > 99.5%

### User Satisfaction Metrics
- **Feature Adoption**: > 80% of users interact with Augmentalis
- **Calculator Usage**: > 50% of users try Calculator
- **Error Frustration**: < 5% of users report content loading issues

---

## Appendix

### Glossary
- **Template**: Augmentalis page layout type (dashboard, detail, editor, settings)
- **Bridge**: JavaScript interface for web-native communication
- **Managed WebView**: WebView wrapper with lifecycle and telemetry
- **Window Content**: Abstract representation of window display (WebView, widget, etc.)
- **Spatial Mode**: 3D workspace with curved projection
- **Curved Mode**: ViewPager2 carousel with curved side previews

### References
- Android WebView Security Best Practices: https://developer.android.com/guide/webapps/managing-webview
- Jetpack Compose Interop: https://developer.android.com/jetpack/compose/migrate/interoperability-apis
- JavaScript Bridge Patterns: https://github.com/android/platform_frameworks_base/blob/master/core/java/android/webkit/JavascriptInterface.java

---

**Specification Status**: Ready for Review
**Next Steps**: Review → Plan (`/i.plan`) → Implementation (`/i.implement`)
