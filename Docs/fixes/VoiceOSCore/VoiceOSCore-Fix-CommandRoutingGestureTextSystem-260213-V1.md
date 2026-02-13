# VoiceOSCore Fix: Command Routing for Gesture, Text, System, and Input Handlers

**Date:** 2026-02-13
**Branch:** VoiceOSCore-KotlinUpdate (b092877a), IosVoiceOS-Development (7b974179)
**Status:** Complete

## Problem

Multiple voice command categories were failing during device testing:

| Category | Failing Commands |
|----------|-----------------|
| NAVIGATION | Standalone tap/click/press, long press, double tap, zoom in/out, page up/down |
| TEXT | select all (stolen by AndroidGestureHandler), undo (wrong API), redo (null check) |
| SYSTEM | open app drawer, synonyms mismatch |
| INPUT | show/hide keyboard (invalid XML attribute) |

## Root Cause Analysis

### 1. AndroidGestureHandler Command Stealing (Priority Collision)

**HandlerRegistry** routes by priority: SYSTEM(1) > NAVIGATION(2) > ... > INPUT(8).

**BaseHandler.canHandle()** has prefix matching: `normalized.startsWith(supportedAction + " ")`.

AndroidGestureHandler (NAVIGATION=priority 2) had `"select"` in its `supportedActions`, which matched `"select all"` via prefix match — stealing the command from TextHandler (INPUT=priority 8) before it could process.

### 2. EXECUTE ActionType Not Handled

The `processVoiceCommand()` 3-step flow creates commands with `actionType=EXECUTE` from the static handler path (Step 2). AndroidGestureHandler's `execute()` only had phrase-based routing for scroll/swipe, so all other gesture commands with EXECUTE actionType fell to the `else -> notHandled()` branch.

### 3. TextHandler.performUndo Using Wrong API

`performUndo()` was calling `ACTION_IME_ENTER` which sends an Enter key — not undo. Android's AccessibilityNodeInfo does not expose undo/redo actions.

### 4. Invalid XML Attribute

`android:canControlSoftKeyboard` does not exist in the accessibility service XML schema. The `SoftKeyboardController` is a runtime API on `AccessibilityService`, not an XML declaration.

### 5. SystemHandler Missing Synonyms and Commands

`supportedActions` list was missing synonyms that were present in the `execute()` when-block (e.g., "navigate back", "previous screen"). Also missing: "open app drawer", "app drawer", "all apps".

## Fix Applied

### VoiceOSCoreAndroidFactory.kt — AndroidGestureHandler

- Removed `"select"` from `supportedActions` (prevents prefix-match theft of "select all")
- Removed bare `"lock"` (kept "lock element" — prevents stealing from SystemHandler's "lock screen")
- Added `"page up"`, `"page down"` to supportedActions
- Added comprehensive phrase-based routing block covering ALL gesture commands:
  - tap/click/press at screen center
  - long press, double tap
  - zoom in/out/reset, scale up/down
  - fling/flick/throw with direction extraction
  - grab/release (long press / tap)
  - pan (scroll alias)
  - select word (double tap), clear selection (tap)
  - page up/down

### VoiceOSCoreAndroidFactory.kt — AndroidSystemExecutor

- Added `openAppDrawer()` using `GLOBAL_ACTION_ALL_APPS` (value 14) on API 30+
- Fallback to `GLOBAL_ACTION_HOME` on older API levels

### TextHandler.kt

- `performUndo()`: Removed incorrect `ACTION_IME_ENTER` usage, returns clear failure message
- `performRedo()`: Added null check for focused node
- Removed unused `android.os.Build` import

### SystemHandler.kt (KMP)

- Added missing synonyms to `supportedActions`: "navigate back", "previous screen", "navigate home", "open home", "show recent apps", "open recents", "app switcher", "notification panel"
- Added "open app drawer", "app drawer", "all apps" to both `supportedActions` and `execute()` routing
- Added `openAppDrawer()` to `SystemExecutor` interface

### accessibility_service_config.xml

- Removed invalid `android:canControlSoftKeyboard="true"` (attribute does not exist)
- Added `flagRequestFilterKeyEvents` to `accessibilityFlags` for keyboard event interception

## Files Modified

| File | Change Type |
|------|-------------|
| `Modules/VoiceOSCore/src/androidMain/.../VoiceOSCoreAndroidFactory.kt` | Gesture routing + system executor |
| `Modules/VoiceOSCore/src/androidMain/.../handlers/TextHandler.kt` | Undo/redo fix |
| `Modules/VoiceOSCore/src/commonMain/.../system/SystemHandler.kt` | Synonyms + app drawer |
| `apps/avanues/src/main/res/xml/accessibility_service_config.xml` | XML flag fix |

## Verification

- VoiceOSCore module: BUILD SUCCESSFUL
- apps:avanues: BUILD SUCCESSFUL
- Cherry-pick to IosVoiceOS-Development: Clean (no conflicts)

## Device Test Checklist

| Command | Expected Result |
|---------|----------------|
| "select all" | TextHandler handles (not stolen by gesture handler) |
| "tap" / "click" | AndroidGestureHandler taps screen center |
| "zoom in" / "zoom out" | Pinch gesture dispatched |
| "page up" / "page down" | Scroll gesture dispatched |
| "open app drawer" | SystemHandler → GLOBAL_ACTION_ALL_APPS |
| "undo" / "redo" | Graceful failure with message |
| "show keyboard" / "hide keyboard" | InputHandler via SoftKeyboardController |
