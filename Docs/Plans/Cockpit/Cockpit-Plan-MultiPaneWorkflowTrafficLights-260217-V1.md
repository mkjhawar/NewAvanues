# Cockpit Plan: Multi-Pane Workflow + Traffic Lights + External App Support

**Module:** `Modules/Cockpit/`
**Branch:** `IosVoiceOS-Development`
**Date:** 2026-02-17
**Status:** IMPLEMENTED

---

## Summary

Three features implemented in the Cockpit module:

1. **Traffic Light Window Controls** — macOS-style colored dots (red/yellow/green) replacing IconButton controls in FrameWindow
2. **3-Panel Workflow Layout** — extends WORKFLOW mode from 2-panel (30/70) to 3-panel (20/60/20) when AUXILIARY frames exist
3. **External App Support** — KMP interface for 3rd-party app detection and adjacent launch

## KMP Score

**8/11 files in commonMain (73% shared)**

All UI composables and models are cross-platform. Only platform-specific APIs (PackageManager, Intent) live in androidMain/desktopMain.

---

## Part A: Traffic Light Controls

**File:** `FrameWindow.kt` (commonMain)

### What Changed

- Replaced 3 `IconButton` controls (lines 207-243) with `TrafficLights` composable
- Added `TrafficDot` helper with hover/press icon reveal via `animateFloatAsState`
- Hover detection: `PointerEventType.Enter/Exit` (KMP-compatible)
- Press detection: `MutableInteractionSource.collectIsPressedAsState()`
- Removed `IconButton` import (no longer used in title bar)
- Added `OpenInNew` icon import for ExternalApp support

### Visual Layout

```
[●][●][●] ── Step 1 ── 🌐 ── Frame Title ── ← ──
 ↑   ↑   ↑
red yel grn   (12dp circles, 4dp spacing, 24dp touch target)
```

---

## Part B: Multi-Pane Workflow

### B.1: PanelRole Enum

**File:** `CockpitFrame.kt` (commonMain)

```kotlin
enum class PanelRole { STEPS, CONTENT, AUXILIARY }
```

Added `panelRole: PanelRole = PanelRole.CONTENT` field to `CockpitFrame`.

### B.2: WorkflowSidebar Rewrite

**File:** `WorkflowSidebar.kt` (commonMain, ~710 lines)

**New composables:**
- `WorkflowTriPanelLayout` — 20/60/20 `Row` with weighted columns + dividers
- `WorkflowPhoneTabLayout` — `TabRow` + `HorizontalPager` with 3 swipeable pages
- `VerticalDivider` — shared thin divider

**Bug fixes:**
- All `onClose`/`onMinimize`/`onMaximize` callbacks now properly wired (were empty `{}` before)
- `LayoutEngine.kt` updated to pass callbacks to `WorkflowSidebar`

**Adaptive behavior:**
- If no AUXILIARY frame: uses existing 2-panel (backward compatible)
- If AUXILIARY exists: switches to 3-panel automatically

---

## Part C: External App Support

### C.1: FrameContent.ExternalApp

**File:** `FrameContent.kt` (commonMain)

```kotlin
data class ExternalApp(
    val packageName: String = "",
    val activityName: String = "",
    val label: String = "",
) : FrameContent()
```

### C.2: IExternalAppResolver

**File:** `IExternalAppResolver.kt` (commonMain, NEW)

```kotlin
interface IExternalAppResolver {
    fun resolveApp(packageName: String): ExternalAppStatus
    fun launchAdjacent(packageName: String, activityName: String = "")
}

enum class ExternalAppStatus {
    NOT_INSTALLED, INSTALLED_NO_EMBED, EMBEDDABLE
}
```

### C.3: ExternalAppContent

**File:** `ExternalAppContent.kt` (commonMain, NEW)

Cross-platform composable with:
- App icon placeholder (circle with first letter)
- Status badge (color-coded)
- AvanueButton with OpenInNew icon for launch action

### C.4: AndroidExternalAppResolver

**File:** `AndroidExternalAppResolver.kt` (androidMain, NEW)

- `PackageManager.getApplicationInfo()` for install check
- API 33+: ActivityInfo flags for embedding check
- `FLAG_ACTIVITY_LAUNCH_ADJACENT | FLAG_ACTIVITY_NEW_TASK` for adjacent launch

### C.5: DesktopExternalAppResolver

**File:** `DesktopExternalAppResolver.kt` (desktopMain, NEW)

Stub: all apps resolve as NOT_INSTALLED.

### C.6: Supporting Changes

| File | Change |
|------|--------|
| `ContentAccent.kt` | `"external_app" -> INFO` |
| `FrameWindow.kt` | `is FrameContent.ExternalApp -> Icons.Default.OpenInNew` |
| `CommandBar.kt` | `"External App" to FrameContent.ExternalApp()` in addFrameOptions() |
| `ContentRenderer.kt` | ExternalApp case wiring AndroidExternalAppResolver + ExternalAppContent |

---

## File Summary

| # | File | Source Set | Change | Part |
|---|------|-----------|--------|------|
| 1 | `FrameWindow.kt` | commonMain | TrafficLights + TrafficDot + ExternalApp icon | A, C |
| 2 | `CockpitFrame.kt` | commonMain | PanelRole enum + panelRole field | B |
| 3 | `WorkflowSidebar.kt` | commonMain | TriPanelLayout, PhoneTabLayout, fix callbacks | B |
| 4 | `FrameContent.kt` | commonMain | ExternalApp data class + TYPE_EXTERNAL_APP | C |
| 5 | `IExternalAppResolver.kt` | commonMain | Interface + ExternalAppStatus enum (NEW) | C |
| 6 | `ExternalAppContent.kt` | commonMain | Shared composable for external app UI (NEW) | C |
| 7 | `ContentAccent.kt` | commonMain | external_app accent mapping | C |
| 8 | `CommandBar.kt` | commonMain | ExternalApp in addFrameOptions() | C |
| 9 | `LayoutEngine.kt` | commonMain | Pass callbacks to WorkflowSidebar | B |
| 10 | `AndroidExternalAppResolver.kt` | androidMain | PackageManager + Intent impl (NEW) | C |
| 11 | `ContentRenderer.kt` | androidMain | Wire ExternalApp rendering | C |
| 12 | `DesktopExternalAppResolver.kt` | desktopMain | Stub impl (NEW) | C |

---

## Documentation Updated

- Chapter 97: Updated CockpitFrame model, FrameContent types (17), FrameWindow traffic lights, WorkflowSidebar multi-pane, ExternalApp section, source set table
- MEMORY.md: Added Cockpit Multi-Pane section

---

*Cockpit-Plan-MultiPaneWorkflowTrafficLights-260217-V1*
