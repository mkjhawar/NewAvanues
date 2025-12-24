# WebView Settings Not Applying – Root Cause & Fix

## Overview

In the current implementation, `SettingsApplicator.applySettings()` **appears not to work**, but the real issue is **when and with what data it is called**.

Two separate problems cause settings (privacy, display, desktop mode, etc.) to **not apply reliably**, especially after app restart or tab changes.

This document explains:

- The exact root causes
- Why `applySettings()` is skipped or overridden
- The required fixes in `WebViewContainer.android.kt` and `BrowserScreen.kt`

---

## Issue 1: Settings Applied Before WebView Is Ready (Race Condition)

### Problem

In `WebViewContainer.android.kt`, settings are applied inside:

```kotlin
LaunchedEffect(settings) {
    settings?.let { browserSettings ->
        webView?.let { view ->
            settingsStateMachine.requestUpdate(browserSettings) {
                SettingsApplicator().applySettings(view, it)
            }
        }
    }
}
```

----

If:

- `settings` becomes non-null **before**

- `AndroidView {}` assigns the `webView`

Then:

- `webView == null`

- `applySettings()` is **never called**

- The effect does **not re-run**

- Settings silently fail

This explains why settings:

- Work sometimes

- Fail after fresh install or restart

- Look like they are “not saved”



---

### Fix

The effect **must re-run when the WebView becomes available**.

#### ✅ Correct Fix

`LaunchedEffect(settings, webView) {    val view = webView ?: return@LaunchedEffect     val s = settings ?: return@LaunchedEffect      settingsStateMachine.requestUpdate(s) { settingsToApply ->         SettingsApplicator().applySettings(view, settingsToApply)     } }`

### Why This Works

- The effect now re-triggers when:
  
  - Settings change
  
  - WebView is created

- Guarantees `applySettings()` is executed **after WebView exists**

- Removes race condition completely

---

## Result After Fix

✅ Settings apply reliably after app restart  
✅ Privacy & display toggles no longer “reset”  
✅ Desktop mode works correctly per tab  
✅ `applySettings()` always runs at the right time  
✅ No race conditions
