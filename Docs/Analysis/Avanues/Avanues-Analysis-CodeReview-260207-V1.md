# Avanues Consolidated App - Code Analysis

**Date:** 2026-02-07
**Branch:** 060226-1-consolidation-framework
**Scope:** Full app code review (`apps/avanues/`)
**Status:** 8 issues found (3 critical, 2 high, 3 medium)

---

## Summary

The Avanues consolidated app is architecturally sound — thin wrapper pattern over KMP modules, clean Compose UI, proper Hilt DI, and well-structured manifest. However, 8 issues were found ranging from critical bugs to incomplete implementations that violate zero-tolerance rules.

---

## Findings

| Sev | # | Category | Location | Issue |
|-----|---|----------|----------|-------|
| P0 | 1 | Bug | VoiceAvanueAccessibilityService.kt:98-100 | Race condition: `dispose()` launched in scope that's cancelled on next line |
| P0 | 2 | Stub | BootReceiver.kt:26-34 | Entire implementation is commented out — registered receiver does nothing |
| P0 | 3 | Stub | CursorOverlayService.kt:41-44 | Service starts foreground but does nothing — TODO comments instead of logic |
| P1 | 4 | State | SettingsScreen.kt:34-38 | All settings state is local `remember {}` — never persisted, resets on recomposition |
| P1 | 5 | Security | BrowserScreen.kt:179 | `javaScriptEnabled = true` without `WebChromeClient` — no JS dialog handling, no file upload |
| P2 | 6 | UX | HomeScreen.kt:36-39 | Permission status only checked once in `LaunchedEffect(Unit)` — stale after returning from Settings |
| P2 | 7 | Architecture | VoiceAvanueApplication.kt:129-134 | Singleton anti-pattern with `companion object instance` — conflicts with Hilt DI |
| P2 | 8 | Notification | RpcServerService.kt:81 | Uses system drawable `android.R.drawable.ic_menu_manage` — deprecated, no branded icon |

---

## Detailed Analysis

### P0-1: Race Condition in Accessibility Service Destroy

**File:** `VoiceAvanueAccessibilityService.kt:98-103`

```kotlin
override fun onDestroy() {
    serviceScope.launch { voiceOSCore?.dispose() }  // <-- launched in scope...
    serviceScope.cancel()  // <-- ...that's immediately cancelled
    instance = null
    super.onDestroy()
}
```

**Problem:** `dispose()` is launched as a coroutine in `serviceScope`, but `serviceScope.cancel()` is called on the very next line. The coroutine may never execute, or may be cancelled mid-execution, leaving VoiceOSCore in an inconsistent state (leaking resources, open connections, unflushed DB).

**Fix:** Either use `runBlocking` for cleanup, or call `dispose()` synchronously, or use `serviceScope.coroutineContext.job.invokeOnCompletion` pattern.

---

### P0-2: BootReceiver is Entirely Stub Code

**File:** `BootReceiver.kt:20-36`

The receiver is registered in the manifest (`<receiver android:name=".service.BootReceiver">`) but the implementation is 100% commented-out code. The `onReceive` method logs a message and does nothing. This is a zero-tolerance violation (stubs).

The Settings UI has an "Auto Start on Boot" toggle (`SettingsScreen.kt:187-194`) that sets `autoStartOnBoot` state but it's local-only (`remember { }`) — it never writes to SharedPreferences and BootReceiver never reads it.

**Impact:** User enables "Start on Boot" toggle → setting is lost on app close → boot happens → receiver runs → nothing happens. Broken feature presented to user.

---

### P0-3: CursorOverlayService is Empty Shell

**File:** `CursorOverlayService.kt:35-55`

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // ...
    startForeground(NOTIFICATION_ID, notification)
    // TODO: Initialize VoiceCursor overlay
    return START_STICKY
}
```

Service starts as foreground (takes up notification slot, shows persistent notification) but does absolutely nothing. No overlay window created, no cursor tracking, no dwell click. User sees "Cursor active" notification but nothing works.

---

### P1-4: Settings Not Persisted

**File:** `SettingsScreen.kt:34-38`

```kotlin
var dwellClickEnabled by remember { mutableStateOf(true) }
var dwellClickDelay by remember { mutableStateOf(1500f) }
var cursorSmoothing by remember { mutableStateOf(true) }
var voiceFeedback by remember { mutableStateOf(true) }
var autoStartOnBoot by remember { mutableStateOf(false) }
```

All 5 settings are local Compose state. They:
- Reset to defaults when navigating away and back
- Are never written to DataStore/SharedPreferences (even though `datastore.preferences` is in build.gradle dependencies)
- Are never read by any service or module
- Create a broken UX where user changes settings that immediately vanish

---

### P1-5: WebView Security Gap

**File:** `BrowserScreen.kt:178-201`

JavaScript is enabled (`settings.javaScriptEnabled = true`) but:
- No `WebChromeClient` set → JS `alert()`, `confirm()`, `prompt()` dialogs are silently swallowed
- No file upload handling (broken `<input type="file">`)
- No `shouldOverrideUrlLoading` → no URL filtering or deeplink handling
- No error page handling (`onReceivedError`)
- No SSL error handling (`onReceivedSslError`)
- No mixed content mode set (defaults to MIXED_CONTENT_NEVER_ALLOW on SDK 21+)

For a browser app, these are expected capabilities. Users will encounter broken sites.

---

### P2-6: Stale Permission Status on HomeScreen

**File:** `HomeScreen.kt:36-39`

```kotlin
LaunchedEffect(Unit) {
    accessibilityEnabled = VoiceAvanueAccessibilityService.isEnabled(context)
    overlayEnabled = Settings.canDrawOverlays(context)
}
```

`LaunchedEffect(Unit)` runs exactly once. When user taps "Enable" → goes to Settings → grants permission → returns to app, the status cards still show "Disabled" until full recomposition. Should use `onResume` lifecycle or `LifecycleEventEffect`.

---

### P2-7: Singleton Anti-Pattern in Application

**File:** `VoiceAvanueApplication.kt:129-134`

```kotlin
companion object {
    @Volatile private var instance: VoiceAvanueApplication? = null
    fun getInstance(): VoiceAvanueApplication = instance ?: throw IllegalStateException(...)
}
```

Hilt is the DI framework, but the Application class uses a manual singleton pattern. This means any code calling `VoiceAvanueApplication.getInstance()` bypasses Hilt's lifecycle and scoping. It also makes testing harder.

`VoiceAvanueAccessibilityService` also uses the same pattern (companion `instance`). Neither is injected via Hilt.

---

### P2-8: System Drawables for Notifications

**Files:** `RpcServerService.kt:81`, `CursorOverlayService.kt:83`, `VoiceRecognitionService.kt:98`

All three services use system drawables for notification icons:
- `android.R.drawable.ic_menu_manage` (deprecated)
- `android.R.drawable.ic_menu_compass` (deprecated)
- `android.R.drawable.ic_btn_speak_now` (deprecated)

These render as generic grey icons on modern Android. Should use branded vector drawables.

---

## Metrics

| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| Total Files | 11 source + 1 manifest + 1 gradle | - | OK |
| Lines of Code | ~1,500 | - | OK (thin wrapper) |
| TODO comments | 12 | 0 | FAIL |
| Stub implementations | 3 (P0) | 0 | FAIL |
| Settings persisted | 0/5 | 5/5 | FAIL |
| Notification icons (branded) | 0/3 | 3/3 | FAIL |
| Test files | 0 | ≥1 | FAIL |
| ProGuard rules | Present | - | OK |

---

## Actions (Priority Order) — ALL FIXED

1. **[P0] FIXED: Race condition in VoiceAvanueAccessibilityService.onDestroy()** — uses runBlocking with 3s timeout
2. **[P0] FIXED: BootReceiver** — reads auto-start pref from DataStore, starts CursorOverlayService via goAsync()
3. **[P0] FIXED: CursorOverlayService** — WindowManager overlay, CursorController integration, dwell click, Canvas rendering
4. **[P1] FIXED: Settings persisted to DataStore** — AvanuesSettingsRepository + SettingsViewModel (Hilt)
5. **[P1] FIXED: WebView hardened** — WebChromeClient, JS dialogs, SSL error handling, URL filtering, file upload, error page
6. **[P2] FIXED: HomeScreen permission refresh** — LifecycleEventObserver on ON_RESUME

### Remaining (Deferred)
7. **[P2] Singleton anti-patterns** — invasive refactor, low risk, defer to next session
8. **[P2] Notification icons** — cosmetic, requires vector asset creation
9. **[Future] Replace BrowserScreen with WebAvanue's BrowserApp** — requires BrowserDatabase Hilt wiring + Voyager integration

---

## Architecture Assessment

**Strengths:**
- Clean thin-wrapper architecture — all logic in KMP modules
- Proper use of Hilt for DI with scoped singletons
- Dual launcher alias pattern is elegant and well-implemented
- Compose-first UI with Material 3
- Dormant RPC strategy (don't auto-start, start on demand)
- Edge-to-edge with splash screen

**Weaknesses:**
- Zero test coverage for app layer
- Multiple stub/TODO implementations shipped
- Settings UX is completely broken (nothing persists)
- Browser lacks expected WebView hardening
- Services lack proper branded notification icons
