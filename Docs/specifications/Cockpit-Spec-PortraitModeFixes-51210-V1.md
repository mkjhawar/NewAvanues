# Cockpit MVP - Portrait Mode Fixes Specification

**Version:** 1.0
**Date:** 2025-12-10
**Project:** Cockpit MVP (cockpit-mvp)
**Platform:** Android (Jetpack Compose)
**Type:** Bug Fixes + Feature Enhancements
**Priority:** P0-P2 (Critical to Medium)
**Status:** Ready for Implementation

---

## Executive Summary

This specification addresses 5 critical UX and functionality issues discovered during portrait mode testing. The fixes include: window control button logic bug, dynamic window sizing, window preset persistence, UX improvements to the Add Window dialog, and terminology clarification. Implementation is split into two phases: Quick Wins (2.5 hours) and Future Enhancements (5 hours).

**Key Features:**
- Fix maximize/minimize button state conflict
- Dynamic maximize sizing (fill screen minus 20px border)
- Window preset save/load system
- Improved Add Window dialog UX
- Clearer terminology for window types

---

## Problem Statement

**Current State:**
- Maximize button leaves windows stuck in minimized state (broken logic)
- Maximize uses fixed 600x800dp (doesn't adapt to screen size)
- Users must re-type URLs every time (no persistence)
- Default URL is webavanue.com instead of google.com
- Enter key doesn't submit Add Window dialog
- "Web App" terminology is confusing
- No UI for selecting Android apps (must type package name)
- Widget option shown but not implemented

**Pain Points:**
1. **Broken Window Controls:** Windows get stuck in minimized state, blocking productivity
2. **Poor Maximize Behavior:** Fixed size doesn't match user expectations of "fill screen"
3. **Repetitive Data Entry:** No way to save frequently-used windows
4. **Poor UX:** Must click button instead of hitting Enter, confusing labels

**Desired State:**
- Maximize button properly restores minimized windows
- Maximize fills screen with 20dp border on all sides
- Window presets allow saving/loading frequently-used windows
- Add Window dialog uses clear terminology and accepts Enter key
- Android app picker available instead of manual package entry

---

## Functional Requirements

### Phase 1: Quick Wins (P0-P1 Priority)

#### FR-1.1: Fix Maximize/Minimize Button State Conflict
**Priority:** P0 (Critical)
**Platform:** Android
**Component:** WorkspaceViewModel, WindowCard

**Requirements:**
- When maximize button is clicked on a minimized window, window MUST restore to full size
- Maximize button MUST clear `isHidden` state before toggling `isLarge`
- Button behavior MUST follow this state machine:
  - Normal → Maximize: `isHidden = false, isLarge = true`
  - Maximized → Normal: `isHidden = false, isLarge = false`
  - Minimized → Maximize: `isHidden = false, isLarge = true` (restore + maximize)
  - Minimized → Minimize: No-op (already minimized)

**Acceptance Criteria:**
- [ ] Minimize window → height = 48dp
- [ ] Click maximize on minimized window → height = screen height - 40dp
- [ ] Click maximize on normal window → height = screen height - 40dp
- [ ] Click maximize on maximized window → height = 400dp (normal)
- [ ] No windows stuck in minimized state

**Files Modified:**
- `WorkspaceViewModel.kt` - Update `toggleWindowSize()` method

---

#### FR-1.2: Dynamic Maximize Sizing (Fill Screen Minus 20px)
**Priority:** P1 (High)
**Platform:** Android
**Component:** WindowCard

**Requirements:**
- Maximize button MUST calculate window size dynamically based on screen dimensions
- Maximized window MUST be: `width = screenWidth - 40.dp, height = screenHeight - 40.dp`
- Sizing MUST adapt to orientation changes (portrait/landscape)
- Sizing MUST respect safe area insets (status bar, navigation bar)
- Animation MUST remain smooth (300ms tween)

**Acceptance Criteria:**
- [ ] Portrait mode: Maximized window fills screen with 20dp border
- [ ] Landscape mode: Maximized window fills screen with 20dp border
- [ ] Rotate while maximized: Window adapts to new orientation
- [ ] Different screen sizes (phone, tablet): 20dp border maintained
- [ ] Navigation bar visible: Window respects insets

**Files Modified:**
- `WindowCard.kt` - Wrap in `BoxWithConstraints`, calculate dynamic size

---

#### FR-1.3: Change Default URL to Google.com
**Priority:** P1 (High)
**Platform:** Android
**Component:** ControlPanel

**Requirements:**
- Default URL in Add Window dialog MUST be `https://google.com`
- URL field MUST be pre-populated with default on dialog open
- User MUST be able to clear and type custom URL

**Acceptance Criteria:**
- [ ] Open Add Window dialog → URL field shows "https://google.com"
- [ ] Clear URL field → empty field
- [ ] Type custom URL → accepts input

**Files Modified:**
- `ControlPanel.kt` - Change default URL from webavanue.com to google.com

---

#### FR-1.4: Enter Key Submits Add Window Dialog
**Priority:** P1 (High)
**Platform:** Android
**Component:** ControlPanel

**Requirements:**
- Pressing Enter/Return in URL field MUST submit dialog (same as clicking "Add")
- Submit action MUST only trigger if window title is not blank
- Keyboard MUST dismiss after Enter is pressed
- IME action MUST be "Done" for URL field

**Acceptance Criteria:**
- [ ] Type title, type URL, press Enter → window created
- [ ] Press Enter with blank title → nothing happens
- [ ] Press Enter with valid data → keyboard dismisses
- [ ] Press Enter → same result as clicking "Add" button

**Files Modified:**
- `ControlPanel.kt` - Add `keyboardOptions` and `keyboardActions` to URL field

---

#### FR-1.5: Change "Web App" to "Web Page (URL)"
**Priority:** P2 (Medium)
**Platform:** Android
**Component:** ControlPanel

**Requirements:**
- Window type label for `WindowType.WEB_APP` MUST be "Web Page (URL)"
- Label MUST clarify that this option is for entering a URL
- No changes to underlying `WindowType` enum

**Acceptance Criteria:**
- [ ] Open Add Window dialog → radio button shows "Web Page (URL)"
- [ ] Select "Web Page (URL)" → URL input field appears
- [ ] Add window → window type is WEB_APP (unchanged internally)

**Files Modified:**
- `ControlPanel.kt` - Update `getTypeLabel()` function

---

#### FR-1.6: Hide Widget Option (Not Implemented)
**Priority:** P2 (Medium)
**Platform:** Android
**Component:** ControlPanel

**Requirements:**
- Widget option MUST be hidden from Add Window dialog
- Only Android App, Web Page, Remote Desktop shown
- Widget type MUST be filtered from `WindowType.values()` list

**Acceptance Criteria:**
- [ ] Open Add Window dialog → Widget option not visible
- [ ] Only 3 radio buttons shown (Android App, Web Page, Remote Desktop)
- [ ] Existing windows with Widget type still render correctly

**Files Modified:**
- `ControlPanel.kt` - Filter Widget from window type list

---

### Phase 2: Feature Enhancements (P2 Priority)

#### FR-2.1: Window Preset Save/Load System
**Priority:** P2 (Medium)
**Platform:** Android
**Component:** New: WindowPresetManager, ControlPanel

**Requirements:**
- User MUST be able to save window configurations as presets
- Preset MUST include: title, type, content (URL/package), group name, color
- Presets MUST persist across app restarts
- User MUST be able to load preset from list in Add Window dialog
- User MUST be able to organize presets into groups (e.g., "Work", "Personal")
- Presets MUST be stored in SharedPreferences as JSON

**Data Model:**
```kotlin
data class WindowPreset(
    val id: String,
    val title: String,
    val type: WindowType,
    val content: WindowContent,
    val groupName: String = "default",
    val color: String = "#4ECDC4",
    val createdAt: Long = 0L
)
```

**User Flow:**
1. User opens Add Window dialog
2. User enters title, selects type, enters URL/package
3. User checks "Save as preset" checkbox
4. User enters group name (e.g., "Work")
5. User clicks "Add"
6. Window is created AND preset is saved
7. Next time user opens dialog, preset appears in list
8. User selects preset from list → title/URL auto-populated

**Acceptance Criteria:**
- [ ] Add window with "Save as preset" checked → preset saved
- [ ] Open dialog again → preset appears in list
- [ ] Select preset → title/URL auto-populated
- [ ] Click "Add" from preset → window created correctly
- [ ] Restart app → presets persisted
- [ ] Multiple presets with different groups → organized correctly
- [ ] Delete preset (future) → removed from list

**Files Created:**
- `Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/presets/WindowPreset.kt`
- `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WindowPresetManager.kt`

**Files Modified:**
- `ControlPanel.kt` - Add preset UI to AddWindowDialog
- Add `kotlinx-serialization` dependency to `build.gradle.kts`

---

#### FR-2.2: Android App Picker (Future)
**Priority:** P3 (Low)
**Platform:** Android
**Component:** ControlPanel

**Requirements:**
- When "Android App" is selected, user MUST see "Browse Installed Apps" button
- Clicking button MUST show list of installed non-system apps
- App list MUST show app name and icon
- Selecting app MUST auto-fill package name
- Manual package name entry MUST still be available as fallback

**Acceptance Criteria:**
- [ ] Select "Android App" → "Browse Installed Apps" button visible
- [ ] Click button → app list dialog appears
- [ ] App list shows only user apps (no system apps)
- [ ] App list sorted alphabetically by app name
- [ ] Select app → package name field auto-filled
- [ ] Manual entry still works if user prefers

**Files Modified:**
- `ControlPanel.kt` - Add app picker dialog

**Dependencies:**
- Android PackageManager API
- Material 3 Dialog with LazyColumn

---

## Non-Functional Requirements

### NFR-1: Performance
- [ ] Window size animations MUST maintain 60 FPS
- [ ] Preset load from SharedPreferences MUST complete in <50ms
- [ ] Add Window dialog MUST open in <100ms
- [ ] Maximize/minimize transitions MUST be smooth (300ms tween)

### NFR-2: Accessibility
- [ ] All buttons MUST have content descriptions
- [ ] Touch targets MUST be ≥48dp
- [ ] Haptic feedback MUST fire on all button clicks
- [ ] Keyboard navigation MUST work (tab order logical)
- [ ] Screen reader MUST announce window states correctly

### NFR-3: Data Integrity
- [ ] Preset save MUST be atomic (no partial writes)
- [ ] Invalid JSON in SharedPreferences MUST not crash app
- [ ] Preset load MUST gracefully handle missing fields (default values)
- [ ] Window state transitions MUST be deterministic

### NFR-4: Compatibility
- [ ] Android API 26+ (Oreo)
- [ ] Jetpack Compose 1.5+
- [ ] Material Design 3
- [ ] kotlinx-serialization 1.6+

---

## User Stories

### Story 1: Fix Stuck Minimized Window
**As a** user
**I want** the maximize button to restore minimized windows
**So that** I don't have to minimize again before maximizing

**Acceptance Criteria:**
- Given a minimized window (48dp height)
- When I click the maximize button
- Then the window expands to full screen minus 20dp border
- And the window is no longer minimized

---

### Story 2: Maximize Fills Screen
**As a** user
**I want** the maximize button to fill my screen with a small border
**So that** I can focus on one window at full size

**Acceptance Criteria:**
- Given a normal window (400dp height)
- When I click the maximize button
- Then the window expands to screen height minus 40dp (20dp border top+bottom)
- And the window expands to screen width minus 40dp (20dp border left+right)
- And the animation is smooth (300ms)

---

### Story 3: Save Frequently-Used Windows
**As a** power user
**I want** to save my frequently-used windows as presets
**So that** I don't have to re-type URLs every time

**Acceptance Criteria:**
- Given I'm adding a new window
- When I check "Save as preset" and enter a group name
- Then the window is created
- And the preset is saved to storage
- And next time I open the dialog, the preset appears in the list
- And selecting the preset auto-fills title and URL

---

### Story 4: Quick Window Creation
**As a** user
**I want** to press Enter to add a window
**So that** I don't have to reach for the mouse to click "Add"

**Acceptance Criteria:**
- Given I've entered a title and URL
- When I press Enter in the URL field
- Then the window is created
- And the dialog closes
- And the keyboard dismisses

---

### Story 5: Clear Window Type Labels
**As a** non-technical user
**I want** to see "Web Page (URL)" instead of "Web App"
**So that** I understand what each option means

**Acceptance Criteria:**
- Given I open the Add Window dialog
- When I see the window type options
- Then "Web Page (URL)" is displayed
- And I understand this option is for entering a website URL

---

## Technical Design

### Architecture Changes

**Before (Current):**
```
WorkspaceViewModel
├── toggleWindowSize(windowId)
│   └── Toggles isLarge only (BUG: doesn't clear isHidden)
│
WindowCard
├── animatedHeight
│   └── if (isHidden) 48.dp else if (isLarge) 800.dp else 400.dp
│       └── Hardcoded 800.dp (ISSUE: doesn't adapt to screen)
│
ControlPanel
├── AddWindowDialog
│   ├── Default URL: "https://webavanue.com" (ISSUE: wrong default)
│   ├── URL field: No Enter key handler (ISSUE: must click button)
│   └── Type labels: "Web App" (ISSUE: confusing)
```

**After (Fixed):**
```
WorkspaceViewModel
├── toggleWindowSize(windowId)
│   └── If isHidden, restore first, then toggle isLarge
│
WindowCard (wrapped in BoxWithConstraints)
├── animatedHeight
│   └── if (isHidden) 48.dp
│       else if (isLarge) screenHeight - 40.dp
│       else 400.dp
│
ControlPanel
├── AddWindowDialog
│   ├── WindowPresetManager (NEW)
│   ├── Default URL: "https://google.com"
│   ├── URL field: Enter key submits
│   ├── Type labels: "Web Page (URL)"
│   ├── Preset list section (NEW)
│   └── "Save as preset" checkbox (NEW)
│
WindowPresetManager (NEW)
├── savePreset(title, type, content, groupName)
├── loadPresets() → List<WindowPreset>
├── deletePreset(id)
└── getPresetsByGroup(groupName)
```

---

### Component Specifications

#### Component 1: WorkspaceViewModel.toggleWindowSize()

**Current Implementation (Lines 223-234):**
```kotlin
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            window.copy(
                isLarge = !window.isLarge,  // BUG: doesn't check isHidden
                updatedAt = System.currentTimeMillis()
            )
        } else {
            window
        }
    }
}
```

**Fixed Implementation:**
```kotlin
/**
 * Toggle window size between normal (300x400dp) and maximized (screen - 40dp)
 *
 * State Transitions:
 * - Normal (isHidden=false, isLarge=false) → Maximized (isHidden=false, isLarge=true)
 * - Maximized (isHidden=false, isLarge=true) → Normal (isHidden=false, isLarge=false)
 * - Minimized (isHidden=true, isLarge=false) → Maximized (isHidden=false, isLarge=true)
 *
 * Fix: Always clears isHidden state before toggling isLarge
 *
 * @param windowId The ID of the window to toggle size
 */
fun toggleWindowSize(windowId: String) {
    _windows.value = _windows.value.map { window ->
        if (window.id == windowId) {
            window.copy(
                isHidden = false,        // Always restore when maximizing (FIX)
                isLarge = !window.isLarge,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            window
        }
    }
}
```

**Test Cases:**
| Test | Initial State | After toggleWindowSize | Expected Height |
|------|---------------|------------------------|-----------------|
| T1 | isHidden=false, isLarge=false | isHidden=false, isLarge=true | screenHeight - 40dp |
| T2 | isHidden=false, isLarge=true | isHidden=false, isLarge=false | 400dp |
| T3 | isHidden=true, isLarge=false | isHidden=false, isLarge=true | screenHeight - 40dp |
| T4 | isHidden=true, isLarge=true | isHidden=false, isLarge=false | 400dp |

---

#### Component 2: WindowCard Dynamic Sizing

**Current Implementation (Lines 37-48):**
```kotlin
val animatedWidth by animateDpAsState(
    targetValue = if (window.isLarge) 600.dp else OceanTheme.windowWidthDefault,
    animationSpec = tween(durationMillis = 300),
    label = "window_width"
)
val animatedHeight by animateDpAsState(
    targetValue = if (window.isHidden) 48.dp else if (window.isLarge) 800.dp else OceanTheme.windowHeightDefault,
    animationSpec = tween(durationMillis = 300),
    label = "window_height"
)
```

**Fixed Implementation:**
```kotlin
@Composable
fun WindowCard(
    window: AppWindow,
    color: String,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onToggleSize: () -> Unit,
    onSelect: () -> Unit,
    onContentStateChange: (WindowContent) -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    isSelected: Boolean = false,
    freeformManager: FreeformWindowManager? = null
) {
    // Wrap in BoxWithConstraints to get screen dimensions
    BoxWithConstraints {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Dynamic maximize size: fill screen minus 20dp border on all sides
        val maximizedWidth = screenWidth - 40.dp
        val maximizedHeight = screenHeight - 40.dp

        // Animated window size based on isLarge state
        val animatedWidth by animateDpAsState(
            targetValue = if (window.isLarge) maximizedWidth else OceanTheme.windowWidthDefault,
            animationSpec = tween(durationMillis = 300),
            label = "window_width"
        )
        val animatedHeight by animateDpAsState(
            targetValue = if (window.isHidden) 48.dp
                else if (window.isLarge) maximizedHeight
                else OceanTheme.windowHeightDefault,
            animationSpec = tween(durationMillis = 300),
            label = "window_height"
        )

        GlassmorphicCard(
            modifier = modifier
                .width(animatedWidth)
                .height(animatedHeight)
                .clickable { onSelect() },
            isFocused = isFocused,
            isSelected = isSelected
        ) {
            // ... rest of WindowCard content unchanged
        }
    }
}
```

**Test Cases:**
| Screen Size | Maximized Width | Maximized Height | Border |
|-------------|-----------------|------------------|--------|
| 1080x1920 (portrait) | 1040px | 1880px | 20dp (40px) each side |
| 1920x1080 (landscape) | 1880px | 1040px | 20dp (40px) each side |
| 1440x2960 (tablet portrait) | 1400px | 2920px | 20dp (40px) each side |

---

#### Component 3: WindowPresetManager

**New File:** `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WindowPresetManager.kt`

```kotlin
package com.augmentalis.cockpit.mvp

import android.content.Context
import android.content.SharedPreferences
import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType
import com.avanues.cockpit.presets.WindowPreset
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Manages window presets for quick window creation
 * Persists presets to SharedPreferences as JSON
 *
 * Supports:
 * - Save preset with title, type, content, group name
 * - Load all presets
 * - Load presets by group
 * - Delete preset by ID
 */
class WindowPresetManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("window_presets", Context.MODE_PRIVATE)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Save a window preset
     *
     * @param title Window title
     * @param type Window type (WEB_APP, ANDROID_APP, etc.)
     * @param content Window content configuration
     * @param groupName Group name for organization (default: "default")
     * @param color Accent color for window
     */
    fun savePreset(
        title: String,
        type: WindowType,
        content: WindowContent,
        groupName: String = "default",
        color: String = "#4ECDC4"
    ) {
        val preset = WindowPreset(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            content = content,
            groupName = groupName,
            color = color,
            createdAt = System.currentTimeMillis()
        )

        val presets = loadPresets().toMutableList()
        presets.add(preset)

        val presetsJson = json.encodeToString(presets)
        prefs.edit().putString("presets", presetsJson).apply()
    }

    /**
     * Load all window presets
     *
     * @return List of presets, or empty list if none saved
     */
    fun loadPresets(): List<WindowPreset> {
        val presetsJson = prefs.getString("presets", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<WindowPreset>>(presetsJson)
        } catch (e: Exception) {
            // Log error and return empty list if JSON is corrupted
            emptyList()
        }
    }

    /**
     * Delete a preset by ID
     *
     * @param id Preset ID to delete
     */
    fun deletePreset(id: String) {
        val presets = loadPresets().filter { it.id != id }
        val presetsJson = json.encodeToString(presets)
        prefs.edit().putString("presets", presetsJson).apply()
    }

    /**
     * Get presets by group name
     *
     * @param groupName Group name to filter by
     * @return List of presets in the specified group
     */
    fun getPresetsByGroup(groupName: String): List<WindowPreset> {
        return loadPresets().filter { it.groupName == groupName }
    }

    /**
     * Get all unique group names
     *
     * @return Sorted list of group names
     */
    fun getGroupNames(): List<String> {
        return loadPresets().map { it.groupName }.distinct().sorted()
    }

    /**
     * Clear all presets (for testing/reset)
     */
    fun clearAllPresets() {
        prefs.edit().remove("presets").apply()
    }
}
```

**Storage Format (JSON):**
```json
[
  {
    "id": "uuid-1234",
    "title": "Gmail",
    "type": "WEB_APP",
    "content": {
      "type": "WebContent",
      "url": "https://gmail.com"
    },
    "groupName": "Work",
    "color": "#4ECDC4",
    "createdAt": 1702234567890
  },
  {
    "id": "uuid-5678",
    "title": "Calculator",
    "type": "ANDROID_APP",
    "content": {
      "type": "FreeformAppContent",
      "packageName": "com.android.calculator2"
    },
    "groupName": "Tools",
    "color": "#FF6B9D",
    "createdAt": 1702234568901
  }
]
```

---

#### Component 4: Updated AddWindowDialog

**Changes to ControlPanel.kt:**

1. Add preset manager parameter
2. Add preset list section
3. Add "Save as preset" checkbox
4. Add group name input
5. Add Enter key handler to URL field
6. Change default URL to google.com
7. Update type labels
8. Filter Widget option

**Pseudo-code:**
```kotlin
@Composable
private fun AddWindowDialog(
    presetManager: WindowPresetManager,
    onDismiss: () -> Unit,
    onConfirm: (String, WindowType, String, WindowContent) -> Unit
) {
    // State variables
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(WindowType.WEB_APP) }
    var url by remember { mutableStateOf("https://google.com") }  // CHANGED
    var packageName by remember { mutableStateOf("com.android.calculator2") }
    var saveAsPreset by remember { mutableStateOf(false) }  // NEW
    var groupName by remember { mutableStateOf("default") }  // NEW

    val presets by remember { mutableStateOf(presetManager.loadPresets()) }  // NEW
    var selectedPreset by remember { mutableStateOf<WindowPreset?>(null) }  // NEW

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Window") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // NEW: Preset selection section
                if (presets.isNotEmpty()) {
                    Text("Load Preset:", style = MaterialTheme.typography.labelMedium)
                    presets.forEach { preset ->
                        PresetRow(
                            preset = preset,
                            selected = selectedPreset?.id == preset.id,
                            onClick = {
                                selectedPreset = preset
                                title = preset.title
                                selectedType = preset.type
                                // Auto-fill content based on type
                            }
                        )
                    }
                    Divider()
                }

                // Manual entry section
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Window Title") },
                    singleLine = true
                )

                Text("Window Type:", style = MaterialTheme.typography.labelMedium)
                WindowType.values()
                    .filter { it != WindowType.WIDGET }  // NEW: Hide widget
                    .forEach { type ->
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = getTypeLabel(type)  // CHANGED: "Web Page (URL)"
                        )
                    }

                // Type-specific inputs
                if (selectedType == WindowType.WEB_APP) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = true,
                        placeholder = { Text("https://example.com") },
                        // NEW: Enter key handler
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { submitDialog() }
                        )
                    )
                }

                // NEW: Save as preset checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = saveAsPreset,
                        onCheckedChange = { saveAsPreset = it }
                    )
                    Text("Save as preset")
                }

                // NEW: Group name input (only if saving preset)
                if (saveAsPreset) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        placeholder = { Text("e.g., Work, Personal") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    submitDialog()
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    fun submitDialog() {
        if (title.isNotBlank()) {
            val color = when (selectedType) {
                WindowType.ANDROID_APP -> "#FF6B9D"
                WindowType.WEB_APP -> "#4ECDC4"
                WindowType.WIDGET -> "#95E1D3"
                WindowType.REMOTE_DESKTOP -> "#FFD93D"
            }

            val content = when (selectedType) {
                WindowType.WEB_APP -> WindowContent.WebContent(url)
                WindowType.ANDROID_APP -> WindowContent.FreeformAppContent(packageName)
                else -> WindowContent.MockContent
            }

            // NEW: Save preset if checkbox checked
            if (saveAsPreset) {
                presetManager.savePreset(title, selectedType, content, groupName, color)
            }

            onConfirm(title, selectedType, color, content)
        }
    }
}
```

---

## Dependencies

### Existing Dependencies
- Jetpack Compose 1.5+
- Material 3
- kotlinx-coroutines
- AndroidX Lifecycle

### New Dependencies (Phase 2)
```kotlin
// build.gradle.kts (app-level)
dependencies {
    // For WindowPreset serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

**Add to project-level build.gradle.kts:**
```kotlin
plugins {
    kotlin("plugin.serialization") version "1.9.20" apply false
}
```

**Add to app-level build.gradle.kts:**
```kotlin
plugins {
    kotlin("plugin.serialization")
}
```

---

## Implementation Plan

### Phase 1: Quick Wins (P0-P1) - 2.5 hours

#### Task 1.1: Fix Maximize/Minimize Button Logic (30 min)
**Priority:** P0
**Files:** `WorkspaceViewModel.kt`
**Steps:**
1. Read current `toggleWindowSize()` implementation
2. Update logic to always set `isHidden = false` before toggling `isLarge`
3. Add KDoc comment explaining state transitions
4. Build and test on emulator

**Testing:**
- Minimize window → verify 48dp
- Click maximize on minimized → verify full screen
- Click maximize on normal → verify full screen
- Click maximize on maximized → verify 400dp

---

#### Task 1.2: Dynamic Maximize Sizing (1 hour)
**Priority:** P1
**Files:** `WindowCard.kt`
**Steps:**
1. Wrap WindowCard content in `BoxWithConstraints`
2. Calculate `maximizedWidth = maxWidth - 40.dp`
3. Calculate `maximizedHeight = maxHeight - 40.dp`
4. Update `animateDpAsState` to use dynamic sizes
5. Test in portrait and landscape modes

**Testing:**
- Portrait: Maximize → verify 20dp border all sides
- Landscape: Maximize → verify 20dp border all sides
- Rotate while maximized → verify adapts

---

#### Task 1.3: Change Default URL + Add Enter Key (25 min)
**Priority:** P1
**Files:** `ControlPanel.kt`
**Steps:**
1. Change default URL from webavanue.com to google.com (Line 128)
2. Add `keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)` to URL field
3. Add `keyboardActions = KeyboardActions(onDone = { submitDialog() })` to URL field
4. Extract submit logic to `submitDialog()` function
5. Test Enter key behavior

**Testing:**
- Open dialog → verify "https://google.com" default
- Type URL, press Enter → verify window created
- Enter with blank title → verify no action

---

#### Task 1.4: Update Terminology + Hide Widget (15 min)
**Priority:** P2
**Files:** `ControlPanel.kt`
**Steps:**
1. Update `getTypeLabel()` to return "Web Page (URL)" for WEB_APP
2. Filter Widget from `WindowType.values()` list (Line 144)
3. Build and test

**Testing:**
- Open dialog → verify "Web Page (URL)" label
- Verify only 3 radio buttons (Android App, Web Page, Remote Desktop)

---

### Phase 2: Window Presets (P2) - 5 hours

#### Task 2.1: Create WindowPreset Data Model (30 min)
**Priority:** P2
**Files:** New: `Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/presets/WindowPreset.kt`
**Steps:**
1. Create WindowPreset data class with `@Serializable` annotation
2. Add id, title, type, content, groupName, color, createdAt fields
3. Add to Common module
4. Build and verify compilation

---

#### Task 2.2: Implement WindowPresetManager (1.5 hours)
**Priority:** P2
**Files:** New: `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WindowPresetManager.kt`
**Steps:**
1. Create WindowPresetManager class
2. Implement `savePreset()` with JSON serialization
3. Implement `loadPresets()` with JSON deserialization
4. Implement `deletePreset()`
5. Implement `getPresetsByGroup()`
6. Implement `getGroupNames()`
7. Add error handling for corrupted JSON
8. Write unit tests

**Testing:**
- Save preset → verify JSON in SharedPreferences
- Load presets → verify deserialization
- Delete preset → verify removal
- Corrupted JSON → verify graceful degradation

---

#### Task 2.3: Update AddWindowDialog with Preset UI (2 hours)
**Priority:** P2
**Files:** `ControlPanel.kt`
**Steps:**
1. Add `presetManager` parameter to AddWindowDialog
2. Add preset list section with LazyColumn
3. Add "Save as preset" checkbox
4. Add group name input field
5. Add preset selection logic (auto-fill title/URL)
6. Update submit logic to save preset if checked
7. Add preset row composable
8. Test full workflow

**Testing:**
- Add window with "Save as preset" → verify saved
- Open dialog → verify preset in list
- Select preset → verify auto-fill
- Add from preset → verify window created
- Restart app → verify presets persisted

---

#### Task 2.4: Add kotlinx-serialization Dependency (30 min)
**Priority:** P2
**Files:** `build.gradle.kts`
**Steps:**
1. Add `kotlin("plugin.serialization")` plugin to app build.gradle.kts
2. Add `kotlinx-serialization-json` dependency
3. Sync project
4. Verify builds successfully

---

#### Task 2.5: Integration Testing (30 min)
**Priority:** P2
**Steps:**
1. Test full preset workflow end-to-end
2. Test with multiple presets in different groups
3. Test preset deletion (if implemented)
4. Test error scenarios (corrupted JSON, missing fields)
5. Test performance with 20+ presets

---

## Testing Strategy

### Unit Tests

**WorkspaceViewModel Tests:**
```kotlin
@Test
fun `toggleWindowSize on minimized window restores and maximizes`() {
    // Given: minimized window
    val window = AppWindow(
        id = "test-id",
        title = "Test",
        isHidden = true,
        isLarge = false
    )
    viewModel.addWindow(window)

    // When: toggle size
    viewModel.toggleWindowSize("test-id")

    // Then: window is restored and maximized
    val updated = viewModel.windows.value.first { it.id == "test-id" }
    assertFalse(updated.isHidden)
    assertTrue(updated.isLarge)
}

@Test
fun `toggleWindowSize on normal window maximizes`() {
    // Given: normal window
    val window = AppWindow(
        id = "test-id",
        title = "Test",
        isHidden = false,
        isLarge = false
    )
    viewModel.addWindow(window)

    // When: toggle size
    viewModel.toggleWindowSize("test-id")

    // Then: window is maximized
    val updated = viewModel.windows.value.first { it.id == "test-id" }
    assertFalse(updated.isHidden)
    assertTrue(updated.isLarge)
}
```

**WindowPresetManager Tests:**
```kotlin
@Test
fun `savePreset persists to SharedPreferences`() {
    // Given: preset manager
    val manager = WindowPresetManager(context)

    // When: save preset
    manager.savePreset(
        title = "Gmail",
        type = WindowType.WEB_APP,
        content = WindowContent.WebContent("https://gmail.com"),
        groupName = "Work"
    )

    // Then: preset is saved
    val presets = manager.loadPresets()
    assertEquals(1, presets.size)
    assertEquals("Gmail", presets[0].title)
    assertEquals("Work", presets[0].groupName)
}

@Test
fun `loadPresets handles corrupted JSON gracefully`() {
    // Given: corrupted JSON in SharedPreferences
    val prefs = context.getSharedPreferences("window_presets", Context.MODE_PRIVATE)
    prefs.edit().putString("presets", "{invalid json}").commit()

    // When: load presets
    val manager = WindowPresetManager(context)
    val presets = manager.loadPresets()

    // Then: returns empty list instead of crashing
    assertEquals(0, presets.size)
}
```

---

### Manual Testing

**Test Suite: Portrait Mode Fixes**

| ID | Test Case | Expected Result |
|----|-----------|-----------------|
| **FR-1.1: Maximize/Minimize Bug** |
| T1.1.1 | Minimize window, then maximize | Window goes from 48dp → full screen minus 20px |
| T1.1.2 | Maximize window, then minimize | Window goes from full → 48dp |
| T1.1.3 | Normal → Maximize → Minimize → Maximize | Window restores to full screen |
| T1.1.4 | Minimize → Minimize (double click) | No change (already minimized) |
| **FR-1.2: Dynamic Maximize** |
| T1.2.1 | Maximize in portrait mode | Window fills screen with 20dp border |
| T1.2.2 | Maximize in landscape mode | Window fills screen with 20dp border |
| T1.2.3 | Rotate while maximized | Window adapts to new orientation |
| T1.2.4 | Test on phone (1080x1920) | 20dp border maintained |
| T1.2.5 | Test on tablet (1440x2960) | 20dp border maintained |
| **FR-1.3: Default URL** |
| T1.3.1 | Open Add Window dialog | Default URL is "https://google.com" |
| T1.3.2 | Clear URL field | Empty field |
| T1.3.3 | Type custom URL | Accepts input |
| **FR-1.4: Enter Key** |
| T1.4.1 | Type title, URL, press Enter | Window created, dialog closed |
| T1.4.2 | Press Enter with blank title | No action |
| T1.4.3 | Press Enter in URL field | Keyboard dismisses |
| **FR-1.5: Terminology** |
| T1.5.1 | Open dialog | "Web Page (URL)" label shown |
| T1.5.2 | Select "Web Page (URL)" | URL field appears |
| **FR-1.6: Hide Widget** |
| T1.6.1 | Open dialog | Widget option not visible |
| T1.6.2 | Count radio buttons | Only 3 shown |
| **FR-2.1: Window Presets** |
| T2.1.1 | Add window, check "Save as preset" | Preset saved |
| T2.1.2 | Open dialog again | Preset appears in list |
| T2.1.3 | Select preset | Title/URL auto-filled |
| T2.1.4 | Add window from preset | Window created correctly |
| T2.1.5 | Restart app, open dialog | Presets persisted |
| T2.1.6 | Add 10 presets in 3 groups | Organized correctly |
| T2.1.7 | Corrupted JSON scenario | App doesn't crash |

---

## Success Criteria

### Phase 1 Success Criteria (Must Pass All)
- [ ] Minimize → Maximize: Window expands to full screen (not stuck at 48dp)
- [ ] Maximize in portrait: Window is `screenWidth - 40dp` x `screenHeight - 40dp`
- [ ] Maximize in landscape: Window is `screenWidth - 40dp` x `screenHeight - 40dp`
- [ ] Rotate while maximized: Window adapts without manual resize
- [ ] Add Window dialog: Default URL is "https://google.com"
- [ ] Add Window dialog: Enter key submits (same as clicking "Add")
- [ ] Add Window dialog: "Web Page (URL)" label shown
- [ ] Add Window dialog: Widget option hidden
- [ ] All animations smooth (60 FPS)
- [ ] No regressions in existing features

### Phase 2 Success Criteria (Must Pass All)
- [ ] Save preset: Preset persisted to SharedPreferences
- [ ] Load preset: Preset list appears in dialog
- [ ] Select preset: Title/URL auto-populated
- [ ] Add from preset: Window created with correct content
- [ ] Restart app: Presets persisted across launches
- [ ] Corrupted JSON: App handles gracefully (no crash)
- [ ] 20+ presets: Performance acceptable (<100ms dialog open)
- [ ] Multiple groups: Presets organized correctly

---

## Rollback Plan

If critical bugs are discovered:

1. **Revert commits:**
   ```bash
   git revert <commit-hash>
   git push origin Cockpit-Development
   ```

2. **Disable new features:**
   - Preset system: Remove WindowPresetManager, revert ControlPanel.kt
   - Dynamic sizing: Revert to fixed 600x800dp
   - State fix: Revert toggleWindowSize() to original logic

3. **Emergency hotfix:**
   - Create hotfix branch from last stable commit
   - Apply minimal fix
   - Fast-track testing
   - Deploy immediately

---

## Future Enhancements

### Phase 3: Advanced Presets (Future)
- Preset import/export (JSON file)
- Preset sharing via QR code
- Cloud sync of presets (Firebase/Supabase)
- Preset categories with icons
- Recent presets section
- Preset search/filter

### Phase 4: Workspace Templates (Future - relates to Legacy Gap #6)
- Save entire workspace as template
- Load workspace template (restores all windows)
- Workspace quick switcher
- Workspace scheduling (load "Work" at 9am, "Personal" at 5pm)

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **BoxWithConstraints performance** | Low | Medium | Profile with GPU Rendering, fallback to fixed size if slow |
| **SharedPreferences data loss** | Low | High | Add backup to Room database in Phase 3 |
| **JSON deserialization errors** | Medium | Medium | Extensive error handling, default to empty list |
| **Screen size edge cases** | Medium | Low | Test on 5+ device sizes (phone, tablet, foldable) |
| **Rotation bugs** | Low | Medium | Test all orientations, add orientation lock option |
| **Preset UI too complex** | Medium | Low | User testing, simplify if >3 clicks to add preset |

---

## Appendix

### A: State Machine Diagram

```
Window States:
┌─────────────────────────────────────────────────────┐
│                                                     │
│  Normal (isHidden=false, isLarge=false)             │
│  - Width: 300dp                                     │
│  - Height: 400dp                                    │
│                                                     │
│  Actions:                                           │
│  - Minimize button → Minimized state                │
│  - Maximize button → Maximized state                │
│                                                     │
└─────────────────────────────────────────────────────┘
         ↓ Minimize                    ↑ Maximize
         ↓                             ↑
┌─────────────────────────────────────────────────────┐
│                                                     │
│  Minimized (isHidden=true, isLarge=false)           │
│  - Width: 300dp                                     │
│  - Height: 48dp (title bar only)                    │
│                                                     │
│  Actions:                                           │
│  - Minimize button → Restored to Normal             │
│  - Maximize button → Maximized state (FIX)          │
│                                                     │
└─────────────────────────────────────────────────────┘
         ↓ Maximize                    ↑ Maximize
         ↓                             ↑
┌─────────────────────────────────────────────────────┐
│                                                     │
│  Maximized (isHidden=false, isLarge=true)           │
│  - Width: screenWidth - 40dp                        │
│  - Height: screenHeight - 40dp                      │
│                                                     │
│  Actions:                                           │
│  - Minimize button → Minimized state                │
│  - Maximize button → Normal state                   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

### B: File Change Summary

| File | Changes | Lines Added | Lines Removed |
|------|---------|-------------|---------------|
| `WorkspaceViewModel.kt` | Update toggleWindowSize() | 3 | 1 |
| `WindowCard.kt` | Wrap in BoxWithConstraints, dynamic sizing | 15 | 8 |
| `ControlPanel.kt` | Default URL, Enter key, terminology, hide widget | 25 | 10 |
| `WindowPresetManager.kt` (NEW) | Full implementation | 120 | 0 |
| `WindowPreset.kt` (NEW) | Data model | 15 | 0 |
| `AddWindowDialog` (ControlPanel.kt) | Preset UI, save checkbox, group input | 80 | 30 |
| `build.gradle.kts` | Add kotlinx-serialization | 3 | 0 |
| **TOTAL** | - | **261** | **49** |

---

### C: Related Documentation

- **Issue Analysis:** `/Volumes/M-Drive/Coding/NewAvanues-Cockpit/Docs/Issues/Cockpit-Portrait-Mode-Issues-51210-V1.md`
- **Sprint Status:** `/Volumes/M-Drive/Coding/NewAvanues-Cockpit/Docs/Cockpit-Sprint-Status-51210-V1.md`
- **Testing Guide:** `/Volumes/M-Drive/Coding/NewAvanues-Cockpit/Docs/Cockpit-Testing-Guide-51210-V1.md`
- **Legacy Gaps:** `/Volumes/M-Drive/Coding/NewAvanues-Cockpit/Docs/Cockpit-Legacy-Gaps-Analysis-51210-V1.md`

---

## Sign-Off

**Specification Status:** ✅ READY FOR IMPLEMENTATION
**Approval Required:** Yes - Review by stakeholders before Phase 2
**Estimated Effort:** 7.5 hours total (2.5h Phase 1, 5h Phase 2)
**Risk Level:** Low-Medium

**Next Steps:**
1. Review specification with stakeholders
2. Approve Phase 1 implementation (P0-P1)
3. Create implementation tasks in TodoWrite
4. Begin Task 1.1 (Fix Maximize/Minimize)
5. Deploy Phase 1 fixes
6. User testing on physical device
7. Review Phase 2 (Window Presets) for next iteration

**Prepared By:** AI Assistant
**Date:** 2025-12-10
**Specification Method:** CoT (Chain of Thought) + RoT (Reasoning over Thoughts)
**Review Status:** Pending

---

**End of Specification**
