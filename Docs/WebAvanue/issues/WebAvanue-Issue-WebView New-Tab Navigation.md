# WebView New-Tab Navigation + Real-time Settings Updates

## Scope

This document captures the fixes and changes made in:

- `WebViewContainer.android.kt`
- `BrowserScreen.kt`

The changes address:

1. Opening every user-clicked link in a **new tab**
2. Ensuring **BrowserSettings update in real time** inside the WebViewContainer

---

## Summary of Changes

### A. New Tab on Link Click

- Intercept user-initiated navigations in `WebViewClient.shouldOverrideUrlLoading`
- Prevent the current WebView from loading the URL
- Forward the URL to BrowserScreen to create a new tab

### B. Real-time Settings Update Fix

- Settings were not applying if updated before WebView creation
- Fixed by re-applying settings whenever **either settings OR WebView reference changes**
- ViewModel remains in `BrowserScreen`; only data flows into KMP container

---

## A) Changes in `WebViewContainer.android.kt`

### 1. Add new callback to composable API

```kotlin
onOpenInNewTab: (String) -> Unit
```

Updated function signature (excerpt):

```kotlin
actual fun WebViewContainer(
    tabId: String,
    url: String,
    controller: WebViewController?,
    onUrlChange: (String) -> Unit,
    onOpenInNewTab: (String) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    ...
    settings: BrowserSettings?,
    ...
)
```

---

### 2. Intercept link clicks in `WebViewClient`

```kotlin
override fun shouldOverrideUrlLoading(
    view: WebView?,
    request: WebResourceRequest?
): Boolean {
    val newUrl = request?.url?.toString() ?: return false

    // Only intercept user-initiated navigations
    if (request.isForMainFrame && request.hasGesture()) {
        println("🔗 Opening link in new tab: $newUrl")
        onOpenInNewTab(newUrl)
        return true // Prevent current WebView from loading
    }

    return false
}
```

**Why this works**

- `hasGesture()` → filters out JS redirects
- `isForMainFrame` → avoids subresources
- `return true` → cancels navigation in current tab

---

### 3. Fix real-time settings updates

#### Root Cause

If `settings` updated while `webView == null`, the previous `LaunchedEffect(settings)` ran once and never re-ran when WebView became available.

#### Fix

Re-run settings application when **either** settings or WebView changes.

```kotlin
val latestSettings by rememberUpdatedState(settings)

LaunchedEffect(webView, latestSettings) {
    val view = webView ?: return@LaunchedEffect
    val s = latestSettings ?: return@LaunchedEffect

    settingsStateMachine.requestUpdate(s) { settingsToApply ->
        SettingsApplicator().applySettings(view, settingsToApply)
    }
}
```

**Important**

- Do NOT include `settings` inside `key(tabId)`
- Recreating WebView would break history/session pooling

---

## B) Changes in `BrowserScreen.kt`

Wire the new-tab callback to `TabViewModel`:

```kotlin
WebViewContainer(
    tabId = tabState.tab.id,
    url = tabState.tab.url,
    controller = webViewController,
    onUrlChange = { newUrl ->
        tabViewModel.updateTabUrl(tabState.tab.id, newUrl)
        urlInput = newUrl
    },
    onOpenInNewTab = { newUrl ->
        tabViewModel.createTab(
            initialUrl = newUrl,
            isDesktopMode = tabState.tab.isDesktopMode
        )
    },
    ...
    settings = settings,
    isDesktopMode = tabState.tab.isDesktopMode,
    modifier = Modifier.fillMaxSize()
)
```

> Ensure `TabViewModel.createTab()` supports an initial URL.