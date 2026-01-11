# BrowserApp Exit Cleanup Fix (DisposableEffect onDispose not working)

## Problem

`DisposableEffect(Unit) { onDispose { ... } }` inside **BrowserApp** was being used to run “clear on exit” logic (cookies/cache/history/etc.), but it **did not run reliably** when the user “exits” the app.

### Why it happens

`onDispose {}` runs only when the composable is **removed from the Compose composition** (typically when the Activity is actually destroyed). It usually **will not run** in these common cases:

- User presses **Home** → Activity goes to background (**onStop**), not destroyed

- User opens **Recents** → usually **onStop**

- OS kills the process → no callback is guaranteed

- “Exit” is often interpreted as “app backgrounded”, not “activity destroyed”

So cleanup logic placed only in `onDispose {}` is **not a correct hook** for “on exit” behavior.

---

## Fix Overview

We moved exit cleanup to **Lifecycle events** using a `LifecycleEventObserver`:

- Run cleanup on `Lifecycle.Event.ON_STOP` (covers Home/Recents/background)

- Also run cleanup on `Lifecycle.Event.ON_DESTROY` (covers actual close)

- Keep Compose `onDispose` only as a **fallback**

- Add a **one-time guard** to prevent cleanup running multiple times

---

## Changes Made

### 1) Add Lifecycle Observer in BrowserApp

**BrowserApp.kt** (inside the `@Composable fun BrowserApp(...)`)

#### Add:

- `val lifecycleOwner = LocalLifecycleOwner.current`

- A `LifecycleEventObserver` inside `DisposableEffect(lifecycleOwner)`

- Call cleanup in `ON_STOP` and `ON_DESTROY`

---

### 2) Add One-Time Guard to Avoid Duplicate Cleanup

Because `ON_STOP`, `ON_DESTROY`, and Compose `onDispose` can all be triggered in one session, cleanup must run **once**.

We used:

- `AtomicBoolean` (`compareAndSet(false, true)`)  
  or you can use a simple `remember { mutableStateOf(false) }` guard (Atomic is safer).

---

### 3) Implement Real Cookie Clearing (Important)

Previously code had only logs like:

`// Cookies are cleared via WebView - handled in WebViewPoolManager`

But unless `WebViewPoolManager` actually clears cookies via `CookieManager`, cookies were not cleared.

We added actual cookie clearing:

`CookieManager.getInstance().removeAllCookies(null) CookieManager.getInstance().flush()`

---

## Recommended Implementation (Code Snippet)

Paste this logic inside `BrowserApp(...)`:

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
val didCleanup = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

fun runExitCleanup(reason: String) {
    if (!didCleanup.compareAndSet(false, true)) return

    val currentSettings = viewModels.settingsViewModel.settings.value
    println("BrowserApp: runExitCleanup($reason) settings=$currentSettings")

    // Example exit cleanup
    if (currentSettings?.clearHistoryOnExit == true) {
        viewModels.historyViewModel.clearHistory()
    }

    if (currentSettings?.clearCookiesOnExit == true) {
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
    }

    if (currentSettings?.clearCacheOnExit == true) {
        WebViewPoolManager.clearAllWebViews() // or a dedicated clearCache() API
    }

    // optional: release viewmodels/resources
    repository.cleanup()
}

// Lifecycle-based cleanup (reliable for "exit"/background)
DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
        when (event) {
            androidx.lifecycle.Lifecycle.Event.ON_STOP -> runExitCleanup("ON_STOP")
            androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> runExitCleanup("ON_DESTROY")
            else -> Unit
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}

// Compose fallback cleanup (not reliable alone)
DisposableEffect(Unit) {
    onDispose { runExitCleanup("COMPOSE_DISPOSE") }
}

```

---

## Behavior After Fix

✅ **Clear on Exit now works when:**

- User presses **Home**

- User opens **Recents**

- App goes to background (`ON_STOP`)

- Activity finishes / app is closed (`ON_DESTROY`)

- Compose is destroyed (`onDispose` fallback)
