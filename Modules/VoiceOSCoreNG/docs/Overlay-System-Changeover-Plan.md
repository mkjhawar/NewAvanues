# Overlay System Changeover Plan

## Overview

This document outlines the migration from a separate `OverlayService` (requiring SYSTEM_ALERT_WINDOW permission) to using the `AccessibilityService`'s built-in overlay capability (`TYPE_ACCESSIBILITY_OVERLAY`).

## Current Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Current System                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────┐    ┌─────────────────────────┐ │
│  │ VoiceOSAccessibility    │    │    OverlayService       │ │
│  │       Service           │    │    (Foreground)         │ │
│  ├─────────────────────────┤    ├─────────────────────────┤ │
│  │ - Monitors apps         │    │ - Renders number badges │ │
│  │ - Extracts UI elements  │───▶│ - Instruction bar       │ │
│  │ - Generates commands    │    │ - Floating scan button  │ │
│  │ - VUID mapping          │    │                         │ │
│  └─────────────────────────┘    └─────────────────────────┘ │
│           │                              │                  │
│           ▼                              ▼                  │
│  ┌─────────────────────┐      ┌─────────────────────────┐   │
│  │ BIND_ACCESSIBILITY  │      │ SYSTEM_ALERT_WINDOW     │   │
│  │     (Required)      │      │ (Extra Permission)      │   │
│  └─────────────────────┘      └─────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Problems with Current Architecture

1. **Two Permissions Required**: Users must grant both Accessibility and Overlay permissions
2. **Manual Start**: OverlayService must be started manually from app UI
3. **Lifecycle Complexity**: Two services to manage, potential sync issues
4. **User Confusion**: "Why do I need two permissions?"

## Target Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Unified System                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │           VoiceOSAccessibilityService                   ││
│  ├─────────────────────────────────────────────────────────┤│
│  │ Core Functions:                                         ││
│  │ - Monitors apps via accessibility events                ││
│  │ - Extracts UI elements from AccessibilityNodeInfo       ││
│  │ - Generates voice commands and VUID mappings            ││
│  │                                                         ││
│  │ Overlay Functions (NEW):                                ││
│  │ - Renders number badges using TYPE_ACCESSIBILITY_OVERLAY││
│  │ - Shows instruction bar                                 ││
│  │ - Floating scan button (optional)                       ││
│  └─────────────────────────────────────────────────────────┘│
│                         │                                   │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              BIND_ACCESSIBILITY_SERVICE                 ││
│  │                   (Single Permission)                   ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Benefits of Unified Architecture

1. **Single Permission**: Only accessibility permission needed
2. **Auto-Start**: Overlay available immediately when accessibility enabled
3. **Better Lifecycle**: Single service, no sync issues
4. **Simpler UX**: One toggle enables everything

## Implementation Plan

### Phase 1: Move Overlay Rendering to AccessibilityService

**Files to Modify:**
- `VoiceOSAccessibilityService.kt` - Add overlay rendering capability
- `OverlayService.kt` - Mark as deprecated (keep for reference)

**Key Changes:**

1. **Add WindowManager Overlay Support**

```kotlin
// In VoiceOSAccessibilityService.kt

private var overlayView: ComposeView? = null
private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

private fun createOverlayParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,  // No extra permission!
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )
}
```

2. **Migrate Compose UI Components**

Move from `OverlayService.kt`:
- `NumberBadge` composable
- `InstructionBar` composable
- `NumberedOverlay` composable
- `BadgeTheme` enum and styling

3. **Update Overlay State Management**

```kotlin
// Current: StateFlow in companion object, collected by OverlayService
val numberedOverlayItems: StateFlow<List<NumberOverlayItem>>

// New: Direct rendering in same service
private fun updateOverlay(items: List<NumberOverlayItem>) {
    overlayView?.setContent {
        NumberedOverlay(items = items, theme = currentTheme)
    }
}
```

### Phase 2: Handle Touch Events for Clickable Badges

**Challenge:** `TYPE_ACCESSIBILITY_OVERLAY` with `FLAG_NOT_TOUCHABLE` won't receive touch events.

**Solution:** For clickable badges, create separate small windows per badge:

```kotlin
private fun createBadgeWindow(item: NumberOverlayItem): View {
    val params = WindowManager.LayoutParams(
        BADGE_SIZE,
        BADGE_SIZE,
        item.left,  // X position
        item.top,   // Y position
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,  // Touchable!
        PixelFormat.TRANSLUCENT
    )
    // ... create and return badge view with click handler
}
```

### Phase 3: Clean Up

1. Remove `OverlayService.kt` (or keep as deprecated reference)
2. Update `MainActivity.kt` - remove "Start Scanner Overlay" button
3. Update `AndroidManifest.xml` - remove OverlayService declaration if unused
4. Remove overlay permission request from UI

## Technical Details

### TYPE_ACCESSIBILITY_OVERLAY vs TYPE_APPLICATION_OVERLAY

| Feature | TYPE_ACCESSIBILITY_OVERLAY | TYPE_APPLICATION_OVERLAY |
|---------|---------------------------|-------------------------|
| Permission | BIND_ACCESSIBILITY_SERVICE | SYSTEM_ALERT_WINDOW |
| User Grant | Settings > Accessibility | Settings > Apps > Special |
| Auto-Available | When accessibility enabled | Manual permission grant |
| Z-Order | Above apps, below system UI | Above apps, below system UI |
| Touch Events | Supported | Supported |

### Key API Methods

```kotlin
// AccessibilityService has built-in access to WindowManager
val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

// Create overlay with accessibility type (no permission needed)
val params = WindowManager.LayoutParams(
    ...,
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    ...
)
windowManager.addView(overlayView, params)

// Update overlay content
overlayView.setContent { ... }

// Remove overlay
windowManager.removeView(overlayView)
```

## Migration Checklist

- [ ] Add overlay view management to VoiceOSAccessibilityService
- [ ] Move NumberBadge composable to accessibility service
- [ ] Move InstructionBar composable to accessibility service
- [ ] Move BadgeTheme enum and styling
- [ ] Implement direct overlay updates (no StateFlow needed)
- [ ] Handle badge click events via separate touchable windows
- [ ] Update MainActivity to remove overlay permission request
- [ ] Update MainActivity to remove "Start Scanner Overlay" button
- [ ] Test on emulator with only accessibility permission
- [ ] Deprecate/remove OverlayService
- [ ] Update CLAUDE.md documentation

## Rollback Plan

If issues arise, the current `OverlayService` implementation remains functional. The auto-start code in `onServiceConnected` can be reverted:

```kotlin
// Revert this block if needed:
if (Settings.canDrawOverlays(this)) {
    OverlayService.start(this)
}
```

## Testing

1. **Permission Test**: Disable overlay permission, verify badges still show
2. **Lifecycle Test**: Toggle accessibility on/off, verify overlay appears/disappears
3. **Touch Test**: Tap badges, verify correct email/item opens
4. **Performance Test**: Scroll through long lists, verify smooth rendering
5. **Orientation Test**: Rotate device, verify badges reposition correctly

## Timeline

This refactoring should be done in a single focused session to avoid having two half-working systems.

---

**Author:** VoiceOSCoreNG Development Team
**Created:** 2026-01-09
**Status:** Planning
